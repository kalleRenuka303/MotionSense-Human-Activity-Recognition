"""
Random Forest Classifier Baseline for Pipeline A (561 features)
"""

import joblib
import numpy as np
from pathlib import Path
from typing import Dict, Any, Optional, List
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score, f1_score
from ml.configs.config import RFConfig, CHECKPOINTS_DIR, ACTIVITY_NAMES

CHECKPOINTS_DIR.mkdir(parents=True, exist_ok=True)

class MotionSenseRandomForest:
    def __init__(self, config: Optional[RFConfig] = None):
        self.config = config or RFConfig()
        self.model = RandomForestClassifier(
            n_estimators=self.config.n_estimators,
            max_depth=self.config.max_depth,
            min_samples_split=self.config.min_samples_split,
            min_samples_leaf=self.config.min_samples_leaf,
            random_state=self.config.random_state,
            n_jobs=self.config.n_jobs
        )
        self.is_fitted = False
        self.feature_names: List[str] = []

    def fit(self, X_train: np.ndarray, y_train: np.ndarray, feature_names: Optional[List[str]] = None):
        self.model.fit(X_train, y_train)
        self.is_fitted = True
        if feature_names is not None:
            self.feature_names = feature_names

    def predict(self, X: np.ndarray) -> np.ndarray:
        return self.model.predict(X)

    def predict_proba(self, X: np.ndarray) -> np.ndarray:
        return self.model.predict_proba(X)

    def evaluate(self, X_test: np.ndarray, y_test: np.ndarray) -> Dict[str, Any]:
        y_pred = self.predict(X_test)
        y_proba = self.predict_proba(X_test)
        
        acc = accuracy_score(y_test, y_pred)
        macro_f1 = f1_score(y_test, y_pred, average="macro")
        weighted_f1 = f1_score(y_test, y_pred, average="weighted")
        cm = confusion_matrix(y_test, y_pred).tolist()
        clf_report = classification_report(y_test, y_pred, target_names=ACTIVITY_NAMES, output_dict=True)

        return {
            "model_name": "Random Forest",
            "accuracy": float(acc),
            "macro_f1": float(macro_f1),
            "weighted_f1": float(weighted_f1),
            "confusion_matrix": cm,
            "classification_report": clf_report,
            "num_test_samples": len(y_test)
        }

    def get_feature_importances(self, top_k: int = 20) -> List[Dict[str, Any]]:
        if not self.is_fitted:
            raise RuntimeError("Model is not fitted yet.")
        importances = self.model.feature_importances_
        indices = np.argsort(importances)[::-1]
        
        top_features = []
        for rank, idx in enumerate(indices[:top_k], 1):
            name = self.feature_names[idx] if self.feature_names and idx < len(self.feature_names) else f"feature_{idx}"
            top_features.append({
                "rank": rank,
                "feature_index": int(idx),
                "feature_name": name,
                "importance": float(importances[idx])
            })
        return top_features

    def save(self, filepath: Optional[Path] = None):
        path = filepath or (CHECKPOINTS_DIR / "random_forest.joblib")
        joblib.dump({
            "model": self.model,
            "config": self.config,
            "feature_names": self.feature_names,
            "is_fitted": self.is_fitted
        }, path)
        print(f"[+] Saved Random Forest checkpoint to: {path}")

    @classmethod
    def load(cls, filepath: Optional[Path] = None) -> "MotionSenseRandomForest":
        path = filepath or (CHECKPOINTS_DIR / "random_forest.joblib")
        if not path.exists():
            raise FileNotFoundError(f"Random Forest checkpoint not found at: {path}")
        data = joblib.load(path)
        instance = cls(config=data.get("config"))
        instance.model = data["model"]
        instance.feature_names = data.get("feature_names", [])
        instance.is_fitted = data.get("is_fitted", True)
        return instance
