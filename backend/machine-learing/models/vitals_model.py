import uuid

from sqlalchemy import Column, Integer, String, Float, DateTime
from sqlalchemy.orm import declarative_base

Base = declarative_base()

class VitalMeasurement(Base):
    __tablename__ = "vital_measurements"
    id = Column(String, primary_key=True, index=True, default=lambda: str(uuid.uuid4()))
    patient_id = Column(String, index=True, nullable=False)
    timestamp = Column(DateTime(timezone=True), nullable=False)

    heart_rate = Column(Integer, nullable=False)
    sys_bp = Column(Integer, nullable=False)
    dia_bp = Column(Integer, nullable=False)
    temperature = Column(Float, nullable=False)
    spO2 = Column(Integer, nullable=False)