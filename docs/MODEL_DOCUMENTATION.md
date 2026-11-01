# Model Architectures & Deep Learning Specifications

## 1. Model 1: Random Forest Classifier Baseline
- **Pipeline**: Pipeline A (561 statistical features).
- **Library**: `scikit-learn` (`RandomForestClassifier`).
- **Hyperparameters**:
  - `n_estimators`: 150
  - `max_depth`: 25
  - `min_samples_split`: 4
  - `min_samples_leaf`: 2
  - `criterion`: Gini Impurity
  - `random_state`: 42
- **Checkpoint**: `artifacts/checkpoints/random_forest.joblib` (5.7 MB)
- **Top Feature Drivers**: Gravity acceleration mean vector along $y$ and $x$ axes, body acceleration jerk magnitude energy.

---

## 2. Model 2: 1D Convolutional Neural Network
- **Pipeline**: Pipeline B (9 raw inertial channels).
- **Framework**: PyTorch.
- **Input Dimension**: `(batch_size, 128, 9)` transposed internally to `(batch_size, 9, 128)`.
- **Layer Architecture**:
  1. `Conv1d(9 -> 64, kernel=5, padding=2)` &rarr; `BatchNorm1d(64)` &rarr; `ReLU()` &rarr; `MaxPool1d(2)` &rarr; `Dropout(0.2)`
  2. `Conv1d(64 -> 128, kernel=5, padding=2)` &rarr; `BatchNorm1d(128)` &rarr; `ReLU()` &rarr; `MaxPool1d(2)` &rarr; `Dropout(0.25)`
  3. `Conv1d(128 -> 128, kernel=3, padding=1)` &rarr; `BatchNorm1d(128)` &rarr; `ReLU()` &rarr; `AdaptiveAvgPool1d(1)` &rarr; `Dropout(0.3)`
  4. `Linear(128 -> 64)` &rarr; `ReLU()` &rarr; `Dropout(0.3)` &rarr; `Linear(64 -> 6)`
- **Total Parameters**: 111,238 trainable weights.
- **Checkpoint**: `artifacts/checkpoints/best_1d_cnn.pt` (1.3 MB).

---

## 3. Model 3: CNN-LSTM Spatio-Temporal Hybrid (Primary Research)
- **Pipeline**: Pipeline B (9 raw inertial channels).
- **Framework**: PyTorch.
- **Theoretical Justification**: Convolutions act as trainable bandpass filters capturing spatial relationships between orthogonal axes ($x, y, z$), while stacked LSTM layers model long-range gait transitions.
- **Layer Architecture**:
  1. `Conv1d(9 -> 64, kernel=5, padding=2)` &rarr; `BatchNorm1d(64)` &rarr; `ReLU()` &rarr; `Dropout(0.2)`
  2. `Conv1d(64 -> 64, kernel=5, padding=2)` &rarr; `BatchNorm1d(64)` &rarr; `ReLU()` &rarr; `MaxPool1d(2)` &rarr; `Dropout(0.25)`
     - Output temporal resolution: 64 steps with 64 feature channels.
  3. Transpose to `(batch_size, seq_len=64, input_size=64)`.
  4. 2-Layer Stacked LSTM (`hidden_dim=128`, `dropout=0.35`, `batch_first=True`).
  5. Dense Projection: `Linear(128 -> 64)` &rarr; `ReLU()` &rarr; `Dropout(0.35)` &rarr; `Linear(64 -> 6)`.
- **Total Parameters**: 263,814 trainable weights.
- **Checkpoint**: `artifacts/checkpoints/best_cnn_lstm.pt` (3.0 MB).
