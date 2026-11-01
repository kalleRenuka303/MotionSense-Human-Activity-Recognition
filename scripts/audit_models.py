#!/usr/bin/env python3
"""
Independent Model & Metric Verification Audit Script
Strictly reads existing saved checkpoints, runs test inference, and verifies metrics.
"""

import os
import time
import json
import torch
import joblib
import numpy as np
from pathlib import Path
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix, classification_report

from ml.configs.config import (
    CHECKPOINTS_DIR, 
    METRICS_DIR, 
    ACTIVITY_NAMES, 
    DATA_DIR
)
from ml.preprocessing.pipeline_a import load_pipeline_a_split
from ml.preprocessing.pipeline_b import load_pipeline_b_split
from ml.models.cnn_1d_model import MotionSense1DCNN
from ml.models.cnn_lstm_model import MotionSenseCNNLSTM
from ml.models.random_forest_model import MotionSenseRandomForest

def run_audit():
    device = torch.device("cpu")
    print("\n=======================================================")
    print("  INDEPENDENT MODEL & METRIC AUDIT VERIFICATION")
    print("=======================================================")

    results = {}

    # 1. Audit Random Forest
    rf_path = CHECKPOINTS_DIR / "random_forest.joblib"
    rf_size_kb = rf_path.stat().st_size / 1024
    print(f"\n[1] Auditing Random Forest ({rf_size_kb:.1f} KB)...")
    rf_model = MotionSenseRandomForest.load(rf_path)
    X_test_a, y_test_a, sub_test_a = load_pipeline_a_split("test")

    t0 = time.perf_counter()
    y_pred_rf = rf_model.predict(X_test_a)
    rf_latency_ms = ((time.perf_counter() - t0) / len(X_test_a)) * 1000

    rf_acc = accuracy_score(y_test_a, y_pred_rf)
    rf_prec = precision_score(y_test_a, y_pred_rf, average="macro")
    rf_rec = recall_score(y_test_a, y_pred_rf, average="macro")
    rf_f1_macro = f1_score(y_test_a, y_pred_rf, average="macro")
    rf_f1_weighted = f1_score(y_test_a, y_pred_rf, average="weighted")
    rf_cm = confusion_matrix(y_test_a, y_pred_rf).tolist()

    results["random_forest"] = {
        "file_size_kb": round(rf_size_kb, 1),
        "test_samples": len(y_test_a),
        "accuracy": float(rf_acc),
        "macro_precision": float(rf_prec),
        "macro_recall": float(rf_rec),
        "macro_f1": float(rf_f1_macro),
        "weighted_f1": float(rf_f1_weighted),
        "latency_ms_per_sample": round(rf_latency_ms, 4),
        "confusion_matrix": rf_cm
    }
    print(f"    - Accuracy: {rf_acc*100:.2f}% | Macro F1: {rf_f1_macro*100:.2f}% | Latency: {rf_latency_ms:.4f} ms")

    # Load Pipeline B test data for Deep Learning
    X_test_b, y_test_b, sub_test_b = load_pipeline_b_split("test")
    test_tensor = torch.from_numpy(X_test_b).to(device)

    # 2. Audit 1D CNN
    cnn_path = CHECKPOINTS_DIR / "best_1d_cnn.pt"
    cnn_size_kb = cnn_path.stat().st_size / 1024
    print(f"\n[2] Auditing 1D CNN ({cnn_size_kb:.1f} KB)...")
    cp_cnn = torch.load(cnn_path, map_location=device, weights_only=False)
    cnn_model = MotionSense1DCNN(cp_cnn.get("config")).to(device)
    cnn_model.load_state_dict(cp_cnn["model_state_dict"])
    cnn_model.eval()

    t0 = time.perf_counter()
    with torch.no_grad():
        logits_cnn = cnn_model(test_tensor)
        preds_cnn = torch.argmax(logits_cnn, dim=1).numpy()
    cnn_latency_ms = ((time.perf_counter() - t0) / len(test_tensor)) * 1000

    cnn_acc = accuracy_score(y_test_b, preds_cnn)
    cnn_prec = precision_score(y_test_b, preds_cnn, average="macro")
    cnn_rec = recall_score(y_test_b, preds_cnn, average="macro")
    cnn_f1_macro = f1_score(y_test_b, preds_cnn, average="macro")
    cnn_f1_weighted = f1_score(y_test_b, preds_cnn, average="weighted")
    cnn_cm = confusion_matrix(y_test_b, preds_cnn).tolist()

    results["cnn_1d"] = {
        "file_size_kb": round(cnn_size_kb, 1),
        "trainable_params": cnn_model.count_parameters(),
        "test_samples": len(y_test_b),
        "accuracy": float(cnn_acc),
        "macro_precision": float(cnn_prec),
        "macro_recall": float(cnn_rec),
        "macro_f1": float(cnn_f1_macro),
        "weighted_f1": float(cnn_f1_weighted),
        "latency_ms_per_sample": round(cnn_latency_ms, 4),
        "confusion_matrix": cnn_cm
    }
    print(f"    - Accuracy: {cnn_acc*100:.2f}% | Macro F1: {cnn_f1_macro*100:.2f}% | Latency: {cnn_latency_ms:.4f} ms")

    # 3. Audit CNN-LSTM
    lstm_path = CHECKPOINTS_DIR / "best_cnn_lstm.pt"
    lstm_size_kb = lstm_path.stat().st_size / 1024
    print(f"\n[3] Auditing CNN-LSTM ({lstm_size_kb:.1f} KB)...")
    cp_lstm = torch.load(lstm_path, map_location=device, weights_only=False)
    lstm_model = MotionSenseCNNLSTM(cp_lstm.get("config")).to(device)
    lstm_model.load_state_dict(cp_lstm["model_state_dict"])
    lstm_model.eval()

    t0 = time.perf_counter()
    with torch.no_grad():
        logits_lstm = lstm_model(test_tensor)
        preds_lstm = torch.argmax(logits_lstm, dim=1).numpy()
    lstm_latency_ms = ((time.perf_counter() - t0) / len(test_tensor)) * 1000

    lstm_acc = accuracy_score(y_test_b, preds_lstm)
    lstm_prec = precision_score(y_test_b, preds_lstm, average="macro")
    lstm_rec = recall_score(y_test_b, preds_lstm, average="macro")
    lstm_f1_macro = f1_score(y_test_b, preds_lstm, average="macro")
    lstm_f1_weighted = f1_score(y_test_b, preds_lstm, average="weighted")
    lstm_cm = confusion_matrix(y_test_b, preds_lstm).tolist()

    results["cnn_lstm"] = {
        "file_size_kb": round(lstm_size_kb, 1),
        "trainable_params": lstm_model.count_parameters(),
        "test_samples": len(y_test_b),
        "accuracy": float(lstm_acc),
        "macro_precision": float(lstm_prec),
        "macro_recall": float(lstm_rec),
        "macro_f1": float(lstm_f1_macro),
        "weighted_f1": float(lstm_f1_weighted),
        "latency_ms_per_sample": round(lstm_latency_ms, 4),
        "confusion_matrix": lstm_cm
    }
    print(f"    - Accuracy: {lstm_acc*100:.2f}% | Macro F1: {lstm_f1_macro*100:.2f}% | Latency: {lstm_latency_ms:.4f} ms")

    print("\n=======================================================")
    print("  AUDIT COMPLETED: All 3 models verified against test set")
    print("=======================================================\n")
    return results

if __name__ == "__main__":
    res = run_audit()
    with open("artifacts/metadata/independent_audit_metrics.json", "w") as f:
        json.dump(res, f, indent=2)
