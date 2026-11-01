import { 
  DatasetStats, 
  ModelInfo, 
  ValidationResult, 
  PredictionResult, 
  HistoryItem 
} from '../types';

const API_BASE = import.meta.env.VITE_API_URL || '/api';

export const api = {
  async getHealth() {
    const res = await fetch(`${API_BASE}/health`);
    if (!res.ok) throw new Error('Health check failed');
    return res.json();
  },

  async getDatasetInfo(): Promise<DatasetStats> {
    const res = await fetch(`${API_BASE}/dataset/info`);
    if (!res.ok) throw new Error('Failed to fetch dataset info');
    return res.json();
  },

  async getModels(): Promise<ModelInfo[]> {
    const res = await fetch(`${API_BASE}/models`);
    if (!res.ok) throw new Error('Failed to fetch models');
    return res.json();
  },

  async validateCSV(file: File): Promise<ValidationResult> {
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetch(`${API_BASE}/data/validate`, {
      method: 'POST',
      body: formData,
    });
    if (!res.ok) throw new Error('Validation request failed');
    return res.json();
  },

  async predictFeatures(file: File): Promise<PredictionResult> {
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetch(`${API_BASE}/predict/features`, {
      method: 'POST',
      body: formData,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ detail: 'Prediction failed' }));
      throw new Error(err.detail || 'Prediction failed');
    }
    return res.json();
  },

  async predictRaw(file: File, modelName: string): Promise<PredictionResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('model_name', modelName);
    const res = await fetch(`${API_BASE}/predict/raw`, {
      method: 'POST',
      body: formData,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ detail: 'Prediction failed' }));
      throw new Error(err.detail || 'Prediction failed');
    }
    return res.json();
  },

  async getEvaluationSummary() {
    const res = await fetch(`${API_BASE}/evaluation/summary`);
    if (!res.ok) throw new Error('Failed to fetch evaluation summary');
    return res.json();
  },

  async getHistory(): Promise<{ total: number; items: HistoryItem[] }> {
    const res = await fetch(`${API_BASE}/history`);
    if (!res.ok) throw new Error('Failed to fetch history');
    return res.json();
  },

  async deleteHistory(id: number) {
    const res = await fetch(`${API_BASE}/history/${id}`, {
      method: 'DELETE',
    });
    if (!res.ok) throw new Error('Failed to delete history item');
    return res.json();
  },

  getSampleDownloadUrl(type: 'features' | 'raw') {
    return `${API_BASE}/dataset/sample/${type}`;
  }
};
