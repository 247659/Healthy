import asyncio
import json
import logging
import uuid
from _contextvars import ContextVar

import aio_pika
import os
from functools import partial
from datetime import datetime
from influxdb_client.client.influxdb_client_async import InfluxDBClientAsync

from vitals_repository import VitalsRepository
from anomaly_detector import VitalsAnomalyDetector
from ai_trainer import AITrainer
from vitals_schema import VitalsPayload

trace_id_var = ContextVar("trace_id", default="")
span_id_var = ContextVar("span_id", default="")

# 1. Tworzymy nową fabrykę logów, która zawsze dodaje traceId i spanId
old_factory = logging.getLogRecordFactory()

def trace_record_factory(*args, **kwargs):
    record = old_factory(*args, **kwargs)
    record.traceId = trace_id_var.get()
    record.spanId = span_id_var.get()
    return record

logging.setLogRecordFactory(trace_record_factory)

# 2. Ustawiamy globalny format
logging.basicConfig(
    level=logging.INFO,
    format="%(levelname)5s [%(traceId)s] [%(spanId)s] %(message)s"
)

# 3. Pobieramy loggera, nie musimy już dodawać do niego żadnych filtrów!
logger = logging.getLogger("ml_worker")

message_counters = {}

async def train_in_background(patient_id: str, ai_trainer: AITrainer, detector: VitalsAnomalyDetector):
    new_model = await ai_trainer.train_isolation_forest(patient_id)
    if new_model:
        detector.loaded_models[patient_id] = new_model
        logger.info(f"Model dla pacjenta {patient_id} został pomyślnie przetrenowany i załadowany.")

async def on_message(message: aio_pika.abc.AbstractIncomingMessage,
                     detector: VitalsAnomalyDetector,
                     trainer: AITrainer,
                     channel: aio_pika.abc.AbstractChannel):
    async with message.process():
        try:
            headers = message.headers or {}

            # Spring Boot 3 wstrzykuje nagłówek 'traceparent' lub 'b3'
            traceparent = headers.get('traceparent') or headers.get('b3')

            if traceparent:
                if isinstance(traceparent, bytes):
                    traceparent = traceparent.decode('utf-8')

                parts = traceparent.split("-")
                if len(parts) >= 3:
                    trace_id_var.set(parts[1])  # Trace ID
                    span_id_var.set(parts[2])  # Span ID
                else:
                    trace_id_var.set(traceparent.split("-")[0] if "-" in traceparent else traceparent)
                    span_id_var.set(uuid.uuid4().hex[:16])
            else:
                # Brak nagłówka (np. żądanie wysłane ręcznie z panelu RabbitMQ)
                trace_id_var.set(uuid.uuid4().hex)
                span_id_var.set(uuid.uuid4().hex[:16])

            body_dict = json.loads(message.body.decode())
            payload = VitalsPayload(**body_dict)
            patient_id = payload.patientId

            logger.info(f"[*] Analiza pacjenta: {patient_id} | Tętno: {payload.heartRate}")

            analysis = detector.analyze_vitals(payload)
            method = analysis.get('method', 'Unknown')
            if 'lstm_status' in analysis:
                logger.info(
                    f"[*] Analiza pacjenta: {patient_id} | Tętno: {payload.heartRate} | Metoda: {method} | Status: {analysis['lstm_status']}")
            else:
                logger.info(
                    f"[*] Analiza pacjenta: {patient_id} | Tętno: {payload.heartRate} | Całkowite Ryzyko: {analysis.get('risk_score')} | Metoda: {method}")

            if analysis.get('risk_score', 0.0) > 0.6:
                logger.info(f"🚨 ALERT dla {patient_id}: {analysis['risk_score']}")
                alert_payload = {
                    "patientId": patient_id,
                    "riskScore": analysis['risk_score'],
                    "message": "Wykryto anomalię w parametrach życiowych!",
                    "timestamp": datetime.now().isoformat()
                }

                notification_exchange = await channel.get_exchange("notifications.exchange")

                await notification_exchange.publish(
                    aio_pika.Message(
                        body=json.dumps(alert_payload).encode(),
                        content_type="application/json"
                    ),
                    routing_key="notifications.incoming"
                )

            global message_counters
            message_counters[patient_id] = message_counters.get(patient_id, 0) + 1

            if not detector.has_model(patient_id) or message_counters[patient_id] % 50 == 0:
                asyncio.create_task(train_in_background(patient_id, trainer, detector))

        except Exception as e:
            logger.info(f"❌ Błąd przetwarzania: {e}")


async def main():
    influx_client = InfluxDBClientAsync(
        url=os.getenv("INFLUX_URL", "http://localhost:8086"),
        token=os.getenv("INFLUX_TOKEN", "super-secret-auth-token-123"),
        org=os.getenv("INFLUX_ORG", "health_monitoring")
    )

    repository = VitalsRepository(influx_client)
    detector = VitalsAnomalyDetector()
    trainer = AITrainer(repository)

    rabbitmq_url = os.getenv("RABBITMQ_URL", "amqp://admin:adminpassword@localhost/")
    connection = await aio_pika.connect_robust(rabbitmq_url)

    async with connection:
        channel = await connection.channel()
        await channel.set_qos(prefetch_count=10)

        exchange = await channel.declare_exchange(
            "iot.vitals.exchange",
            aio_pika.ExchangeType.TOPIC,
            durable=True
        )

        vitals_queue = await channel.declare_queue("vitals.ml.queue", durable=True)
        await vitals_queue.bind(exchange, routing_key="vitals.incoming")

        await channel.declare_exchange("notifications.exchange", aio_pika.ExchangeType.TOPIC, durable=True)

        print("✅ [AI Worker] Nasłuchuje na 'vitals.ml.queue'...")

        await vitals_queue.consume(
            partial(on_message, detector=detector, trainer=trainer, channel=channel)
        )

        await asyncio.Future()


if __name__ == "__main__":
    asyncio.run(main())