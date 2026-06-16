from fastapi import FastAPI
from api.vitals_controller import router as vitals_router

app = FastAPI(title="HealthMonitor API")

app.include_router(vitals_router)