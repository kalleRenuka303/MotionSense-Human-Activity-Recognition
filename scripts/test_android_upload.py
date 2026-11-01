import io
import sys
from pathlib import Path
from fastapi.testclient import TestClient

# Ensure root directory is on PYTHONPATH
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from backend.app.main import app

def run_tests():
    client = TestClient(app)
    ANDROID_USER_AGENT = (
        "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/124.0.6367.179 Mobile Safari/537.36"
    )
    headers = {"User-Agent": ANDROID_USER_AGENT}

    print("=" * 60)
    print("MOTIONSENSE — ANDROID CSV UPLOAD & VALIDATION TEST SUITE")
    print("=" * 60)

    # 1. Test HTML / Component File Inspection
    page_path = Path("frontend/src/pages/ActivityRecognitionPage.tsx")
    assert page_path.exists(), "ActivityRecognitionPage.tsx not found"
    content = page_path.read_text(encoding="utf-8")

    print("\n[TEST 1] Verifying HTML Input and Label Attributes in Frontend Source:")
    assert 'type="file"' in content, "FAIL: type='file' missing"
    print("  ✓ input[type='file'] present")

    assert 'accept=".csv,text/csv"' in content, "FAIL: accept='.csv,text/csv' missing"
    print("  ✓ input[accept='.csv,text/csv'] present (Supports Android DocumentsUI)")

    assert 'htmlFor="sensor-csv-upload-input"' in content, "FAIL: label htmlFor missing"
    assert 'id="sensor-csv-upload-input"' in content, "FAIL: input id missing"
    print("  ✓ Label htmlFor and Input ID properly associated for native mobile touch interaction")

    assert 'className="sr-only"' in content, "FAIL: sr-only missing"
    print("  ✓ Accessible sr-only styling used instead of display:none (avoids Android gesture blocking)")

    assert "onClick={(e) => {" in content and ".value = '';" in content, "FAIL: input value reset missing"
    print("  ✓ Input value reset on tap verified (enables re-selecting the same file)")

    # 2. Test Android Empty File (0 Bytes)
    print("\n[TEST 2] Testing Android Empty File (0 Bytes) Upload:")
    empty_file = io.BytesIO(b"")
    res = client.post(
        "/api/data/validate",
        files={"file": ("empty_recording.csv", empty_file, "text/csv")},
        headers=headers
    )
    assert res.status_code == 200, f"Unexpected status {res.status_code}"
    data = res.json()
    assert data["valid"] is False, "Empty file should not be valid"
    assert any("empty" in e.lower() for e in data["errors"]), f"Expected empty file error, got {data['errors']}"
    print(f"  ✓ Handled correctly: valid=False, error='{data['errors'][0]}'")

    # 3. Test Android File with Empty MIME Type (Generic Android File Provider)
    print("\n[TEST 3] Testing Android File with Generic Empty MIME Type ('') and Pipeline B Raw Sensors:")
    raw_sample_path = Path("artifacts/samples/sample_raw.csv")
    assert raw_sample_path.exists(), "Sample raw CSV missing"
    with open(raw_sample_path, "rb") as f:
        raw_bytes = f.read()

    # Upload with empty string MIME type, typical of some Android file pickers
    res = client.post(
        "/api/data/validate",
        files={"file": ("android_download_inertial.csv", io.BytesIO(raw_bytes), "")},
        headers=headers
    )
    assert res.status_code == 200
    data = res.json()
    assert data["valid"] is True, f"Raw CSV should be valid even with empty MIME type. Errors: {data.get('errors')}"
    assert data["input_format"] == "raw_inertial"
    assert data["detected_channels_or_features"] == 9
    print(f"  ✓ Validated successfully with empty MIME type: format={data['input_format']}, channels={data['detected_channels_or_features']}")

    # 4. Test Android File with Pipeline A (561 Pre-computed Features)
    print("\n[TEST 4] Testing Android File with Pipeline A 561 Features and Octet-Stream MIME:")
    feat_sample_path = Path("artifacts/samples/sample_features.csv")
    assert feat_sample_path.exists(), "Sample features CSV missing"
    with open(feat_sample_path, "rb") as f:
        feat_bytes = f.read()

    res = client.post(
        "/api/data/validate",
        files={"file": ("motionsense_sample_features.csv", io.BytesIO(feat_bytes), "application/octet-stream")},
        headers=headers
    )
    assert res.status_code == 200
    data = res.json()
    assert data["valid"] is True, f"Features CSV should be valid. Errors: {data.get('errors')}"
    assert data["input_format"] == "features"
    assert data["detected_channels_or_features"] == 561
    print(f"  ✓ Validated successfully: format={data['input_format']}, features={data['detected_channels_or_features']}")

    # 5. Test Non-CSV Extension Rejection (.txt / .json)
    print("\n[TEST 5] Testing Non-CSV File Rejection:")
    res = client.post(
        "/api/data/validate",
        files={"file": ("sensor_data.txt", io.BytesIO(b"some,text,data\n1,2,3"), "text/plain")},
        headers=headers
    )
    assert res.status_code == 200
    data = res.json()
    assert data["valid"] is False
    assert any(".csv extension" in e for e in data["errors"])
    print(f"  ✓ Rejected non-CSV properly: error='{data['errors'][0]}'")

    # 6. Test Invalid CSV Schema (Mismatched Columns)
    print("\n[TEST 6] Testing Invalid CSV Schema (Unsupported Column Headers):")
    invalid_csv = b"col1,col2,col3\n1.0,2.0,3.0\n4.0,5.0,6.0\n"
    res = client.post(
        "/api/data/validate",
        files={"file": ("invalid_schema.csv", io.BytesIO(invalid_csv), "text/csv")},
        headers=headers
    )
    assert res.status_code == 200
    data = res.json()
    assert data["valid"] is False
    assert any("does not match" in e.lower() or "unsupported" in e.lower() for e in data["errors"])
    print(f"  ✓ Rejected invalid schema properly: error='{data['errors'][0]}'")

    # 7. Test Inference Execution with Android Uploaded File
    print("\n[TEST 7] Testing End-to-End Prediction with Android Upload Payload:")
    res_pred = client.post(
        "/api/predict/raw",
        files={"file": ("download_sample_raw.csv", io.BytesIO(raw_bytes), "text/csv")},
        data={"model_name": "cnn_lstm"},
        headers=headers
    )
    assert res_pred.status_code == 200
    pred_data = res_pred.json()
    assert pred_data["model_name"] == "CNN-LSTM"
    assert pred_data["summary"]["total_windows"] == 5
    assert pred_data["summary"]["average_confidence"] > 0.90
    print(f"  ✓ Prediction completed successfully: model={pred_data['model_name']}, windows={pred_data['summary']['total_windows']}, avg_conf={pred_data['summary']['average_confidence']:.2%}")

    print("\n" + "=" * 60)
    print("ALL 7 ANDROID UPLOAD VERIFICATION TESTS PASSED SUCCESSFULLY!")
    print("=" * 60)

if __name__ == "__main__":
    run_tests()
