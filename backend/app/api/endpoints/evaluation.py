import json
from pathlib import Path
from fastapi import APIRouter, HTTPException
from fastapi.responses import FileResponse

from backend.app.core.config import settings

router = APIRouter()

@router.get("/summary")
def get_evaluation_summary():
    report_path = settings.METRICS_DIR / "comprehensive_evaluation.json"
    if not report_path.exists():
        # Fallback to individual metrics if comprehensive is not generated
        rf_file = settings.METRICS_DIR / "rf_metrics.json"
        cnn_file = settings.METRICS_DIR / "1d_cnn_metrics.json"
        lstm_file = settings.METRICS_DIR / "cnn_lstm_metrics.json"

        rf_data = json.load(open(rf_file)) if rf_file.exists() else None
        cnn_data = json.load(open(cnn_file)) if cnn_file.exists() else None
        lstm_data = json.load(open(lstm_file)) if lstm_file.exists() else None

        return {
            "status": "partial" if any([rf_data, cnn_data, lstm_data]) else "pending",
            "models": {
                "random_forest": rf_data,
                "1d_cnn": cnn_data,
                "cnn_lstm": lstm_data
            }
        }

    with open(report_path, "r", encoding="utf-8") as f:
        return json.load(f)

@router.get("/feature-importance")
def get_rf_feature_importance():
    fi_file = settings.METRICS_DIR / "rf_feature_importance.json"
    if not fi_file.exists():
        raise HTTPException(status_code=404, detail="Feature importance artifact not generated yet.")
    with open(fi_file, "r", encoding="utf-8") as f:
        return json.load(f)

@router.get("/plots/{plot_name}")
def get_evaluation_plot(plot_name: str):
    allowed_plots = [
        "model_comparison.png",
        "cm_random_forest.png",
        "cm_1d_cnn.png",
        "cm_cnn_lstm.png",
        "training_curves.png"
    ]
    if plot_name not in allowed_plots:
        raise HTTPException(status_code=400, detail="Invalid plot name.")

    file_path = settings.PLOTS_DIR / plot_name
    if not file_path.exists():
        raise HTTPException(status_code=404, detail=f"Plot {plot_name} not found.")

    return FileResponse(file_path, media_type="image/png")
