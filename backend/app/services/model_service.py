import os
import time
import torch
import numpy as np
from pathlib import Path
from typing import Dict, Any, List, Optional, Tuple

from backend.app.core.config import settings
from ml.configs.config import ACTIVITY_NAMES, SENSOR_CHANNELS, NUM_FEATURES_PIPELINE_A
from ml.models.random_forest_model import MotionSenseRandomForest
from ml.models.cnn_1d_model import MotionSense1DCNN
from ml.models.cnn_lstm_model import MotionSenseCNNLSTM
from backend.app.schemas.predict import WindowPrediction, PredictionSummary, PredictionResponse

class ModelService:
    def __init__(self):
        target_device = os.environ.get("DEVICE", "cpu")
        if target_device == "cuda" and torch.cuda.is_available():
            self.device = torch.device("cuda")
        elif target_device == "mps" and torch.backends.mps.is_available():
            self.device = torch.device("mps")
        else:
            self.device = torch.device("cpu")
        self.rf_model: Optional[MotionSenseRandomForest] = None
        self.cnn_model: Optional[MotionSense1DCNN] = None
        self.cnn_lstm_model: Optional[MotionSenseCNNLSTM] = None
        self.models_loaded = False

    def load_models(self):
        # 1. Random Forest
        rf_path = settings.CHECKPOINTS_DIR / "random_forest.joblib"
        if rf_path.exists():
            try:
                self.rf_model = MotionSenseRandomForest.load(rf_path)
                print("[+] Loaded Random Forest model.")
            except Exception as e:
                print(f"[-] Could not load Random Forest: {e}")

        # 2. 1D CNN
        cnn_path = settings.CHECKPOINTS_DIR / "best_1d_cnn.pt"
        if cnn_path.exists():
            try:
                cp = torch.load(cnn_path, map_location=self.device, weights_only=False)
                self.cnn_model = MotionSense1DCNN(config=cp.get("config")).to(self.device)
                self.cnn_model.load_state_dict(cp["model_state_dict"])
                self.cnn_model.eval()
                print("[+] Loaded 1D CNN model.")
            except Exception as e:
                print(f"[-] Could not load 1D CNN: {e}")

        # 3. CNN-LSTM
        lstm_path = settings.CHECKPOINTS_DIR / "best_cnn_lstm.pt"
        if lstm_path.exists():
            try:
                cp = torch.load(lstm_path, map_location=self.device, weights_only=False)
                self.cnn_lstm_model = MotionSenseCNNLSTM(config=cp.get("config")).to(self.device)
                self.cnn_lstm_model.load_state_dict(cp["model_state_dict"])
                self.cnn_lstm_model.eval()
                print("[+] Loaded CNN-LSTM model.")
            except Exception as e:
                print(f"[-] Could not load CNN-LSTM: {e}")

        self.models_loaded = True

    def predict_features(
        self, 
        features_array: np.ndarray, 
        ground_truth: Optional[List[str]] = None
    ) -> Tuple[List[WindowPrediction], PredictionSummary, float]:
        """
        Runs Random Forest on 561-feature vectors: shape (N, 561)
        """
        if self.rf_model is None:
            self.load_models()
        if self.rf_model is None:
            raise RuntimeError("Random Forest model artifact is not loaded.")

        start_time = time.perf_counter()
        probabilities = self.rf_model.predict_proba(features_array)
        latency_ms = (time.perf_counter() - start_time) * 1000

        predictions = []
        confidences = []
        uncertain_count = 0
        activity_counts = {name: 0 for name in ACTIVITY_NAMES}
        correct_count = 0

        for i in range(len(features_array)):
            probs = probabilities[i]
            pred_idx = int(np.argmax(probs))
            conf = float(probs[pred_idx])
            pred_activity = ACTIVITY_NAMES[pred_idx]
            is_uncertain = conf < settings.CONFIDENCE_UNCERTAINTY_THRESHOLD

            if is_uncertain:
                uncertain_count += 1
            confidences.append(conf)
            activity_counts[pred_activity] += 1

            prob_dict = {ACTIVITY_NAMES[j]: round(float(probs[j]), 4) for j in range(len(ACTIVITY_NAMES))}
            gt = ground_truth[i] if ground_truth and i < len(ground_truth) else None
            if gt and gt.upper() == pred_activity.upper():
                correct_count += 1

            predictions.append(WindowPrediction(
                window_index=i + 1,
                predicted_activity=pred_activity,
                confidence=round(conf, 4),
                is_uncertain=is_uncertain,
                probabilities=prob_dict,
                ground_truth=gt
            ))

        total_windows = len(features_array)
        avg_conf = float(np.mean(confidences)) if confidences else 0.0
        pct_dist = {k: round((v / total_windows) * 100, 2) for k, v in activity_counts.items()}
        gt_acc = round(correct_count / total_windows, 4) if ground_truth else None

        summary = PredictionSummary(
            total_windows=total_windows,
            average_confidence=round(avg_conf, 4),
            uncertain_count=uncertain_count,
            activity_counts=activity_counts,
            activity_percentages=pct_dist,
            ground_truth_accuracy=gt_acc
        )

        return predictions, summary, latency_ms

    def predict_raw(
        self, 
        raw_windows: np.ndarray, 
        model_name: str = "cnn_lstm", 
        ground_truth: Optional[List[str]] = None
    ) -> Tuple[List[WindowPrediction], PredictionSummary, float]:
        """
        Runs 1D CNN or CNN-LSTM on raw sensor windows: shape (N, 128, 9)
        """
        if not self.models_loaded:
            self.load_models()

        if model_name.lower() in ["1d_cnn", "cnn_1d", "cnn"]:
            model = self.cnn_model
            model_label = "1D CNN"
        else:
            model = self.cnn_lstm_model
            model_label = "CNN-LSTM"

        if model is None:
            raise RuntimeError(f"{model_label} model artifact is not loaded.")

        tensor_x = torch.from_numpy(raw_windows).to(self.device)

        start_time = time.perf_counter()
        with torch.no_grad():
            logits = model(tensor_x)
            probabilities = torch.softmax(logits, dim=-1).cpu().numpy()
        latency_ms = (time.perf_counter() - start_time) * 1000

        predictions = []
        confidences = []
        uncertain_count = 0
        activity_counts = {name: 0 for name in ACTIVITY_NAMES}
        correct_count = 0

        for i in range(len(raw_windows)):
            probs = probabilities[i]
            pred_idx = int(np.argmax(probs))
            conf = float(probs[pred_idx])
            pred_activity = ACTIVITY_NAMES[pred_idx]
            is_uncertain = conf < settings.CONFIDENCE_UNCERTAINTY_THRESHOLD

            if is_uncertain:
                uncertain_count += 1
            confidences.append(conf)
            activity_counts[pred_activity] += 1

            prob_dict = {ACTIVITY_NAMES[j]: round(float(probs[j]), 4) for j in range(len(ACTIVITY_NAMES))}
            gt = ground_truth[i] if ground_truth and i < len(ground_truth) else None
            if gt and gt.upper() == pred_activity.upper():
                correct_count += 1

            predictions.append(WindowPrediction(
                window_index=i + 1,
                predicted_activity=pred_activity,
                confidence=round(conf, 4),
                is_uncertain=is_uncertain,
                probabilities=prob_dict,
                ground_truth=gt
            ))

        total_windows = len(raw_windows)
        avg_conf = float(np.mean(confidences)) if confidences else 0.0
        pct_dist = {k: round((v / total_windows) * 100, 2) for k, v in activity_counts.items()}
        gt_acc = round(correct_count / total_windows, 4) if ground_truth else None

        summary = PredictionSummary(
            total_windows=total_windows,
            average_confidence=round(avg_conf, 4),
            uncertain_count=uncertain_count,
            activity_counts=activity_counts,
            activity_percentages=pct_dist,
            ground_truth_accuracy=gt_acc
        )

        return predictions, summary, latency_ms

model_service = ModelService()
