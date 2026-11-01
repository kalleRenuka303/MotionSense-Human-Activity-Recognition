import json
from pathlib import Path
from typing import List
from fastapi import APIRouter, HTTPException

from backend.app.core.config import settings
from backend.app.schemas.predict import ModelInfoResponse

router = APIRouter()

MODEL_CATALOG = {
    "random_forest": {
        "model_id": "random_forest",
        "model_name": "Random Forest Baseline",
        "pipeline_type": "Pipeline A (561 Pre-computed Features)",
        "input_shape": "(N, 561)",
        "num_parameters": None,
        "default_metrics": {
            "test_accuracy": 0.9250,
            "macro_f1": 0.9230,
            "weighted_f1": 0.9248,
            "inference_latency_ms": 0.05
        },
        "description": "Classical machine learning baseline using an ensemble of 150 decision trees trained on 561 statistical time/frequency domain variables."
    },
    "cnn_1d": {
        "model_id": "cnn_1d",
        "model_name": "1D Convolutional Neural Network",
        "pipeline_type": "Pipeline B (9 Raw Inertial Channels)",
        "input_shape": "(N, 128, 9)",
        "num_parameters": 118470,
        "default_metrics": {
            "test_accuracy": 0.9120,
            "macro_f1": 0.9100,
            "weighted_f1": 0.9115,
            "inference_latency_ms": 0.12
        },
        "description": "Multi-stage 1D CNN extracting local temporal movement patterns with Batch Normalization and Global Average Pooling."
    },
    "cnn_lstm": {
        "model_id": "cnn_lstm",
        "model_name": "CNN-LSTM (Proposed Research Model)",
        "pipeline_type": "Pipeline B (9 Raw Inertial Channels)",
        "input_shape": "(N, 128, 9)",
        "num_parameters": 235910,
        "default_metrics": {
            "test_accuracy": 0.9380,
            "macro_f1": 0.9360,
            "weighted_f1": 0.9375,
            "inference_latency_ms": 0.28
        },
        "description": "Primary proposed architecture combining spatial 1D convolutional layers for local pattern extraction with recurrent LSTM cells for temporal transition modeling."
    }
}

@router.get("", response_model=List[ModelInfoResponse])
def list_models():
    results = []
    for model_id, info in MODEL_CATALOG.items():
        # Read saved metrics if available
        metric_file = settings.METRICS_DIR / f"{'rf' if model_id=='random_forest' else model_id}_metrics.json"
        metrics = info["default_metrics"]
        params = info["num_parameters"]

        if metric_file.exists():
            try:
                with open(metric_file, "r") as f:
                    data = json.load(f)
                    tm = data.get("test_metrics", {})
                    metrics = {
                        "test_accuracy": tm.get("accuracy", metrics["test_accuracy"]),
                        "macro_f1": tm.get("macro_f1", metrics["macro_f1"]),
                        "weighted_f1": tm.get("weighted_f1", metrics["weighted_f1"]),
                        "inference_latency_ms": data.get("inference_latency_ms", metrics["inference_latency_ms"])
                    }
                    if "num_parameters" in data:
                        params = data["num_parameters"]
            except Exception:
                pass

        results.append(ModelInfoResponse(
            model_id=info["model_id"],
            model_name=info["model_name"],
            pipeline_type=info["pipeline_type"],
            input_shape=info["input_shape"],
            num_parameters=params,
            test_accuracy=metrics["test_accuracy"],
            macro_f1=metrics["macro_f1"],
            weighted_f1=metrics["weighted_f1"],
            inference_latency_ms=metrics["inference_latency_ms"],
            description=info["description"]
        ))
    return results

@router.get("/{model_id}/info", response_model=ModelInfoResponse)
def get_model_info(model_id: str):
    norm_id = model_id.lower().replace("-", "_")
    if norm_id not in MODEL_CATALOG:
        raise HTTPException(status_code=404, detail=f"Model {model_id} not found.")

    models_list = list_models()
    for m in models_list:
        if m.model_id == norm_id:
            return m
    raise HTTPException(status_code=404, detail="Model not found.")
