import json
from pathlib import Path
from fastapi import APIRouter, HTTPException
from fastapi.responses import Response, FileResponse

from backend.app.core.config import settings
from backend.app.schemas.predict import DatasetStatsResponse
from ml.configs.config import ACTIVITY_NAMES, SENSOR_CHANNELS, NUM_FEATURES_PIPELINE_A

router = APIRouter()

@router.get("/info", response_model=DatasetStatsResponse)
def get_dataset_info():
    verification_path = settings.METADATA_DIR / "dataset_verification.json"
    status_str = "Verified" if verification_path.exists() else "Pending Verification"

    return DatasetStatsResponse(
        dataset_name="Human Activity Recognition Using Smartphones (UCI HAR Dataset 240)",
        source_url="https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones",
        total_windows=10299,
        train_windows=7352,
        test_windows=2947,
        features_count=NUM_FEATURES_PIPELINE_A,
        raw_channels_count=len(SENSOR_CHANNELS),
        sampling_rate_hz=50,
        window_length_samples=128,
        activities=ACTIVITY_NAMES,
        train_subjects_count=21,
        test_subjects_count=9,
        verification_status=status_str
    )

@router.get("/sample/features")
def download_sample_feature_csv():
    """Generates and downloads a valid 561-feature sample CSV."""
    x_test_path = settings.DATASET_DIR / "test" / "X_test.txt"
    y_test_path = settings.DATASET_DIR / "test" / "y_test.txt"
    features_path = settings.DATASET_DIR / "features.txt"

    if not x_test_path.exists():
        raise HTTPException(status_code=404, detail="Official dataset not downloaded yet.")

    # Load first 20 rows of test set
    import numpy as np
    import pandas as pd
    import io
    from ml.preprocessing.pipeline_a import load_feature_names

    X_sample = np.loadtxt(x_test_path, max_rows=20)
    y_sample = np.loadtxt(y_test_path, max_rows=20, dtype=int)
    feature_names = load_feature_names()

    df = pd.DataFrame(X_sample, columns=feature_names)
    df["activity"] = [ACTIVITY_NAMES[label - 1] for label in y_sample]

    buffer = io.StringIO()
    df.to_csv(buffer, index=False)
    
    return Response(
        content=buffer.getvalue(),
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=motionsense_sample_features.csv"}
    )

@router.get("/sample/raw")
def download_sample_raw_csv():
    """Generates and downloads a valid 9-channel raw sensor sample CSV."""
    signals_dir = settings.DATASET_DIR / "test" / "Inertial Signals"
    if not signals_dir.exists():
        raise HTTPException(status_code=404, detail="Inertial signals not downloaded yet.")

    import numpy as np
    import pandas as pd
    import io

    # Read first 3 windows (384 timesteps)
    channels_data = {}
    for ch in SENSOR_CHANNELS:
        ch_file = signals_dir / f"{ch}_test.txt"
        arr = np.loadtxt(ch_file, max_rows=3)  # shape (3, 128)
        channels_data[ch] = arr.flatten()

    df = pd.DataFrame(channels_data)
    # Add timestamp assuming 50Hz (0.02s per sample)
    df["timestamp_sec"] = np.round(np.arange(len(df)) * 0.02, 3)

    buffer = io.StringIO()
    df.to_csv(buffer, index=False)

    return Response(
        content=buffer.getvalue(),
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=motionsense_sample_raw_sensors.csv"}
    )
