from typing import List, Dict, Any, Optional
from pydantic import BaseModel

class ActivityProbability(BaseModel):
    activity: str
    probability: float

class WindowPrediction(BaseModel):
    window_index: int
    predicted_activity: str
    confidence: float
    is_uncertain: bool
    probabilities: Dict[str, float]
    ground_truth: Optional[str] = None

class PredictionSummary(BaseModel):
    total_windows: int
    average_confidence: float
    uncertain_count: int
    activity_counts: Dict[str, int]
    activity_percentages: Dict[str, float]
    ground_truth_accuracy: Optional[float] = None

class PredictionResponse(BaseModel):
    prediction_id: int
    filename: str
    input_format: str
    model_name: str
    inference_time_ms: float
    summary: PredictionSummary
    timeline: List[WindowPrediction]

class ValidationResponse(BaseModel):
    valid: bool
    input_format: Optional[str] = None  # 'features' or 'raw_inertial'
    num_samples_or_windows: int
    detected_channels_or_features: int
    has_ground_truth: bool
    errors: List[str] = []
    warnings: List[str] = []

class ModelInfoResponse(BaseModel):
    model_id: str
    model_name: str
    pipeline_type: str
    input_shape: str
    num_parameters: Optional[int] = None
    test_accuracy: float
    macro_f1: float
    weighted_f1: float
    inference_latency_ms: float
    description: str

class DatasetStatsResponse(BaseModel):
    dataset_name: str
    source_url: str
    total_windows: int
    train_windows: int
    test_windows: int
    features_count: int
    raw_channels_count: int
    sampling_rate_hz: int
    window_length_samples: int
    activities: List[str]
    train_subjects_count: int
    test_subjects_count: int
    verification_status: str
