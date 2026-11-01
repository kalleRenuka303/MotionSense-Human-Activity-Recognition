"""
Pipeline B — Raw Inertial Signal Data Ingestion & Preprocessing (1D CNN & CNN-LSTM)
Processes the 9 tri-axial inertial signal channels (128 time steps per window).
"""

import sys
import numpy as np
import pandas as pd
from pathlib import Path
from typing import Tuple, Dict, List, Optional
from ml.configs.config import (
    DATA_DIR, 
    VAL_SUBJECTS, 
    SENSOR_CHANNELS, 
    WINDOW_SIZE, 
    NUM_CHANNELS
)

def load_inertial_signals(split: str, data_dir: Path = DATA_DIR) -> np.ndarray:
    """
    Loads and stacks the 9 inertial signal files for a given split ('train' or 'test').
    Returns:
        signals: np.ndarray of shape (N, 128, 9), float32
    """
    signals_dir = data_dir / split / "Inertial Signals"
    if not signals_dir.exists():
        raise FileNotFoundError(f"Inertial Signals directory not found: {signals_dir}")

    channel_arrays = []
    for ch in SENSOR_CHANNELS:
        file_path = signals_dir / f"{ch}_{split}.txt"
        if not file_path.exists():
            raise FileNotFoundError(f"Channel file {file_path.name} not found in {signals_dir}")
        arr = np.loadtxt(file_path, dtype=np.float32)  # shape: (N, 128)
        if arr.ndim != 2 or arr.shape[1] != WINDOW_SIZE:
            raise ValueError(f"Channel {ch} has invalid shape {arr.shape}. Expected (N, {WINDOW_SIZE})")
        channel_arrays.append(arr)

    # Stack along axis 2 -> (N, 128, 9)
    signals = np.stack(channel_arrays, axis=-1)
    if np.isnan(signals).any() or np.isinf(signals).any():
        raise ValueError(f"Inertial signals contain NaN or Inf values in {split} set.")

    return signals

def load_pipeline_b_split(split: str, data_dir: Path = DATA_DIR) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
    """
    Loads raw inertial signals, labels, and subjects.
    Returns:
        X: np.ndarray of shape (N, 128, 9), float32
        y: np.ndarray of shape (N,), int64 (0-indexed: 0..5)
        subjects: np.ndarray of shape (N,), int64
    """
    signals = load_inertial_signals(split, data_dir)
    y_raw = np.loadtxt(data_dir / split / f"y_{split}.txt", dtype=np.int64)
    subjects = np.loadtxt(data_dir / split / f"subject_{split}.txt", dtype=np.int64)

    y = y_raw - 1
    assert len(signals) == len(y) == len(subjects), "Inconsistent sample counts across signals, labels, and subjects"
    return signals, y, subjects

def get_train_val_test_raw_data(data_dir: Path = DATA_DIR) -> Dict[str, np.ndarray]:
    """
    Partitions raw inertial data into subject-independent train, val, and test splits.
    """
    X_train_full, y_train_full, sub_train_full = load_pipeline_b_split("train", data_dir)
    X_test, y_test, sub_test = load_pipeline_b_split("test", data_dir)

    val_mask = np.isin(sub_train_full, VAL_SUBJECTS)
    train_mask = ~val_mask

    X_train, y_train, sub_train = X_train_full[train_mask], y_train_full[train_mask], sub_train_full[train_mask]
    X_val, y_val, sub_val = X_train_full[val_mask], y_train_full[val_mask], sub_train_full[val_mask]

    # Verify zero leakage
    train_subs = set(sub_train)
    val_subs = set(sub_val)
    test_subs = set(sub_test)

    assert len(train_subs.intersection(val_subs)) == 0, "Data leakage: train and val subjects overlap!"
    assert len(train_subs.intersection(test_subs)) == 0, "Data leakage: train and test subjects overlap!"

    return {
        "X_train": X_train,
        "y_train": y_train,
        "sub_train": sub_train,
        "X_val": X_val,
        "y_val": y_val,
        "sub_val": sub_val,
        "X_test": X_test,
        "y_test": y_test,
        "sub_test": sub_test
    }

def create_windows_from_raw_timeseries(
    df: pd.DataFrame, 
    window_size: int = WINDOW_SIZE, 
    step_size: int = 64
) -> Tuple[np.ndarray, Optional[np.ndarray]]:
    """
    Slices a continuous multi-channel raw sensor recording into windows of shape (num_windows, 128, 9).
    Optional ground truth is extracted via majority voting per window.
    """
    # Verify required 9 channels are present
    missing = [ch for ch in SENSOR_CHANNELS if ch not in df.columns]
    if missing:
        raise ValueError(f"Missing required sensor channels: {missing}")

    data_matrix = df[SENSOR_CHANNELS].to_numpy(dtype=np.float32)
    n_samples = len(data_matrix)
    
    if n_samples < window_size:
        raise ValueError(f"Recording has only {n_samples} samples; minimum required is {window_size} (one window).")

    windows = []
    labels = []
    
    has_labels = False
    label_col = None
    for cand in ["activity", "label", "Activity", "Label", "y"]:
        if cand in df.columns:
            has_labels = True
            label_col = cand
            break

    for start in range(0, n_samples - window_size + 1, step_size):
        end = start + window_size
        win = data_matrix[start:end, :]  # shape: (128, 9)
        windows.append(win)

        if has_labels:
            # Majority voting for window label
            win_labels = df[label_col].iloc[start:end]
            mode_val = win_labels.mode()[0]
            labels.append(mode_val)

    windows_arr = np.array(windows, dtype=np.float32)
    labels_arr = np.array(labels) if has_labels else None

    return windows_arr, labels_arr

def validate_raw_sensor_csv(df: pd.DataFrame) -> Dict[str, any]:
    """
    Validates user-uploaded raw sensor CSV.
    """
    errors = []
    warnings = []

    missing_channels = [ch for ch in SENSOR_CHANNELS if ch not in df.columns]
    if missing_channels:
        errors.append(f"Missing mandatory sensor channel(s): {', '.join(missing_channels)}")

    # Check for NaN / Inf
    if not missing_channels:
        sensor_data = df[SENSOR_CHANNELS].apply(pd.to_numeric, errors='coerce')
        nan_count = sensor_data.isna().sum().sum()
        if nan_count > 0:
            errors.append(f"Found {nan_count} non-numeric or missing (NaN) values in sensor columns.")

    if len(df) < WINDOW_SIZE:
        errors.append(f"File contains {len(df)} rows. At least {WINDOW_SIZE} rows are required for 1 window.")

    has_labels = any(cand in df.columns for cand in ["activity", "label", "Activity", "Label", "y"])

    return {
        "valid": len(errors) == 0,
        "errors": errors,
        "warnings": warnings,
        "total_rows": len(df),
        "channels_present": [ch for ch in SENSOR_CHANNELS if ch in df.columns],
        "has_ground_truth": has_labels
    }
