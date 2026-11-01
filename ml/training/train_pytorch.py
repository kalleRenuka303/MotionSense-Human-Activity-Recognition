"""
Deep Learning Training Pipeline for PyTorch Models (1D CNN & CNN-LSTM)
Features:
- Device auto-selection (MPS on Apple Silicon, CUDA, CPU)
- Subject-independent validation & early stopping
- Learning rate scheduling
- Complete history recording (Loss, Accuracy per epoch)
- Test set evaluation and confusion matrix generation
"""

import os
import time
import json
import torch
import torch.nn as nn
from torch.utils.data import TensorDataset, DataLoader
import numpy as np
from pathlib import Path
from typing import Dict, Any, Tuple
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score, f1_score, precision_score, recall_score

from ml.configs.config import (
    CHECKPOINTS_DIR, 
    METRICS_DIR, 
    ACTIVITY_NAMES, 
    CNN1DConfig, 
    CNNLSTMConfig
)
from ml.preprocessing.pipeline_b import get_train_val_test_raw_data
from ml.models.cnn_1d_model import MotionSense1DCNN
from ml.models.cnn_lstm_model import MotionSenseCNNLSTM

METRICS_DIR.mkdir(parents=True, exist_ok=True)
CHECKPOINTS_DIR.mkdir(parents=True, exist_ok=True)

def get_device() -> torch.device:
    if os.environ.get("DEVICE") == "cuda" and torch.cuda.is_available():
        return torch.device("cuda")
    elif os.environ.get("DEVICE") == "mps" and torch.backends.mps.is_available():
        return torch.device("mps")
    return torch.device("cpu")

def train_epoch(model: nn.Module, loader: DataLoader, criterion: nn.Module, optimizer: torch.optim.Optimizer, device: torch.device) -> Tuple[float, float]:
    model.train()
    running_loss = 0.0
    correct = 0
    total = 0

    for inputs, labels in loader:
        inputs, labels = inputs.to(device), labels.to(device)
        optimizer.zero_grad()
        outputs = model(inputs)
        loss = criterion(outputs, labels)
        loss.backward()
        optimizer.step()

        running_loss += loss.item() * inputs.size(0)
        _, preds = torch.max(outputs, 1)
        correct += (preds == labels).sum().item()
        total += labels.size(0)

    epoch_loss = running_loss / total
    epoch_acc = correct / total
    return epoch_loss, epoch_acc

def evaluate_model(model: nn.Module, loader: DataLoader, criterion: nn.Module, device: torch.device) -> Tuple[float, float, np.ndarray, np.ndarray]:
    model.eval()
    running_loss = 0.0
    all_preds = []
    all_labels = []

    with torch.no_grad():
        for inputs, labels in loader:
            inputs, labels = inputs.to(device), labels.to(device)
            outputs = model(inputs)
            loss = criterion(outputs, labels)

            running_loss += loss.item() * inputs.size(0)
            _, preds = torch.max(outputs, 1)
            all_preds.extend(preds.cpu().numpy())
            all_labels.extend(labels.cpu().numpy())

    total = len(all_labels)
    epoch_loss = running_loss / total if total > 0 else 0.0
    y_true = np.array(all_labels)
    y_pred = np.array(all_preds)
    epoch_acc = accuracy_score(y_true, y_pred)
    return epoch_loss, epoch_acc, y_true, y_pred

