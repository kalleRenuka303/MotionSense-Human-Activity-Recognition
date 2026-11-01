"""
Pipeline A — Feature-Based Data Ingestion and Preprocessing (Random Forest)
Processes the 561 pre-computed time and frequency domain features.
"""

import sys
import numpy as np
import pandas as pd
from pathlib import Path
from typing import Tuple, Dict, List, Optional
from ml.configs.config import (
    DATA_DIR, 
    VAL_SUBJECTS, 
    ACTIVITY_MAP, 
    NUM_FEATURES_PIPELINE_A
)

def load_feature_names(data_dir: Path = DATA_DIR) -> List[str]:
    features_path = data_dir / "features.txt"
    if not features_path.exists():
        raise FileNotFoundError(f"features.txt not found at {features_path}")
    
    feature_names = []
    with open(features_path, "r", encoding="utf-8") as f:
        for line in f:
            parts = line.strip().split(maxsplit=1)
            if len(parts) == 2:
                feature_names.append(parts[1])
            elif len(parts) == 1:
                feature_names.append(parts[0])
    
    if len(feature_names) != NUM_FEATURES_PIPELINE_A:
        raise ValueError(f"Expected {NUM_FEATURES_PIPELINE_A} features, found {len(feature_names)}")
    return feature_names

def load_pipeline_a_split(split: str, data_dir: Path = DATA_DIR) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
    """
    Loads X, y, and subject arrays for a given split ('train' or 'test').
    Returns:
        X: np.ndarray of shape (N, 561), float32
        y: np.ndarray of shape (N,), int64 (0-indexed: 0..5)
        subjects: np.ndarray of shape (N,), int64
    """
    split_dir = data_dir / split
    x_path = split_dir / f"X_{split}.txt"
    y_path = split_dir / f"y_{split}.txt"
    sub_path = split_dir / f"subject_{split}.txt"

    if not x_path.exists() or not y_path.exists() or not sub_path.exists():
        raise FileNotFoundError(f"Missing split files in {split_dir}")

    # Load numerical arrays
    X = np.loadtxt(x_path, dtype=np.float32)
    y_raw = np.loadtxt(y_path, dtype=np.int64)
    subjects = np.loadtxt(sub_path, dtype=np.int64)

    # Convert 1-indexed (1..6) to 0-indexed (0..5)
    y = y_raw - 1

    # Validate shapes and numerical integrity
    if X.ndim != 2 or X.shape[1] != NUM_FEATURES_PIPELINE_A:
        raise ValueError(f"Unexpected X shape: {X.shape}. Expected (N, {NUM_FEATURES_PIPELINE_A})")
    if len(X) != len(y) or len(X) != len(subjects):
        raise ValueError(f"Mismatch in row counts: X={len(X)}, y={len(y)}, subjects={len(subjects)}")
    if np.isnan(X).any() or np.isinf(X).any():
        raise ValueError(f"Dataset contains NaN or Inf values in {split} set.")

    return X, y, subjects

def get_train_val_test_data(data_dir: Path = DATA_DIR) -> Dict[str, np.ndarray]:
    """
    Loads the official training set and partitions it into subject-independent
    train and validation subsets, plus loads the official untouched test set.
    """
    X_train_full, y_train_full, sub_train_full = load_pipeline_a_split("train", data_dir)
    X_test, y_test, sub_test = load_pipeline_a_split("test", data_dir)

    val_mask = np.isin(sub_train_full, VAL_SUBJECTS)
    train_mask = ~val_mask

    X_train, y_train, sub_train = X_train_full[train_mask], y_train_full[train_mask], sub_train_full[train_mask]
    X_val, y_val, sub_val = X_train_full[val_mask], y_train_full[val_mask], sub_train_full[val_mask]

    # Verify zero leakage
    train_subs = set(sub_train)
    val_subs = set(sub_val)
    test_subs = set(sub_test)

    assert len(train_subs.intersection(val_subs)) == 0, "Data leakage detected: train and val subjects overlap!"
    assert len(train_subs.intersection(test_subs)) == 0, "Data leakage detected: train and test subjects overlap!"

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

def validate_feature_csv(df: pd.DataFrame, expected_features: Optional[List[str]] = None) -> Dict[str, any]:
    """
    Validates user-uploaded CSV containing 561 features.
    """
    errors = []
    warnings = []
    
    # Check if target activity label column exists
    label_col = None
    for cand in ["activity", "label", "Activity", "Label", "y"]:
        if cand in df.columns:
            label_col = cand
            break

    # Check subject column
    subject_col = None
    for cand in ["subject", "Subject", "subject_id", "Subject_ID"]:
        if cand in df.columns:
            subject_col = cand
            break

    feature_cols = [c for c in df.columns if c not in [label_col, subject_col]]
    
    if len(feature_cols) != NUM_FEATURES_PIPELINE_A:
        errors.append(f"Expected {NUM_FEATURES_PIPELINE_A} feature columns, but found {len(feature_cols)}.")

    # Check for NaN / Inf
    numeric_df = df[feature_cols].apply(pd.to_numeric, errors='coerce')
    nan_count = numeric_df.isna().sum().sum()
    if nan_count > 0:
        errors.append(f"Found {nan_count} non-numeric or missing (NaN) values in feature columns.")

    return {
        "valid": len(errors) == 0,
        "errors": errors,
        "warnings": warnings,
        "num_samples": len(df),
        "num_features": len(feature_cols),
        "has_ground_truth": label_col is not None,
        "label_col": label_col,
        "subject_col": subject_col,
        "feature_cols": feature_cols
    }
