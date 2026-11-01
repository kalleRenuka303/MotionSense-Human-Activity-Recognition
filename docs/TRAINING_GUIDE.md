# Reproducible Model Training Guide

## 1. Prerequisites
- Python 3.9+ with virtual environment activated (`source .venv/bin/activate`).
- Official UCI HAR Dataset 240 extracted to `ml/data/raw/UCI HAR Dataset/`.

---

## 2. Step-by-Step Training Execution

### Step 1: Download & Verify Dataset
```bash
python scripts/download_dataset.py
python scripts/verify_dataset.py
```

### Step 2: Train Random Forest Baseline
```bash
PYTHONPATH=. MPLCONFIGDIR=/tmp python ml/training/train_rf.py
```
- Fits 150 trees with Gini criterion on 5,607 train windows.
- Evaluates on 1,745 validation windows and 2,947 test windows.
- Outputs `artifacts/checkpoints/random_forest.joblib` and `artifacts/metrics/rf_metrics.json`.

### Step 3: Train Deep Learning Models (1D CNN & CNN-LSTM)
```bash
PYTHONPATH=. MPLCONFIGDIR=/tmp python ml/training/train_pytorch.py
```
- Trains 1D CNN with early stopping on validation loss.
- Trains CNN-LSTM with 2 recurrent LSTM layers.
- Checkpoints saved in `artifacts/checkpoints/`.

### Step 4: Generate Publication Plots & Evaluation Report
```bash
PYTHONPATH=. MPLCONFIGDIR=/tmp python ml/evaluation/evaluate_all.py
```
- Generates high-resolution confusion matrix heatmaps and comparison charts in `artifacts/plots/`.
