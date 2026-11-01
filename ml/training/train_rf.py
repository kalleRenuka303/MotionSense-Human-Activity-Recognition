"""
Reproducible Training Pipeline for Model 1: Random Forest (Pipeline A)
"""

import time
import json
import numpy as np
from pathlib import Path
from ml.configs.config import (
    RFConfig, 
    CHECKPOINTS_DIR, 
    METRICS_DIR, 
    ACTIVITY_NAMES
)
from ml.preprocessing.pipeline_a import (
    get_train_val_test_data, 
    load_feature_names
)
from ml.models.random_forest_model import MotionSenseRandomForest

METRICS_DIR.mkdir(parents=True, exist_ok=True)
CHECKPOINTS_DIR.mkdir(parents=True, exist_ok=True)

def train_random_forest() -> dict:
    print("\n=======================================================")
    print("  Training Model 1: Random Forest Baseline (561 features)")
    print("=======================================================")

    feature_names = load_feature_names()
    data = get_train_val_test_data()

    X_train, y_train = data["X_train"], data["y_train"]
    X_val, y_val = data["X_val"], data["y_val"]
    X_test, y_test = data["X_test"], data["y_test"]

    print(f"[*] Training samples: {len(X_train)} across subjects: {sorted(list(set(data['sub_train'])))}")
    print(f"[*] Validation samples: {len(X_val)} across subjects: {sorted(list(set(data['sub_val'])))}")
    print(f"[*] Test samples: {len(X_test)} across subjects: {sorted(list(set(data['sub_test'])))}")

    config = RFConfig()
    model = MotionSenseRandomForest(config=config)

    start_time = time.time()
    model.fit(X_train, y_train, feature_names=feature_names)
    train_duration = time.time() - start_time
    print(f"[+] Fitting completed in {train_duration:.2f} seconds.")

    # Validation evaluation (Subject-aware)
    val_metrics = model.evaluate(X_val, y_val)
    print(f"[+] Validation Accuracy: {val_metrics['accuracy']*100:.2f}% | Macro F1: {val_metrics['macro_f1']*100:.2f}%")

    # Final Test evaluation
    test_metrics = model.evaluate(X_test, y_test)
    print(f"[+] Official Test Accuracy: {test_metrics['accuracy']*100:.2f}% | Macro F1: {test_metrics['macro_f1']*100:.2f}%")

    # Latency benchmarking
    bench_samples = X_test[:100]
    t0 = time.perf_counter()
    for _ in range(50):
        _ = model.predict(bench_samples)
    bench_time = (time.perf_counter() - t0) / (50 * len(bench_samples)) * 1000  # ms per sample

    # Feature Importance
    top_features = model.get_feature_importances(top_k=25)

    # Save checkpoint
    model.save()

    results = {
        "model_name": "Random Forest",
        "pipeline": "Pipeline A (561 Features)",
        "train_samples": int(len(X_train)),
        "val_samples": int(len(X_val)),
        "test_samples": int(len(X_test)),
        "train_duration_sec": round(train_duration, 3),
        "inference_latency_ms": round(bench_time, 4),
        "validation_metrics": val_metrics,
        "test_metrics": test_metrics,
        "top_features": top_features,
        "hyperparameters": {
            "n_estimators": config.n_estimators,
            "max_depth": config.max_depth,
            "min_samples_split": config.min_samples_split,
            "min_samples_leaf": config.min_samples_leaf,
            "random_state": config.random_state
        }
    }

    with open(METRICS_DIR / "rf_metrics.json", "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)

    with open(METRICS_DIR / "rf_feature_importance.json", "w", encoding="utf-8") as f:
        json.dump(top_features, f, indent=2)

    print(f"[+] Metrics saved to {METRICS_DIR / 'rf_metrics.json'}")
    return results

if __name__ == "__main__":
    train_random_forest()
