export interface DatasetStats {
  dataset_name: string;
  source_url: string;
  total_windows: number;
  train_windows: number;
  test_windows: number;
  features_count: number;
  raw_channels_count: number;
  sampling_rate_hz: number;
  window_length_samples: number;
  activities: string[];
  train_subjects_count: number;
  test_subjects_count: number;
  verification_status: string;
}

export interface ModelInfo {
  model_id: string;
  model_name: string;
  pipeline_type: string;
  input_shape: string;
  num_parameters?: number;
  test_accuracy: number;
  macro_f1: number;
  weighted_f1: number;
  inference_latency_ms: number;
  description: string;
}

export interface WindowPrediction {
  window_index: number;
  predicted_activity: string;
  confidence: number;
  is_uncertain: boolean;
  probabilities: Record<string, number>;
  ground_truth?: string;
}

export interface PredictionSummary {
  total_windows: number;
  average_confidence: number;
  uncertain_count: number;
  activity_counts: Record<string, number>;
  activity_percentages: Record<string, number>;
  ground_truth_accuracy?: number;
}

export interface PredictionResult {
  prediction_id: number;
  filename: string;
  input_format: 'features' | 'raw_inertial';
  model_name: string;
  inference_time_ms: number;
  summary: PredictionSummary;
  timeline: WindowPrediction[];
}

export interface ValidationResult {
  valid: boolean;
  input_format?: 'features' | 'raw_inertial';
  num_samples_or_windows: number;
  detected_channels_or_features: number;
  has_ground_truth: boolean;
  errors: string[];
  warnings: string[];
}

export interface HistoryItem {
  id: number;
  filename: string;
  input_format: string;
  selected_model: string;
  num_windows: number;
  avg_confidence: number;
  activity_distribution: Record<string, number>;
  timeline_summary: WindowPrediction[];
  ground_truth_accuracy?: number;
  created_at: string;
}
