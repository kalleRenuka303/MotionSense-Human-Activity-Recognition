import os
from pathlib import Path
from typing import List
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "MotionSense"
    API_V1_STR: str = "/api"
    BACKEND_CORS_ORIGINS: List[str] = [
        "http://localhost:5173",
        "http://localhost:3000",
        "https://motionsense.vercel.app",
        "*"
    ]
    
    BASE_DIR: Path = Path(__file__).resolve().parent.parent.parent.parent
    DATASET_DIR: Path = BASE_DIR / "ml" / "data" / "raw" / "UCI HAR Dataset"
    ARTIFACTS_DIR: Path = BASE_DIR / "artifacts"
    CHECKPOINTS_DIR: Path = ARTIFACTS_DIR / "checkpoints"
    METRICS_DIR: Path = ARTIFACTS_DIR / "metrics"
    PLOTS_DIR: Path = ARTIFACTS_DIR / "plots"
    METADATA_DIR: Path = ARTIFACTS_DIR / "metadata"

    DATABASE_URL: str = "sqlite:///./motionsense.db"
    CONFIDENCE_UNCERTAINTY_THRESHOLD: float = 0.55

    class Config:
        env_file = ".env"
        extra = "allow"

settings = Settings()
