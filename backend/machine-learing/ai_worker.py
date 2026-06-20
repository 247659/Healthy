import asyncio
import json
import logging
import uuid
from contextvars import ContextVar
from datetime import datetime
from functools import partial

import aio_pika
import os

from vitals_repository import VitalsRepository
from anomaly_detector import VitalsAnomalyDetector
from ai_trainer import AITrainer
from vitals_schema import VitalsPayload
from data_interpolator import payload_to_reading, validate_reading

trace_id_var = ContextVar("trace_id", default="")
span_id_var  = ContextVar("span_id",  default="")

old_factory = logging.getLogRecordFactory()

def trace_record_factory(*args, **kwargs):
    record          = old_factory(*args, **kwargs)
    record.traceId  = trace_id_var.get()
    record.spanId   = span_id_var.get()
    return record

logging.setLogRecordFactory(trace_record_factory)
logging.basicConfig(
    level=logging.INFO,
    format="%(levelname)5s [%(traceId)s] [%(spanId)s] %(message)s"
)
logger = logging.getLogger("ml_worker")

message_counters: dict[str, int] = {}

async def train_in_background(
    patient_id: str,
    ai_trainer: AITrainer,
    detector: VitalsAnomalyDetector,
) -> None:
    new_model = await ai_trainer.train_isolation_forest(patient_id)
    if new_model:
        detector.loaded_models[patient_id] = new_model
        logger.info(f"Model per-pacjent {patient_id} zaktualizowany.")

async def on_message(
    message:  aio_pika.abc.AbstractIncomingMessage,
    detector: VitalsAnomalyDetector,
    trainer:  AITrainer,
    channel:  aio_pika.abc.AbstractChannel,
) -> None:
    async with message.process():
        try:
            headers    = message.headers or {}
            traceparent = headers.get("traceparent") or headers.get("b3")
            if traceparent:
                if isinstance(traceparent, bytes):
                    traceparent = traceparent.decode()
                parts = traceparent.split("-")
                trace_id_var.set(parts[1] if len(parts) >= 3 else traceparent)
                span_id_var.set(parts[2] if len(parts) >= 3 else uuid.uuid4().hex[:16])
            else:
                trace_id_var.set(uuid.uuid4().hex)
                span_id_var.set(uuid.uuid4().hex[:16])

            body_dict = json.loads(message.body.decode())
            payload   = VitalsPayload(**body_dict)
            patient_id = payload.patientId

            reading = payload_to_reading(payload)
            reading = validate_reading(reading)
            if reading.rejected:
                logger.warning(
                    f"[!] Odrzucono artefakt dla {patient_id}: {reading.reject_reason}"
                )
                return

            analysis   = detector.analyze_vitals(payload)
            risk_score = analysis.get("risk_score", 0.0)
            method     = analysis.get("method", "Unknown")
            xai        = analysis.get("xai", {})

            logger.info(
                f"[*] {patient_id} | HR={payload.heartRate} | "
                f"risk={risk_score:.2f} | method={method} | "
                f"severity={xai.get('severity', '?')}"
            )

            if risk_score > 0.6:
                logger.info(
                    f"ALERT {patient_id}: {xai.get('headline', '')}"
                )
                alert_payload = {
                    "patientId":      patient_id,
                    "riskScore":      risk_score,
                    "severity":       xai.get("severity", "HIGH"),
                    "message":        xai.get("headline", "Wykryto anomalie!"),
                    "details":        xai.get("details", []),
                    "recommendation": xai.get("recommendation", ""),
                    "forecastNote":   xai.get("forecast_note"),
                    "method":         method,
                    "timestamp":      datetime.now().isoformat(),
                    # "rawValues":      analysis.get("xai", {}).get("raw_values", {}),
                }
                notif_exchange = await channel.get_exchange("notifications.exchange")
                await notif_exchange.publish(
                    aio_pika.Message(
                        body=json.dumps(alert_payload).encode(),
                        content_type="application/json",
                    ),
                    routing_key="notifications.incoming",
                )

            message_counters[patient_id] = message_counters.get(patient_id, 0) + 1
            if not detector.has_model(patient_id) or message_counters[patient_id] % 50 == 0:
                asyncio.create_task(
                    train_in_background(patient_id, trainer, detector)
                )

        except Exception as e:
            logger.error(f"Blad przetwarzania: {e}", exc_info=True)

async def main() -> None:
    from influxdb_client.client.influxdb_client_async import InfluxDBClientAsync

    influx_client = InfluxDBClientAsync(
        url=os.getenv("INFLUX_URL",   "http://localhost:8086"),
        token=os.getenv("INFLUX_TOKEN", "super-secret-auth-token-123"),
        org=os.getenv("INFLUX_ORG",    "health_monitoring"),
        timeout=6000
    )

    repository = VitalsRepository(influx_client)
    detector   = VitalsAnomalyDetector()
    trainer    = AITrainer(repository)

    connection = await aio_pika.connect_robust(
        os.getenv("RABBITMQ_URL", "amqp://admin:adminpassword@localhost/")
    )

    async with connection:
        channel = await connection.channel()
        await channel.set_qos(prefetch_count=10)

        exchange = await channel.declare_exchange(
            "iot.vitals.exchange",
            aio_pika.ExchangeType.TOPIC,
            durable=True,
        )
        vitals_queue = await channel.declare_queue("vitals.ml.queue", durable=True)
        await vitals_queue.bind(exchange, routing_key="vitals.incoming")

        await channel.declare_exchange(
            "notifications.exchange",
            aio_pika.ExchangeType.TOPIC,
            durable=True,
        )

        logger.info("AI Worker nasluchuje na 'vitals.ml.queue'...")
        await vitals_queue.consume(
            partial(on_message, detector=detector, trainer=trainer, channel=channel)
        )
        await asyncio.Future()


if __name__ == "__main__":
    asyncio.run(main())
