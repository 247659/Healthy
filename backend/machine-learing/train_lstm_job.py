import asyncio
import os
from influxdb_client.client.influxdb_client_async import InfluxDBClientAsync
from vitals_repository import VitalsRepository
from ai_trainer import AITrainer


async def main():
    print("==================================================")
    print("🤖 ROZPOCZYNAM GLOBALNY TRENING SIECI NEURONOWEJ LSTM")
    print("==================================================")

    influx_client = InfluxDBClientAsync(
        url=os.getenv("INFLUX_URL", "http://localhost:8086"),
        token=os.getenv("INFLUX_TOKEN", "super-secret-auth-token-123"),
        org=os.getenv("INFLUX_ORG", "health_monitoring"),
        timeout=6000
    )

    repository = VitalsRepository(influx_client)
    trainer = AITrainer(repository)

    await trainer.train_global_lstm()

    print("\n✅ Trening zakończony! Zrestartuj 'ai_worker.py', aby załadował nowy model LSTM do RAM.")


if __name__ == "__main__":
    asyncio.run(main())