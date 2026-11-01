import pytest
from fastapi.testclient import TestClient
from backend.app.main import app
from backend.app.database.session import engine, Base

# Ensure SQLite tables exist for tests
Base.metadata.create_all(bind=engine)

client = TestClient(app)

def test_health_endpoint():
    response = client.get("/api/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["service"] == "MotionSense"

def test_dataset_info_endpoint():
    response = client.get("/api/dataset/info")
    assert response.status_code == 200
    data = response.json()
    assert data["total_windows"] == 10299
    assert data["features_count"] == 561
    assert data["raw_channels_count"] == 9

def test_models_endpoint():
    response = client.get("/api/models")
    assert response.status_code == 200
    data = response.json()
    assert len(data) >= 3
    model_ids = [m["model_id"] for m in data]
    assert "random_forest" in model_ids
    assert "cnn_1d" in model_ids
    assert "cnn_lstm" in model_ids

def test_history_endpoint():
    response = client.get("/api/history")
    assert response.status_code == 200
    data = response.json()
    assert "items" in data
    assert "total" in data
