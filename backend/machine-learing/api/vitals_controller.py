from fastapi import APIRouter, Depends
from influxdb_client.client.influxdb_client_async import InfluxDBClientAsync

from schemas.vitals_schema import VitalsPayload
from dependencies.influx import get_influx_client
from repositories.vitals_repository import VitalsRepository
from services.vitals_service import VitalsService

router = APIRouter()

def get_vitals_service(influx_client: InfluxDBClientAsync = Depends(get_influx_client)) -> VitalsService:
    repository = VitalsRepository(influx_client)
    return VitalsService(repository)

@router.post('/api/vitals')
async def receive_vitals(payload: VitalsPayload, vitals_service: VitalsService = Depends(get_vitals_service)):
    result = await vitals_service.process_vitals(payload)
    return result