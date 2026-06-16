from datetime import datetime

from fastapi import APIRouter, Depends, Path, Query
from influxdb_client.client.influxdb_client_async import InfluxDBClientAsync
from starlette.background import BackgroundTasks

from schemas.vitals_schema import VitalsPayload
from dependencies.influx import get_influx_client
from repositories.vitals_repository import VitalsRepository
from services.vitals_service import VitalsService

router = APIRouter()

def get_vitals_service(influx_client: InfluxDBClientAsync = Depends(get_influx_client)) -> VitalsService:
    repository = VitalsRepository(influx_client)
    return VitalsService(repository)

@router.post('/api/vitals')
async def receive_vitals(
        payload: VitalsPayload,
        background_tasks: BackgroundTasks,
        vitals_service: VitalsService = Depends(get_vitals_service)
):
    return await vitals_service.process_vitals(payload, background_tasks)


@router.get("/api/vitals/{patientId}")
async def get_history(
        patientId: str = Path(..., title="Patient's ID"),
        start_time: datetime = Query(..., description="Czas początkowy (np. 2026-06-15T00:00:00Z)"),
        end_time: datetime = Query(..., description="Czas końcowy (np. 2026-06-16T23:59:59Z)"),
        vitals_service: VitalsService = Depends(get_vitals_service)
):
    return await vitals_service.get_vitals_measure_history(patientId, start_time, end_time)