import os
import sys
import json
import torch
import numpy as np
from pathlib import Path

# Ensure root is on PYTHONPATH
BASE_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(BASE_DIR))

from ml.models.cnn_1d_model import MotionSense1DCNN
from ml.models.cnn_lstm_model import MotionSenseCNNLSTM
from ml.configs.config import CNN1DConfig, CNNLSTMConfig

def export_and_verify():
    print("=" * 60)
    print("MOTIONSENSE — MOBILE MODEL EXPORT & NUMERICAL PARITY AUDIT")
    print("=" * 60)

    assets_dir = BASE_DIR / "android" / "app" / "src" / "main" / "assets" / "models"
    assets_dir.mkdir(parents=True, exist_ok=True)

    # 1. Export 1D CNN
    print("\n[1] Exporting 1D CNN model to ONNX...")
    cnn_ckpt_path = BASE_DIR / "artifacts" / "checkpoints" / "best_1d_cnn.pt"
    assert cnn_ckpt_path.exists(), f"1D CNN checkpoint not found at {cnn_ckpt_path}"

    cnn_ckpt = torch.load(cnn_ckpt_path, map_location="cpu", weights_only=False)
    cnn_model = MotionSense1DCNN(cnn_ckpt.get("config", CNN1DConfig()))
    cnn_model.load_state_dict(cnn_ckpt["model_state_dict"])
    cnn_model.eval()

    # Input tensor shape: (batch_size=1, timesteps=128, channels=9)
    dummy_input = torch.randn(1, 128, 9, dtype=torch.float32)
    cnn_onnx_path = assets_dir / "cnn_1d.onnx"

    torch.onnx.export(
        cnn_model,
        dummy_input,
        str(cnn_onnx_path),
        export_params=True,
        opset_version=14,
        do_constant_folding=True,
        input_names=["input"],
        output_names=["output"],
        dynamic_axes={"input": {0: "batch_size"}, "output": {0: "batch_size"}}
    )
    print(f"  ✓ 1D CNN saved to {cnn_onnx_path} ({cnn_onnx_path.stat().st_size / 1024:.1f} KB)")

    # 2. Export CNN-LSTM
    print("\n[2] Exporting CNN-LSTM model to ONNX...")
    lstm_ckpt_path = BASE_DIR / "artifacts" / "checkpoints" / "best_cnn_lstm.pt"
    assert lstm_ckpt_path.exists(), f"CNN-LSTM checkpoint not found at {lstm_ckpt_path}"

    lstm_ckpt = torch.load(lstm_ckpt_path, map_location="cpu", weights_only=False)
    lstm_model = MotionSenseCNNLSTM(lstm_ckpt.get("config", CNNLSTMConfig()))
    lstm_model.load_state_dict(lstm_ckpt["model_state_dict"])
    lstm_model.eval()

    lstm_onnx_path = assets_dir / "cnn_lstm.onnx"
    torch.onnx.export(
        lstm_model,
        dummy_input,
        str(lstm_onnx_path),
        export_params=True,
        opset_version=14,
        do_constant_folding=True,
        input_names=["input"],
        output_names=["output"],
        dynamic_axes={"input": {0: "batch_size"}, "output": {0: "batch_size"}}
    )
    print(f"  ✓ CNN-LSTM saved to {lstm_onnx_path} ({lstm_onnx_path.stat().st_size / 1024:.1f} KB)")

    # 3. Verify Parity with ONNX Runtime
    import onnxruntime as ort

    print("\n[3] Verifying Output Parity (PyTorch vs ONNX Runtime):")
    # Test on multiple realistic samples
    np.random.seed(42)
    test_inputs = np.random.randn(5, 128, 9).astype(np.float32)

    # Test 1D CNN
    ort_session_cnn = ort.InferenceSession(str(cnn_onnx_path))
    with torch.no_grad():
        pt_cnn_out = cnn_model(torch.from_numpy(test_inputs)).numpy()
    ort_cnn_out = ort_session_cnn.run(None, {"input": test_inputs})[0]
    cnn_max_diff = float(np.max(np.abs(pt_cnn_out - ort_cnn_out)))
    print(f"  1D CNN Max Absolute Difference: {cnn_max_diff:.8f}")
    assert cnn_max_diff < 1e-4, f"1D CNN parity check failed (diff: {cnn_max_diff})"
    print("  ✓ 1D CNN numerical parity verified (< 1e-4 tolerance)")

    # Test CNN-LSTM
    ort_session_lstm = ort.InferenceSession(str(lstm_onnx_path))
    with torch.no_grad():
        pt_lstm_out = lstm_model(torch.from_numpy(test_inputs)).numpy()
    ort_lstm_out = ort_session_lstm.run(None, {"input": test_inputs})[0]
    lstm_max_diff = float(np.max(np.abs(pt_lstm_out - ort_lstm_out)))
    print(f"  CNN-LSTM Max Absolute Difference: {lstm_max_diff:.8f}")
    assert lstm_max_diff < 1e-4, f"CNN-LSTM parity check failed (diff: {lstm_max_diff})"
    print("  ✓ CNN-LSTM numerical parity verified (< 1e-4 tolerance)")

    # 4. Save metadata report
    report = {
        "export_status": "SUCCESS",
        "opset_version": 14,
        "models": {
            "1d_cnn": {
                "file": "cnn_1d.onnx",
                "size_kb": round(cnn_onnx_path.stat().st_size / 1024, 2),
                "input_shape": [1, 128, 9],
                "output_shape": [1, 6],
                "max_absolute_error_vs_pytorch": cnn_max_diff
            },
            "cnn_lstm": {
                "file": "cnn_lstm.onnx",
                "size_kb": round(lstm_onnx_path.stat().st_size / 1024, 2),
                "input_shape": [1, 128, 9],
                "output_shape": [1, 6],
                "max_absolute_error_vs_pytorch": lstm_max_diff
            }
        }
    }
    report_path = BASE_DIR / "artifacts" / "metadata" / "mobile_model_parity_report.json"
    with open(report_path, "w") as f:
        json.dump(report, f, indent=2)
    print(f"\n[4] Parity report saved to {report_path}")
    print("=" * 60)

if __name__ == "__main__":
    export_and_verify()
