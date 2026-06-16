from influxdb_client import Point
from influxdb_client.client.influxdb_client_async import InfluxDBClientAsync
from schemas.vitals_schema import VitalsPayload
import os

class VitalsRepository:
    def __init__(self, client: InfluxDBClientAsync):
        self.client = client
        self.bucket = os.getenv("INFLUX_BUCKET", "vital_signs")

    async def save_measurement(self, payload: VitalsPayload):
        point = (
            Point("vitals")
            .tag("patient_id", payload.patientId)
            .field("heart_rate", payload.measurements.heartRate)
            .field("sys_bp", payload.measurements.bloodPressure.systolic)
            .field("dia_bp", payload.measurements.bloodPressure.diastolic)
            .field("temperature", payload.measurements.temperature)
            .field("spo2", payload.measurements.spO2)
            .time(payload.timestamp)
        )

        write_api = self.client.write_api()
        await write_api.write(bucket=self.bucket, record=point)