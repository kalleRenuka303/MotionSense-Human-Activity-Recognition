import io
import json
import numpy as np
import pandas as pd
from typing import Optional
from fastapi import APIRouter, UploadFile, File, Form, HTTPException, Depends
from sqlalchemy.orm import Session

from backend.app.core.config import settings
from backend.app.database.session import get_db
from backend.app.database.models import PredictionRecord
from backend.app.services.model_service import model_service
from backend.app.schemas.predict import (
    ValidationResponse, 
    PredictionResponse, 
    PredictionSummary
)
from ml.configs.config import SENSOR_CHANNELS, NUM_FEATURES_PIPELINE_A
from ml.preprocessing.pipeline_a import validate_feature_csv
from ml.preprocessing.pipeline_b import validate_raw_sensor_csv, create_windows_from_raw_timeseries

router = APIRouter()

@router.post("/validate", response_model=ValidationResponse)
async def validate_uploaded_csv(file: UploadFile = File(...)):
    filename = file.filename or ""
    if not filename.lower().endswith(".csv"):
        return ValidationResponse(
            valid=False,
            num_samples_or_windows=0,
            detected_channels_or_features=0,
            has_ground_truth=False,
            errors=["Uploaded file must be a CSV (.csv extension)."]
        )

    content = await file.read()
    if len(content) == 0:
        return ValidationResponse(
            valid=False,
            num_samples_or_windows=0,
            detected_channels_or_features=0,
            has_ground_truth=False,
            errors=["The uploaded CSV file is empty (0 bytes). Please upload a file containing sensor recordings."]
        )

    try:
        df = pd.read_csv(io.BytesIO(content))
    except Exception as e:
        return ValidationResponse(
            valid=False,
            num_samples_or_windows=0,
            detected_channels_or_features=0,
            has_ground_truth=False,
            errors=[f"Error parsing CSV content: {str(e)}"]
        )

    # Detect whether it matches Pipeline A (561 features) or Pipeline B (9 sensor channels)
    has_9_channels = all(ch in df.columns for ch in SENSOR_CHANNELS)
    feature_cols = [c for c in df.columns if c.lower() not in ["activity", "label", "subject", "subject_id", "timestamp", "timestamp_sec"]]

    if has_9_channels:
        res = validate_raw_sensor_csv(df)
        return ValidationResponse(
            valid=res["valid"],
            input_format="raw_inertial",
            num_samples_or_windows=res["total_rows"],
            detected_channels_or_features=len(res["channels_present"]),
            has_ground_truth=res["has_ground_truth"],
            errors=res["errors"],
            warnings=res["warnings"]
        )
    elif len(feature_cols) == NUM_FEATURES_PIPELINE_A or len(feature_cols) >= 500:
        res = validate_feature_csv(df)
        return ValidationResponse(
            valid=res["valid"],
            input_format="features",
            num_samples_or_windows=res["num_samples"],
            detected_channels_or_features=res["num_features"],
            has_ground_truth=res["has_ground_truth"],
            errors=res["errors"],
            warnings=res["warnings"]
        )
    else:
        return ValidationResponse(
            valid=False,
            input_format=None,
            num_samples_or_windows=len(df),
            detected_channels_or_features=len(df.columns),
            has_ground_truth=False,
            errors=[
                f"CSV does not match either Pipeline A (561 statistical features) or Pipeline B (9 raw inertial channels). Found {len(df.columns)} columns."
            ]
        )

@router.post("/features", response_model=PredictionResponse)
async def predict_features_csv(
    file: UploadFile = File(...),
    db: Session = Depends(get_db)
):
    content = await file.read()
    try:
        df = pd.read_csv(io.BytesIO(content))
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid CSV: {str(e)}")

    validation = validate_feature_csv(df)
    if not validation["valid"]:
        raise HTTPException(status_code=400, detail=f"Validation errors: {', '.join(validation['errors'])}")

    feature_cols = validation["feature_cols"]
    X = df[feature_cols].to_numpy(dtype=np.float32)

    ground_truth = None
    if validation["has_ground_truth"]:
        ground_truth = [str(x) for x in df[validation["label_col"]].tolist()]

    timeline, summary, latency_ms = model_service.predict_features(X, ground_truth=ground_truth)

    # Persist in DB
    record = PredictionRecord(
        filename=file.filename or "uploaded_features.csv",
        input_format="features",
        selected_model="Random Forest",
        num_windows=summary.total_windows,
        avg_confidence=summary.average_confidence,
        activity_distribution_json=json.dumps(summary.activity_counts),
        timeline_summary_json=json.dumps([t.dict() for t in timeline[:100]]),  # cap initial stored timeline
        ground_truth_accuracy=summary.ground_truth_accuracy
    )
    db.add(record)
    db.commit()
    db.refresh(record)

    return PredictionResponse(
        prediction_id=record.id,
        filename=record.filename,
        input_format="features",
        model_name="Random Forest",
        inference_time_ms=round(latency_ms, 2),
        summary=summary,
        timeline=timeline
    )

@router.post("/raw", response_model=PredictionResponse)
async def predict_raw_csv(
    file: UploadFile = File(...),
    model_name: str = Form("cnn_lstm"),
    db: Session = Depends(get_db)
):
    content = await file.read()
    try:
        df = pd.read_csv(io.BytesIO(content))
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid CSV: {str(e)}")

    validation = validate_raw_sensor_csv(df)
    if not validation["valid"]:
        raise HTTPException(status_code=400, detail=f"Validation errors: {', '.join(validation['errors'])}")

    try:
        windows, labels = create_windows_from_raw_timeseries(df)
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Windowing error: {str(e)}")

    ground_truth = [str(x) for x in labels] if labels is not None else None
    timeline, summary, latency_ms = model_service.predict_raw(
        windows, 
        model_name=model_name, 
        ground_truth=ground_truth
    )

    selected_model_name = "1D CNN" if "cnn_1d" in model_name or "1d_cnn" in model_name else "CNN-LSTM"

    # Persist in DB
    record = PredictionRecord(
        filename=file.filename or "uploaded_raw.csv",
        input_format="raw_inertial",
        selected_model=selected_model_name,
        num_windows=summary.total_windows,
        avg_confidence=summary.average_confidence,
        activity_distribution_json=json.dumps(summary.activity_counts),
        timeline_summary_json=json.dumps([t.dict() for t in timeline[:100]]),
        ground_truth_accuracy=summary.ground_truth_accuracy
    )
    db.add(record)
    db.commit()
    db.refresh(record)

    return PredictionResponse(
        prediction_id=record.id,
        filename=record.filename,
        input_format="raw_inertial",
        model_name=selected_model_name,
        inference_time_ms=round(latency_ms, 2),
        summary=summary,
        timeline=timeline
    )
