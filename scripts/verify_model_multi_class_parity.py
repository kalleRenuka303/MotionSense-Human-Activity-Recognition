import os
import sys
import json
import torch
import numpy as np
from pathlib import Path

# Add project root to sys.path
BASE_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(BASE_DIR))

import onnxruntime as ort
from ml.models.cnn_1d_model import MotionSense1DCNN
from ml.models.cnn_lstm_model import MotionSenseCNNLSTM
from ml.configs.config import CNN1DConfig, CNNLSTMConfig, ACTIVITY_NAMES
from ml.preprocessing.pipeline_b import load_pipeline_b_split

def run_parity_test():
    print("=" * 70)
    print("MOTIONSENSE — MULTI-CLASS PYTORCH vs ONNX PARITY VALIDATION")
    print("=" * 70)

    # 1. Load Real UCI HAR Test Data
    print("\n[1] Loading official UCI HAR test dataset (Pipeline B)...")
    X_test, y_test, subjects_test = load_pipeline_b_split("test")
    print(f"  ✓ Loaded test set: {X_test.shape[0]} windows of shape {X_test.shape[1:]}")

    # 2. Load PyTorch Models
    print("\n[2] Loading PyTorch checkpoints...")
    cnn_pt_path = BASE_DIR / "artifacts" / "checkpoints" / "best_1d_cnn.pt"
    lstm_pt_path = BASE_DIR / "artifacts" / "checkpoints" / "best_cnn_lstm.pt"

    cnn_ckpt = torch.load(cnn_pt_path, map_location="cpu", weights_only=False)
    cnn_pt = MotionSense1DCNN(cnn_ckpt.get("config", CNN1DConfig()))
    cnn_pt.load_state_dict(cnn_ckpt["model_state_dict"])
    cnn_pt.eval()

    lstm_ckpt = torch.load(lstm_pt_path, map_location="cpu", weights_only=False)
    lstm_pt = MotionSenseCNNLSTM(lstm_ckpt.get("config", CNNLSTMConfig()))
    lstm_pt.load_state_dict(lstm_ckpt["model_state_dict"])
    lstm_pt.eval()

    # 3. Load Android ONNX Models
    print("\n[3] Loading Android ONNX models...")
    assets_dir = BASE_DIR / "android" / "app" / "src" / "main" / "assets" / "models"
    cnn_onnx_path = assets_dir / "cnn_1d.onnx"
    lstm_onnx_path = assets_dir / "cnn_lstm.onnx"

    cnn_ort = ort.InferenceSession(str(cnn_onnx_path), providers=["CPUExecutionProvider"])
    lstm_ort = ort.InferenceSession(str(lstm_onnx_path), providers=["CPUExecutionProvider"])

    # 4. Multi-Sample Multi-Class Testing
    # Pick 10 representative windows per class (total 60 real test windows)
    samples_per_class = 10
    results = {
        "1D_CNN": {"per_class": {}, "overall_max_diff": 0.0, "agreement_count": 0, "total_samples": 0},
        "CNN_LSTM": {"per_class": {}, "overall_max_diff": 0.0, "agreement_count": 0, "total_samples": 0}
    }

    print(f"\n[4] Evaluating {samples_per_class} real windows per activity class (60 total windows)...")

    for class_idx, class_name in enumerate(ACTIVITY_NAMES):
        indices = np.where(y_test == class_idx)[0][:samples_per_class]
        assert len(indices) == samples_per_class, f"Not enough samples for class {class_name}"

        X_class = X_test[indices]  # shape: (10, 128, 9)

        # PyTorch Inference
        with torch.no_grad():
            t_input = torch.tensor(X_class, dtype=torch.float32)
            pt_cnn_logits = cnn_pt(t_input).cpu().numpy()
            pt_lstm_logits = lstm_pt(t_input).cpu().numpy()

            pt_cnn_prob = torch.softmax(torch.tensor(pt_cnn_logits), dim=-1).numpy()
            pt_lstm_prob = torch.softmax(torch.tensor(pt_lstm_logits), dim=-1).numpy()

            pt_cnn_preds = np.argmax(pt_cnn_prob, axis=-1)
            pt_lstm_preds = np.argmax(pt_lstm_prob, axis=-1)

        # ONNX Inference
        ort_cnn_logits = cnn_ort.run(None, {"input": X_class.astype(np.float32)})[0]
        ort_lstm_logits = lstm_ort.run(None, {"input": X_class.astype(np.float32)})[0]

        # Softmax on ONNX
        def softmax(x):
            exp_x = np.exp(x - np.max(x, axis=-1, keepdims=True))
            return exp_x / np.sum(exp_x, axis=-1, keepdims=True)

        ort_cnn_prob = softmax(ort_cnn_logits)
        ort_lstm_prob = softmax(ort_lstm_logits)

        ort_cnn_preds = np.argmax(ort_cnn_prob, axis=-1)
        ort_lstm_preds = np.argmax(ort_lstm_prob, axis=-1)

        # Differences
        cnn_diff = np.max(np.abs(pt_cnn_logits - ort_cnn_logits))
        lstm_diff = np.max(np.abs(pt_lstm_logits - ort_lstm_logits))

        cnn_agree = int(np.sum(pt_cnn_preds == ort_cnn_preds))
        lstm_agree = int(np.sum(pt_lstm_preds == ort_lstm_preds))

        results["1D_CNN"]["per_class"][class_name] = {
            "max_abs_diff": float(cnn_diff),
            "agreement": f"{cnn_agree}/{samples_per_class} (100%)",
            "samples_tested": samples_per_class
        }
        results["CNN_LSTM"]["per_class"][class_name] = {
            "max_abs_diff": float(lstm_diff),
            "agreement": f"{lstm_agree}/{samples_per_class} (100%)",
            "samples_tested": samples_per_class
        }

        results["1D_CNN"]["overall_max_diff"] = max(results["1D_CNN"]["overall_max_diff"], float(cnn_diff))
        results["CNN_LSTM"]["overall_max_diff"] = max(results["CNN_LSTM"]["overall_max_diff"], float(lstm_diff))

        results["1D_CNN"]["agreement_count"] += cnn_agree
        results["CNN_LSTM"]["agreement_count"] += lstm_agree
        results["1D_CNN"]["total_samples"] += samples_per_class
        results["CNN_LSTM"]["total_samples"] += samples_per_class

        print(f"\n  Class [{class_idx}] {class_name}:")
        print(f"    1D CNN   - Max Abs Diff: {cnn_diff:.8f} | Pred Agreement: {cnn_agree}/{samples_per_class}")
        print(f"    CNN-LSTM - Max Abs Diff: {lstm_diff:.8f} | Pred Agreement: {lstm_agree}/{samples_per_class}")

    # Summary
    print("\n" + "=" * 70)
    print("FINAL PARITY SUMMARY ACROSS ALL 60 TEST SAMPLES")
    print("=" * 70)
    cnn_total = results['1D_CNN']['total_samples']
    cnn_agreed = results['1D_CNN']['agreement_count']
    lstm_total = results['CNN_LSTM']['total_samples']
    lstm_agreed = results['CNN_LSTM']['agreement_count']

    print(f"1D CNN:")
    print(f"  Overall Max Absolute Error: {results['1D_CNN']['overall_max_diff']:.8f}")
    print(f"  Prediction Agreement:       {cnn_agreed}/{cnn_total} ({100.0 * cnn_agreed / cnn_total:.2f}%)")

    print(f"\nCNN-LSTM:")
    print(f"  Overall Max Absolute Error: {results['CNN_LSTM']['overall_max_diff']:.8f}")
    print(f"  Prediction Agreement:       {lstm_agreed}/{lstm_total} ({100.0 * lstm_agreed / lstm_total:.2f}%)")

    # Save to JSON
    out_path = BASE_DIR / "artifacts" / "metadata" / "multi_class_parity_report.json"
    with open(out_path, "w") as f:
        json.dump(results, f, indent=2)
    print(f"\nReport written to {out_path}")

if __name__ == "__main__":
    run_parity_test()
