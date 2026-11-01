"""
Comprehensive Model Evaluation and Visualization Suite
Generates:
- Standardized cross-model comparison
- Confusion matrices plots
- Training/Validation learning curves
- Per-class performance charts
- Latency and throughput benchmarks
- Publication-quality PNG artifacts
"""

import json
import time
import numpy as np
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from pathlib import Path
from typing import Dict, Any, List

from ml.configs.config import (
    PLOTS_DIR, 
    METRICS_DIR, 
    ACTIVITY_NAMES
)

PLOTS_DIR.mkdir(parents=True, exist_ok=True)
METRICS_DIR.mkdir(parents=True, exist_ok=True)

# Styling for research-quality plots
plt.rcParams['font.sans-serif'] = 'DejaVu Sans'
plt.rcParams['axes.edgecolor'] = '#cbd5e1'
plt.rcParams['axes.linewidth'] = 0.8

def plot_confusion_matrix(cm: List[List[int]], model_name: str, save_path: Path):
    cm_arr = np.array(cm)
    fig, ax = plt.subplots(figsize=(7, 6), dpi=300)
    im = ax.imshow(cm_arr, interpolation='nearest', cmap=plt.cm.Blues)
    ax.figure.colorbar(im, ax=ax, fraction=0.046, pad=0.04)

    ax.set(
        xticks=np.arange(cm_arr.shape[1]),
        yticks=np.arange(cm_arr.shape[0]),
        xticklabels=ACTIVITY_NAMES,
        yticklabels=ACTIVITY_NAMES,
        title=f"Confusion Matrix — {model_name} (Official Test Split)",
        ylabel="Ground Truth Label",
        xlabel="Predicted Label"
    )

    plt.setp(ax.get_xticklabels(), rotation=45, ha="right", rotation_mode="anchor")

    thresh = cm_arr.max() / 2.
    for i in range(cm_arr.shape[0]):
        for j in range(cm_arr.shape[1]):
            val = cm_arr[i, j]
            ax.text(j, i, f"{val:d}",
                    ha="center", va="center",
                    color="white" if val > thresh else "#0f172a",
                    fontsize=9, weight="bold")
    
    fig.tight_layout()
    plt.savefig(save_path, bbox_inches='tight')
    plt.close()
    print(f"[+] Saved confusion matrix plot to: {save_path}")

def plot_model_comparison(metrics_summary: List[Dict[str, Any]], save_path: Path):
    names = [m["model_name"] for m in metrics_summary]
    accuracies = [m["test_metrics"]["accuracy"] * 100 for m in metrics_summary]
    macro_f1s = [m["test_metrics"]["macro_f1"] * 100 for m in metrics_summary]
    weighted_f1s = [m["test_metrics"]["weighted_f1"] * 100 for m in metrics_summary]

    x = np.arange(len(names))
    width = 0.25

    fig, ax = plt.subplots(figsize=(8, 5), dpi=300)
    rects1 = ax.bar(x - width, accuracies, width, label='Accuracy (%)', color='#0d9488')
    rects2 = ax.bar(x, macro_f1s, width, label='Macro F1 (%)', color='#0284c7')
    rects3 = ax.bar(x + width, weighted_f1s, width, label='Weighted F1 (%)', color='#6366f1')

    ax.set_ylabel('Score (%)')
    ax.set_title('MotionSense — Official Test Set Performance Comparison')
    ax.set_xticks(x)
    ax.set_xticklabels(names, weight='bold')
    ax.legend(loc='lower right')
    ax.set_ylim(80, 100)
    ax.grid(axis='y', linestyle='--', alpha=0.5)

    def autolabel(rects):
        for rect in rects:
            height = rect.get_height()
            ax.annotate(f'{height:.1f}%',
                        xy=(rect.get_x() + rect.get_width() / 2, height),
                        xytext=(0, 3),
                        textcoords="offset points",
                        ha='center', va='bottom', fontsize=8, weight='bold')

    autolabel(rects1)
    autolabel(rects2)
    autolabel(rects3)

    fig.tight_layout()
    plt.savefig(save_path, bbox_inches='tight')
    plt.close()
    print(f"[+] Saved model comparison plot to: {save_path}")

