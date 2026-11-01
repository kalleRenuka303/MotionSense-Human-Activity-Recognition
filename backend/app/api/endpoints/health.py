from fastapi import APIRouter
from backend.app.core.config import settings

router = APIRouter()

@router.get("/health")
def health_check():
    return {
        "status": "healthy",
        "service": settings.PROJECT_NAME,
        "dataset_ready": settings.DATASET_DIR.exists(),
        "checkpoints_dir_exists": settings.CHECKPOINTS_DIR.exists()
    }
