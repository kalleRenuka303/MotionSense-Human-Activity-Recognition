import pytest
from pathlib import Path
from ml.configs.config import (
    DATA_DIR, 
    ACTIVITY_MAP, 
    SENSOR_CHANNELS, 
    WINDOW_SIZE, 
    NUM_FEATURES_PIPELINE_A
)

def test_dataset_directory_exists():
    assert DATA_DIR.exists(), f"Dataset directory {DATA_DIR} does not exist"

def test_features_file():
    features_path = DATA_DIR / "features.txt"
    assert features_path.exists()
    with open(features_path, "r", encoding="utf-8") as f:
        lines = [line.strip() for line in f if line.strip()]
    assert len(lines) == NUM_FEATURES_PIPELINE_A, f"Expected 561 features, got {len(lines)}"

def test_activity_labels():
    labels_path = DATA_DIR / "activity_labels.txt"
    assert labels_path.exists()
    with open(labels_path, "r", encoding="utf-8") as f:
        lines = [line.strip().split() for line in f if line.strip()]
    mapping = {int(p[0]): p[1] for p in lines}
    assert mapping == ACTIVITY_MAP

def test_raw_inertial_signals_exist():
    for split in ["train", "test"]:
        signals_dir = DATA_DIR / split / "Inertial Signals"
        assert signals_dir.exists(), f"Inertial signals missing for {split}"
        for ch in SENSOR_CHANNELS:
            ch_file = signals_dir / f"{ch}_{split}.txt"
            assert ch_file.exists(), f"Channel file {ch_file.name} missing"