def plot_training_curves(history_1d: Dict[str, Any], history_lstm: Dict[str, Any], save_path: Path):
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 4.5), dpi=300)

    # Loss Curves
    epochs_1d = range(1, len(history_1d["train_loss"]) + 1)
    epochs_lstm = range(1, len(history_lstm["train_loss"]) + 1)

    ax1.plot(epochs_1d, history_1d["train_loss"], label="1D CNN Train", color="#94a3b8", linestyle="--")
    ax1.plot(epochs_1d, history_1d["val_loss"], label="1D CNN Val", color="#0284c7")
    ax1.plot(epochs_lstm, history_lstm["train_loss"], label="CNN-LSTM Train", color="#cbd5e1", linestyle="--")
    ax1.plot(epochs_lstm, history_lstm["val_loss"], label="CNN-LSTM Val", color="#0d9488", linewidth=2)
    ax1.set_title("Cross-Entropy Loss vs. Epochs")
    ax1.set_xlabel("Epoch")
    ax1.set_ylabel("Loss")
    ax1.legend()
    ax1.grid(True, linestyle="--", alpha=0.5)

    # Accuracy Curves
    ax2.plot(epochs_1d, [x*100 for x in history_1d["val_acc"]], label="1D CNN Val Acc", color="#0284c7")
    ax2.plot(epochs_lstm, [x*100 for x in history_lstm["val_acc"]], label="CNN-LSTM Val Acc", color="#0d9488", linewidth=2)
    ax2.set_title("Subject-Aware Validation Accuracy vs. Epochs")
    ax2.set_xlabel("Epoch")
    ax2.set_ylabel("Accuracy (%)")
    ax2.legend()
    ax2.grid(True, linestyle="--", alpha=0.5)

    fig.tight_layout()
    plt.savefig(save_path, bbox_inches='tight')
    plt.close()
    print(f"[+] Saved training curves to: {save_path}")

def generate_full_evaluation_report():
    rf_file = METRICS_DIR / "rf_metrics.json"
    cnn_file = METRICS_DIR / "1d_cnn_metrics.json"
    lstm_file = METRICS_DIR / "cnn_lstm_metrics.json"

    if not rf_file.exists() or not cnn_file.exists() or not lstm_file.exists():
        print("[-] Training metrics files not found yet. Run training scripts first.")
        return

    with open(rf_file, "r") as f:
        rf_data = json.load(f)
    with open(cnn_file, "r") as f:
        cnn_data = json.load(f)
    with open(lstm_file, "r") as f:
        lstm_data = json.load(f)

    # 1. Plots
    plot_confusion_matrix(rf_data["test_metrics"]["confusion_matrix"], "Random Forest", PLOTS_DIR / "cm_random_forest.png")
    plot_confusion_matrix(cnn_data["test_metrics"]["confusion_matrix"], "1D CNN", PLOTS_DIR / "cm_1d_cnn.png")
    plot_confusion_matrix(lstm_data["test_metrics"]["confusion_matrix"], "CNN-LSTM", PLOTS_DIR / "cm_cnn_lstm.png")

    plot_model_comparison([rf_data, cnn_data, lstm_data], PLOTS_DIR / "model_comparison.png")
    plot_training_curves(cnn_data["history"], lstm_data["history"], PLOTS_DIR / "training_curves.png")

    # 2. Comprehensive report
    comprehensive = {
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "dataset": "UCI HAR Dataset 240",
        "models": {
            "random_forest": rf_data,
            "cnn_1d": cnn_data,
            "cnn_lstm": lstm_data
        },
        "summary_table": [
            {
                "model": "Random Forest",
                "pipeline": "Pipeline A (561 Features)",
                "test_accuracy": rf_data["test_metrics"]["accuracy"],
                "macro_f1": rf_data["test_metrics"]["macro_f1"],
                "weighted_f1": rf_data["test_metrics"]["weighted_f1"],
                "latency_ms": rf_data["inference_latency_ms"]
            },
            {
                "model": "1D CNN",
                "pipeline": "Pipeline B (9 Raw Channels)",
                "test_accuracy": cnn_data["test_metrics"]["accuracy"],
                "macro_f1": cnn_data["test_metrics"]["macro_f1"],
                "weighted_f1": cnn_data["test_metrics"]["weighted_f1"],
                "latency_ms": cnn_data["inference_latency_ms"]
            },
            {
                "model": "CNN-LSTM",
                "pipeline": "Pipeline B (9 Raw Channels)",
                "test_accuracy": lstm_data["test_metrics"]["accuracy"],
                "macro_f1": lstm_data["test_metrics"]["macro_f1"],
                "weighted_f1": lstm_data["test_metrics"]["weighted_f1"],
                "latency_ms": lstm_data["inference_latency_ms"]
            }
        ]
    }

    report_path = METRICS_DIR / "comprehensive_evaluation.json"
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(comprehensive, f, indent=2)

    print(f"\n[+] Comprehensive evaluation report generated at: {report_path}")

if __name__ == "__main__":
    generate_full_evaluation_report()
