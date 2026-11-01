# FastAPI Backend REST API Documentation

## Base Information
- **Base URL**: `http://localhost:8000/api`
- **Swagger Documentation**: `http://localhost:8000/docs`
- **Redoc Documentation**: `http://localhost:8000/redoc`

---

## Endpoints

### 1. System Health
- **`GET /api/health`**
- **Description**: Verifies service readiness, dataset file availability, and artifact status.
- **Response**:
```json
{
  "status": "healthy",
  "service": "MotionSense",
  "dataset_ready": true,
  "checkpoints_dir_exists": true
}
```

### 2. Dataset Metadata
- **`GET /api/dataset/info`**: Returns total windows, sampling rate, subject counts, and audit status.
- **`GET /api/dataset/sample/features`**: Downloads sample 561-feature CSV (`motionsense_sample_features.csv`).
- **`GET /api/dataset/sample/raw`**: Downloads sample 9-axis inertial CSV (`motionsense_sample_raw_sensors.csv`).

### 3. Model Catalog
- **`GET /api/models`**: Returns all 3 trained models with input shapes, parameters, test accuracy, and latency.
- **`GET /api/models/{model_id}/info`**: Returns detailed parameters for a single model (`random_forest`, `cnn_1d`, `cnn_lstm`).

### 4. Input Validation & Inference
- **`POST /api/data/validate`**:
  - Multipart form upload `file: UploadFile`.
  - Analyzes schema, detecting Pipeline A (561 features) or Pipeline B (9 raw channels).
  - Checks for non-numeric/NaN values, column names, and row count.
- **`POST /api/predict/features`**:
  - Multipart upload `file: UploadFile`.
  - Runs Random Forest inference. Returns probabilities, uncertainty flags, timeline, and persists record.
- **`POST /api/predict/raw`**:
  - Multipart upload `file: UploadFile` and form field `model_name: "cnn_lstm" | "cnn_1d"`.
  - Automatically slices continuous signal into 128-sample windows with 50% overlap.
  - Returns classification summary and timeline.

### 5. Academic Evaluation Metrics
- **`GET /api/evaluation/summary`**: Returns test accuracy, macro F1, weighted F1, and per-class reports.
- **`GET /api/evaluation/feature-importance`**: Returns top 25 feature importances from Random Forest.
- **`GET /api/evaluation/plots/{plot_name}`**: Serves generated plots (`model_comparison.png`, `cm_random_forest.png`, etc.).

### 6. Prediction History
- **`GET /api/history?limit=50&offset=0`**: Fetches persisted prediction runs from SQLite.
- **`GET /api/history/{id}`**: Detailed timeline and activity distribution for a run.
- **`DELETE /api/history/{id}`**: Removes a record from the database.
