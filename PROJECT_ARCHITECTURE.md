# MotionSense — System & Research Architecture

## 1. High-Level System Architecture

MotionSense is engineered as a decoupled, multi-tier platform separating data ingestion, machine learning research pipelines, production inference, and interactive visualization.

```mermaid
flowchart TD
    subgraph Data Layer
        UCI["Official UCI HAR Dataset 240"] --> PA["Pipeline A: 561 Statistical Features"]
        UCI --> PB["Pipeline B: 9-Axis Inertial Signals (128 samples @ 50Hz)"]
    end

    subgraph ML Research & Training
        PA --> RF["Random Forest Classifier (Gini, 150 trees)"]
        PB --> CNN["1D Deep CNN (Feature Extraction + Pooling)"]
        PB --> CNNLSTM["CNN-LSTM (Spatial Filters + Recurrent Dynamics)"]
    end

    subgraph Model Artifacts
        RF --> ART_RF["artifacts/checkpoints/random_forest.joblib"]
        CNN --> ART_CNN["artifacts/checkpoints/best_1d_cnn.pt"]
        CNNLSTM --> ART_CL["artifacts/checkpoints/best_cnn_lstm.pt"]
        METRICS["artifacts/metrics/*.json & plots/*.png"]
    end

    subgraph Backend - FastAPI
        ART_RF --> INF["Inference Engine & Validator"]
        ART_CNN --> INF
        ART_CL --> INF
        METRICS --> API_EVAL["Evaluation Endpoints"]
        INF --> API_PRED["Prediction Endpoints (/api/predict/*)"]
        API_PRED --> DB[("SQLite Database: Prediction History")]
    end

    subgraph Frontend - React + Vite + Tailwind
        API_EVAL --> UI_EVAL["Evaluation & Comparison Dashboard"]
        API_PRED --> UI_REC["Interactive Activity Recognition & Timeline"]
        DB --> UI_HIST["Prediction History & Export"]
    end
```

---

## 2. Input Pipeline Specifications

### Pipeline A — Feature-Based Pipeline (Random Forest)
- **Source**: 561 pre-computed time and frequency domain variables (`X_train.txt`, `X_test.txt`).
- **Feature Categories**: Mean, standard deviation, median absolute deviation, max/min, signal magnitude area, energy, interquartile range, autoregression coefficients, correlation coefficients, skewness, kurtosis, and spectral energy bands.
- **Dimensionality**: `(N, 561)` float32 vector.
- **Normalization**: Pre-normalized in $[-1, 1]$ in original UCI HAR dataset.

### Pipeline B — Raw Inertial Signals Pipeline (1D CNN & CNN-LSTM)
- **Source**: 9 raw tri-axial inertial sensor files per window located in `Inertial Signals/`.
- **Channels (Exact Order)**:
  1. `total_acc_x`: Total acceleration from accelerometer ($x$-axis, units: $g$).
  2. `total_acc_y`: Total acceleration from accelerometer ($y$-axis, units: $g$).
  3. `total_acc_z`: Total acceleration from accelerometer ($z$-axis, units: $g$).
  4. `body_acc_x`: Estimated body acceleration component ($x$-axis).
  5. `body_acc_y`: Estimated body acceleration component ($y$-axis).
  6. `body_acc_z`: Estimated body acceleration component ($z$-axis).
  7. `body_gyro_x`: Angular velocity from gyroscope ($x$-axis, units: $\text{rad/s}$).
  8. `body_gyro_y`: Angular velocity from gyroscope ($y$-axis, units: $\text{rad/s}$).
  9. `body_gyro_z`: Angular velocity from gyroscope ($z$-axis, units: $\text{rad/s}$).
- **Window Length**: 128 readings per channel ($\approx 2.56$ seconds at 50 Hz, 50% overlap of 64 readings).
- **Tensor Shape**: `(batch_size, 128, 9)` or `(batch_size, 9, 128)` transposed for PyTorch 1D convolutions.

---

## 3. Strict Subject Independence & Data Leakage Prevention

- **Subject Pool**: 30 volunteers aged 19–48.
- **Official Split**: 70% training (21 subjects), 30% testing (9 subjects).
- **Validation Partitioning**: Validation set is formed by grouping subjects exclusively from the training pool (e.g. 16 train subjects, 5 validation subjects). No subject ever appears in both train and validation splits, completely eliminating window overlap contamination.

---

## 4. Class Nomenclature & Integer Mapping

1. `WALKING`: Class 0 (0-indexed for PyTorch), Label 1 in UCI original.
2. `WALKING_UPSTAIRS`: Class 1 (Label 2).
3. `WALKING_DOWNSTAIRS`: Class 2 (Label 3).
4. `SITTING`: Class 3 (Label 4).
5. `STANDING`: Class 4 (Label 5).
6. `LAYING`: Class 5 (Label 6).
