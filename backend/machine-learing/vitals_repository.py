from datetime import datetime
from typing import Any

from influxdb_client.client.influxdb_client_async import InfluxDBClientAsync
import os

class VitalsRepository:
    def __init__(self, client: InfluxDBClientAsync):
        self.client = client
        self.bucket = os.getenv("INFLUX_BUCKET", "vital_signs")
        self.org = os.getenv("INFLUX_ORG", "health_monitoring")

    async def get_measurements(self, patient_id: str, start_time: datetime, end_time: datetime) -> list[Any]:
        query = """
                from(bucket: _bucket)
                    |> range(start: _start, stop: _stop)
                    |> filter(fn: (r) => r["_measurement"] == "vitals")
                    |> filter(fn: (r) => r["patient_id"] == _patient)
                    |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
                    |> sort(columns: ["_time"], desc: true)
                """

        params = {
            "_bucket": self.bucket,
            "_start": start_time,
            "_stop": end_time,
            "_patient": patient_id
        }

        query_api = self.client.query_api()
        result = await query_api.query(query=query, params=params, org=self.org)
        history = []
        for table in result:
            for record in table.records:
                history.append({
                    "timestamp": record.get_time().isoformat(),
                    "measurements": {
                        "heartRate": record.values.get("heart_rate"),
                        "bloodPressure": {
                            "systolic": record.values.get("sys_bp"),
                            "diastolic": record.values.get("dia_bp")
                        },
                        "temperature": record.values.get("temperature"),
                        "spO2": record.values.get("spo2")
                    }
                })

        return history

    async def get_all_measurements(self, start_time: datetime, end_time: datetime) -> list[Any]:
        query = """
                from(bucket: _bucket)
                    |> range(start: _start, stop: _stop)
                    |> filter(fn: (r) => r["_measurement"] == "vitals")
                    |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
                    |> sort(columns: ["_time"], desc: true)
                """

        params = {
            "_bucket": self.bucket,
            "_start": start_time,
            "_stop": end_time
        }

        query_api = self.client.query_api()
        result = await query_api.query(query=query, params=params, org=self.org)
        history = []

        for table in result:
            for record in table.records:
                history.append({
                    "patientId": record.values.get("patient_id"),
                    "timestamp": record.get_time().isoformat(),
                    "measurements": {
                        "heartRate": record.values.get("heart_rate"),
                        "bloodPressure": {
                            "systolic": record.values.get("sys_bp"),
                            "diastolic": record.values.get("dia_bp")
                        },
                        "temperature": record.values.get("temperature"),
                        "spO2": record.values.get("spo2")
                    }
                })

        return history