def train_deep_model(model_type: str = "cnn_lstm") -> Dict[str, Any]:
    device = get_device()
    print(f"\n=======================================================")
    print(f"  Training Deep Learning Model: {model_type.upper()}")
    print(f"  Target Device: {device}")
    print(f"=======================================================")

    # Load Raw Inertial Data
    data = get_train_val_test_raw_data()
    X_train, y_train = data["X_train"], data["y_train"]
    X_val, y_val = data["X_val"], data["y_val"]
    X_test, y_test = data["X_test"], data["y_test"]

    print(f"[*] Training Windows: {X_train.shape} across subjects: {sorted(list(set(data['sub_train'])))}")
    print(f"[*] Validation Windows: {X_val.shape} across subjects: {sorted(list(set(data['sub_val'])))}")
    print(f"[*] Test Windows: {X_test.shape} across subjects: {sorted(list(set(data['sub_test'])))}")

    # Instantiate Model
    if model_type == "cnn_1d":
        config = CNN1DConfig()
        model = MotionSense1DCNN(config).to(device)
        checkpoint_path = CHECKPOINTS_DIR / "best_1d_cnn.pt"
        metrics_file = METRICS_DIR / "1d_cnn_metrics.json"
    elif model_type == "cnn_lstm":
        config = CNNLSTMConfig()
        model = MotionSenseCNNLSTM(config).to(device)
        checkpoint_path = CHECKPOINTS_DIR / "best_cnn_lstm.pt"
        metrics_file = METRICS_DIR / "cnn_lstm_metrics.json"
    else:
        raise ValueError(f"Unknown model type: {model_type}")

    num_params = model.count_parameters()
    print(f"[+] Model architecture instantiated with {num_params:,} trainable parameters.")

    # DataLoaders
    batch_size = config.batch_size
    train_ds = TensorDataset(torch.from_numpy(X_train), torch.from_numpy(y_train))
    val_ds = TensorDataset(torch.from_numpy(X_val), torch.from_numpy(y_val))
    test_ds = TensorDataset(torch.from_numpy(X_test), torch.from_numpy(y_test))

    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True)
    val_loader = DataLoader(val_ds, batch_size=batch_size, shuffle=False)
    test_loader = DataLoader(test_ds, batch_size=batch_size, shuffle=False)

    criterion = nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(model.parameters(), lr=config.learning_rate, weight_decay=1e-4)
    scheduler = torch.optim.lr_scheduler.ReduceLROnPlateau(optimizer, mode="min", factor=0.5, patience=3)

    history = {
        "train_loss": [],
        "train_acc": [],
        "val_loss": [],
        "val_acc": [],
        "learning_rates": []
    }

    best_val_loss = float("inf")
    patience_counter = 0
    start_time = time.time()

    for epoch in range(1, config.epochs + 1):
        t_loss, t_acc = train_epoch(model, train_loader, criterion, optimizer, device)
        v_loss, v_acc, _, _ = evaluate_model(model, val_loader, criterion, device)
        scheduler.step(v_loss)
        
        current_lr = optimizer.param_groups[0]["lr"]
        history["train_loss"].append(round(t_loss, 4))
        history["train_acc"].append(round(t_acc, 4))
        history["val_loss"].append(round(v_loss, 4))
        history["val_acc"].append(round(v_acc, 4))
        history["learning_rates"].append(current_lr)

        print(f"Epoch [{epoch:02d}/{config.epochs:02d}] - Train Loss: {t_loss:.4f}, Acc: {t_acc*100:.2f}% | Val Loss: {v_loss:.4f}, Acc: {v_acc*100:.2f}% (lr: {current_lr:.6f})")

        # Checkpoint Best Model
        if v_loss < best_val_loss:
            best_val_loss = v_loss
            patience_counter = 0
            torch.save({
                "epoch": epoch,
                "model_state_dict": model.state_dict(),
                "optimizer_state_dict": optimizer.state_dict(),
                "config": config,
                "val_loss": v_loss,
                "val_acc": v_acc
            }, checkpoint_path)
            print(f"  [*] Best checkpoint updated at epoch {epoch}")
        else:
            patience_counter += 1
            if patience_counter >= config.early_stopping_patience:
                print(f"[*] Early stopping triggered after {epoch} epochs.")
                break

    train_duration = time.time() - start_time
    print(f"[+] Training finished in {train_duration:.2f} seconds.")

    # Load best checkpoint for official test evaluation
    checkpoint = torch.load(checkpoint_path, map_location=device, weights_only=False)
    model.load_state_dict(checkpoint["model_state_dict"])
    model.eval()

    test_loss, test_acc, y_true_test, y_pred_test = evaluate_model(model, test_loader, criterion, device)
    macro_f1 = f1_score(y_true_test, y_pred_test, average="macro")
    weighted_f1 = f1_score(y_true_test, y_pred_test, average="weighted")
    macro_prec = precision_score(y_true_test, y_pred_test, average="macro")
    macro_rec = recall_score(y_true_test, y_pred_test, average="macro")
    cm = confusion_matrix(y_true_test, y_pred_test).tolist()
    clf_rep = classification_report(y_true_test, y_pred_test, target_names=ACTIVITY_NAMES, output_dict=True)

    # Benchmark Latency
    bench_tensor = torch.from_numpy(X_test[:100]).to(device)
    t0 = time.perf_counter()
    with torch.no_grad():
        for _ in range(50):
            _ = model(bench_tensor)
    latency_ms = (time.perf_counter() - t0) / (50 * len(bench_tensor)) * 1000

    results = {
        "model_name": "1D CNN" if model_type == "cnn_1d" else "CNN-LSTM",
        "model_type": model_type,
        "pipeline": "Pipeline B (9 Raw Channels, 128 Timesteps)",
        "num_parameters": num_params,
        "train_samples": int(len(X_train)),
        "val_samples": int(len(X_val)),
        "test_samples": int(len(X_test)),
        "train_duration_sec": round(train_duration, 3),
        "inference_latency_ms": round(latency_ms, 4),
        "device_used": str(device),
        "history": history,
        "test_metrics": {
            "accuracy": float(test_acc),
            "macro_precision": float(macro_prec),
            "macro_recall": float(macro_rec),
            "macro_f1": float(macro_f1),
            "weighted_f1": float(weighted_f1),
            "loss": float(test_loss),
            "confusion_matrix": cm,
            "classification_report": clf_rep
        }
    }

    with open(metrics_file, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)

    print(f"\n[+] Official Test Evaluation:")
    print(f"    - Accuracy: {test_acc*100:.2f}%")
    print(f"    - Macro F1: {macro_f1*100:.2f}%")
    print(f"    - Weighted F1: {weighted_f1*100:.2f}%")
    print(f"    - Saved metrics to {metrics_file}")
    return results

if __name__ == "__main__":
    train_deep_model("cnn_1d")
    train_deep_model("cnn_lstm")
