"""
Shared Configuration for MotionSense Human Activity Recognition
Defines:
- Exact 6 activity labels
- Exact 9 inertial signal channels order
- Window length (128) and sampling rate (50 Hz)
- Subject-independent splitting protocol
- Model hyperparameters
"""

from pathlib import Path
from dataclasses import dataclass, field
from typing import List, Dict

ROOT_DIR = Path(__file__).resolve().parent.parent.parent
DATA_DIR = ROOT_DIR / "ml" / "data" / "raw" / "UCI HAR Dataset"
PROCESSED_DIR = ROOT_DIR / "ml" / "data" / "processed"
ARTIFACTS_DIR = ROOT_DIR / "artifacts"
CHECKPOINTS_DIR = ARTIFACTS_DIR / "checkpoints"
METRICS_DIR = ARTIFACTS_DIR / "metrics"
PLOTS_DIR = ARTIFACTS_DIR / "plots"
METADATA_DIR = ARTIFACTS_DIR / "metadata"

# Official UCI HAR 240 Activity Labels
# 1-indexed in UCI original files, mapped to 0-indexed for PyTorch
ACTIVITY_MAP: Dict[int, str] = {
    1: "WALKING",
    2: "WALKING_UPSTAIRS",
    3: "WALKING_DOWNSTAIRS",
    4: "SITTING",
    5: "STANDING",
    6: "LAYING"
}

ACTIVITY_NAMES: List[str] = [
    "WALKING",
    "WALKING_UPSTAIRS",
    "WALKING_DOWNSTAIRS",
    "SITTING",
    "STANDING",
    "LAYING"
]

NUM_CLASSES = 6

# Exact 9-channel sensor order for Pipeline B (Raw Inertial Signals)
SENSOR_CHANNELS: List[str] = [
    "total_acc_x",
    "total_acc_y",
    "total_acc_z",
    "body_acc_x",
    "body_acc_y",
    "body_acc_z",
    "body_gyro_x",
    "body_gyro_y",
    "body_gyro_z"
]

NUM_CHANNELS = 9
WINDOW_SIZE = 128
SAMPLING_FREQ_HZ = 50
WINDOW_DURATION_SEC = 2.56
NUM_FEATURES_PIPELINE_A = 561

# Official Subject Partitions (30 total subjects)
# Official Test subjects: [2, 4, 9, 10, 12, 13, 18, 20, 24] (9 subjects)
# Official Train subjects: [1, 3, 5, 6, 7, 8, 11, 14, 15, 16, 17, 19, 21, 22, 23, 25, 26, 27, 28, 29, 30] (21 subjects)
# Subject-Aware Validation Split (from train subjects pool only to prevent leakage):
# Validation Subjects: [7, 14, 19, 23, 28] (5 subjects)
# Training Subjects: 16 remaining subjects
VAL_SUBJECTS: List[int] = [7, 14, 19, 23, 28]
OFFICIAL_TEST_SUBJECTS: List[int] = [2, 4, 9, 10, 12, 13, 18, 20, 24]

@dataclass
class RFConfig:
    n_estimators: int = 150
    max_depth: int = 25
    min_samples_split: int = 4
    min_samples_leaf: int = 2
    random_state: int = 42
    n_jobs: int = -1

@dataclass
class CNN1DConfig:
    in_channels: int = 9
    num_classes: int = 6
    conv1_filters: int = 64
    conv1_kernel: int = 5
    conv2_filters: int = 128
    conv2_kernel: int = 5
    conv3_filters: int = 128
    conv3_kernel: int = 3
    fc_dim: int = 128
    dropout: float = 0.3
    learning_rate: float = 0.001
    batch_size: int = 64
    epochs: int = 35
    early_stopping_patience: int = 8
    random_seed: int = 42

@dataclass
class CNNLSTMConfig:
    in_channels: int = 9
    num_classes: int = 6
    conv_filters: int = 64
    conv_kernel: int = 5
    lstm_hidden_dim: int = 128
    lstm_num_layers: int = 2
    fc_dim: int = 64
    dropout: float = 0.35
    learning_rate: float = 0.0008
    batch_size: int = 64
    epochs: int = 40
    early_stopping_patience: int = 8
    random_seed: int = 42
