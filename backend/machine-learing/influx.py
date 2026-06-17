from influxdb_client.client.influxdb_client_async import InfluxDBClientAsync
import os

INFLUX_URL = os.getenv("INFLUX_URL", "http://localhost:8086")
INFLUX_TOKEN = os.getenv("INFLUX_TOKEN", "super-secret-auth-token-123")
INFLUX_ORG = os.getenv("INFLUX_ORG", "health_monitoring")

_influx_client = None


async def get_influx_client() -> InfluxDBClientAsync:
    global _influx_client

    if _influx_client is None:
        _influx_client = InfluxDBClientAsync(url=INFLUX_URL, token=INFLUX_TOKEN, org=INFLUX_ORG)

    return _influx_client