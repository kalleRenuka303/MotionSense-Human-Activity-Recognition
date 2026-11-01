from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from backend.app.core.config import settings
from backend.app.database.session import engine, Base
from backend.app.services.model_service import model_service
from backend.app.api.endpoints import health, dataset, models, predict, evaluation, history

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize DB tables
    Base.metadata.create_all(bind=engine)
    # Load model checkpoints
    model_service.load_models()
    yield

app = FastAPI(
    title="MotionSense API",
    description="Human Activity Recognition Using Smartphones (UCI HAR Dataset 240)",
    version="1.0.0",
    lifespan=lifespan
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Routers
app.include_router(health.router, prefix="/api", tags=["Health"])
app.include_router(dataset.router, prefix="/api/dataset", tags=["Dataset"])
app.include_router(models.router, prefix="/api/models", tags=["Models"])
app.include_router(predict.router, prefix="/api/predict", tags=["Prediction"])
app.include_router(evaluation.router, prefix="/api/evaluation", tags=["Evaluation"])
app.include_router(history.router, prefix="/api/history", tags=["History"])

# Data validation router alias
app.include_router(predict.router, prefix="/api/data", tags=["Data Validation"])

@app.get("/")
def root():
    return {
        "message": "MotionSense HAR API is running",
        "docs_url": "/docs",
        "api_prefix": "/api"
    }
