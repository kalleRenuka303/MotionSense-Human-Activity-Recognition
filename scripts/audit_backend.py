#!/usr/bin/env python3
"""
Comprehensive FastAPI Endpoint & Error Handling Audit
Tests all REST routes, valid CSV uploads, malformed CSV uploads, edge cases, and DB persistence.
"""

import io
import json
import pandas as pd
import numpy as np
from fastapi.testclient import TestClient

from backend.app.main import app
from backend.app.database.session import engine, Base
from ml.configs.config import SENSOR_CHANNELS

Base.metadata.create_all(bind=engine)
client = TestClient(app)

def audit_backend():
    print("\n=======================================================")
    print("  FASTAPI BACKEND COMPREHENSIVE ENDPOINT AUDIT")
    print("=======================================================")

    results = []

    def check(name, condition, details=""):
        status = "PASS" if condition else "FAIL"
        results.append({"name": name, "status": status, "details": details})
        print(f"[{status}] {name} {f'- {details}' if details else ''}")

    # 1. Health
    r = client.get("/api/health")
    check("Health Check (GET /api/health)", r.status_code == 200 and r.json()["status"] == "healthy", f"Status: {r.status_code}")

    # 2. Dataset Info
    r = client.get("/api/dataset/info")
    data = r.json()
    check("Dataset Info (GET /api/dataset/info)", r.status_code == 200 and data["total_windows"] == 10299, f"Total windows: {data.get('total_windows')}")

    # 3. Sample CSVs
    r_feat = client.get("/api/dataset/sample/features")
    check("Sample Features CSV (GET /api/dataset/sample/features)", r_feat.status_code == 200 and "text/csv" in r_feat.headers.get("content-type", ""))
    
    r_raw = client.get("/api/dataset/sample/raw")
    check("Sample Raw CSV (GET /api/dataset/sample/raw)", r_raw.status_code == 200 and "text/csv" in r_raw.headers.get("content-type", ""))

    # 4. Models Catalog
    r = client.get("/api/models")
    m_list = r.json()
    check("Models Catalog (GET /api/models)", r.status_code == 200 and len(m_list) == 3, f"Returned {len(m_list)} models")

    # 5. Model Detail & 404
    r = client.get("/api/models/cnn_lstm/info")
    check("Model Detail (GET /api/models/cnn_lstm/info)", r.status_code == 200 and r.json()["model_id"] == "cnn_lstm")

    r_404 = client.get("/api/models/nonexistent_model/info")
    check("Model 404 Handler", r_404.status_code == 404, f"Status: {r_404.status_code}")

    # 6. CSV Validation - Valid 9-channel raw
    valid_raw_df = pd.DataFrame(np.random.randn(256, 9), columns=SENSOR_CHANNELS)
    buf = io.BytesIO()
    valid_raw_df.to_csv(buf, index=False)
    buf.seek(0)
    r = client.post("/api/data/validate", files={"file": ("test_raw.csv", buf.getvalue(), "text/csv")})
    check("Validation of Valid 9-Channel CSV", r.status_code == 200 and r.json()["valid"] is True and r.json()["input_format"] == "raw_inertial")

    # 7. CSV Validation - Invalid Extension
    r = client.post("/api/data/validate", files={"file": ("test.txt", b"abc", "text/plain")})
    check("Rejection of non-CSV files (.txt)", r.status_code == 200 and r.json()["valid"] is False, f"Errors: {r.json()['errors']}")

    # 8. CSV Validation - Missing channels
    bad_df = pd.DataFrame({"total_acc_x": [1, 2, 3], "body_gyro_z": [0.1, 0.2, 0.3]})
    buf = io.BytesIO()
    bad_df.to_csv(buf, index=False)
    r = client.post("/api/data/validate", files={"file": ("bad_channels.csv", buf.getvalue(), "text/csv")})
    check("Rejection of Incomplete Sensor Channels", r.status_code == 200 and r.json()["valid"] is False)

    # 9. CSV Validation - Insufficient rows (< 128)
    short_df = pd.DataFrame(np.random.randn(50, 9), columns=SENSOR_CHANNELS)
    buf = io.BytesIO()
    short_df.to_csv(buf, index=False)
    r = client.post("/api/data/validate", files={"file": ("short.csv", buf.getvalue(), "text/csv")})
    check("Rejection of Insufficient Row Count (< 128)", r.status_code == 200 and r.json()["valid"] is False)

    # 10. Raw Prediction Execution (CNN-LSTM)
    buf = io.BytesIO()
    valid_raw_df.to_csv(buf, index=False)
    r = client.post("/api/predict/raw", data={"model_name": "cnn_lstm"}, files={"file": ("recording_sample.csv", buf.getvalue(), "text/csv")})
    pred_data = r.json()
    check("Prediction on Raw Inertial Signals (POST /api/predict/raw)", r.status_code == 200 and pred_data["summary"]["total_windows"] > 0, f"Predicted windows: {pred_data.get('summary', {}).get('total_windows')}")

    # 11. Raw Prediction Execution (1D CNN)
    r_cnn = client.post("/api/predict/raw", data={"model_name": "1d_cnn"}, files={"file": ("recording_sample_cnn.csv", buf.getvalue(), "text/csv")})
    check("Prediction on Raw Signals via 1D CNN", r_cnn.status_code == 200 and r_cnn.json()["model_name"] == "1D CNN")

    # 12. Feature Prediction Execution (Random Forest)
    buf_feat = io.BytesIO(r_feat.content)
    r_rf = client.post("/api/predict/features", files={"file": ("test_features.csv", buf_feat.getvalue(), "text/csv")})
    check("Prediction on 561 Features (POST /api/predict/features)", r_rf.status_code == 200 and r_rf.json()["model_name"] == "Random Forest")

    # 13. Prediction History Audit
    r_hist = client.get("/api/history")
    hist_items = r_hist.json().get("items", [])
    check("History Logging in SQLite", r_hist.status_code == 200 and len(hist_items) >= 2, f"Found {len(hist_items)} persisted runs")

    # 14. History Detail & Deletion
    if hist_items:
        first_id = hist_items[0]["id"]
        r_det = client.get(f"/api/history/{first_id}")
        check("History Detail (GET /api/history/{id})", r_det.status_code == 200 and r_det.json()["id"] == first_id)

    # 15. Evaluation Summary & Plots
    r_eval = client.get("/api/evaluation/summary")
    check("Evaluation Summary (GET /api/evaluation/summary)", r_eval.status_code == 200 and "models" in r_eval.json())

    r_plot = client.get("/api/evaluation/plots/model_comparison.png")
    check("Plot Image Serving (GET /api/evaluation/plots/model_comparison.png)", r_plot.status_code == 200 and r_plot.headers.get("content-type") == "image/png")

    r_bad_plot = client.get("/api/evaluation/plots/invalid.png")
    check("Rejection of Invalid Plot Request", r_bad_plot.status_code == 400)

    print("\n=======================================================")
    all_passed = all(x["status"] == "PASS" for x in results)
    print(f"  BACKEND AUDIT RESULT: {'ALL PASS' if all_passed else 'FAILURES DETECTED'}")
    print(f"  Passed: {sum(1 for x in results if x['status'] == 'PASS')} / {len(results)}")
    print("=======================================================\n")
    return results

if __name__ == "__main__":
    audit_backend()
