import os
from influxdb_client.client.influxdb_client_async import InfluxDBClientAsync

INFLUX_URL = os.getenv("INFLUX_URL", "http://localhost:8086")
INFLUX_TOKEN = os.getenv("INFLUX_TOKEN", "super-secret-auth-token-123")
INFLUX_ORG = os.getenv("INFLUX_ORG", "health_monitoring")

INFLUX_CLIENT = None


async def get_influx_client() -> InfluxDBClientAsync:
    global INFLUX_CLIENT

    if INFLUX_CLIENT is None:
        INFLUX_CLIENT = InfluxDBClientAsync(url=INFLUX_URL, token=INFLUX_TOKEN, org=INFLUX_ORG)

    return INFLUX_CLIENT
