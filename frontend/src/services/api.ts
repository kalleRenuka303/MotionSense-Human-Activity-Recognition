import { 
  DatasetStats, 
  ModelInfo, 
  ValidationResult, 
  PredictionResult, 
  HistoryItem 
} from '../types';

export const getApiBase = (): string => {
  if (typeof window !== 'undefined') {
    const custom = localStorage.getItem('motionsense_api_url');
    if (custom && custom.trim() !== '') {
      return custom.trim().replace(/\/$/, '');
    }
  }
  return (import.meta.env.VITE_API_URL as string) || '/api';
};

export const api = {
  async getHealth() {
    const res = await fetch(`${getApiBase()}/health`);
    if (!res.ok) throw new Error('Health check failed');
    return res.json();
  },

  async getDatasetInfo(): Promise<DatasetStats> {
    const res = await fetch(`${getApiBase()}/dataset/info`);
    if (!res.ok) throw new Error('Failed to fetch dataset info');
    return res.json();
  },

  async getModels(): Promise<ModelInfo[]> {
    const res = await fetch(`${getApiBase()}/models`);
    if (!res.ok) throw new Error('Failed to fetch models');
    return res.json();
  },

  async validateCSV(file: File): Promise<ValidationResult> {
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetch(`${getApiBase()}/data/validate`, {
      method: 'POST',
      body: formData,
    });
    if (!res.ok) throw new Error('Validation request failed');
    return res.json();
  },

  async predictFeatures(file: File): Promise<PredictionResult> {
    const formData = new FormData();
    formData.append('file', file);
    const res = await fetch(`${getApiBase()}/predict/features`, {
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
    const res = await fetch(`${getApiBase()}/predict/raw`, {
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
    const res = await fetch(`${getApiBase()}/evaluation/summary`);
    if (!res.ok) throw new Error('Failed to fetch evaluation summary');
    return res.json();
  },

  async getHistory(): Promise<{ total: number; items: HistoryItem[] }> {
    const res = await fetch(`${getApiBase()}/history`);
    if (!res.ok) throw new Error('Failed to fetch history');
    return res.json();
  },

  async deleteHistory(id: number) {
    const res = await fetch(`${getApiBase()}/history/${id}`, {
      method: 'DELETE',
    });
    if (!res.ok) throw new Error('Failed to delete history item');
    return res.json();
  },

  getSampleDownloadUrl(type: 'features' | 'raw') {
    return `${getApiBase()}/dataset/sample/${type}`;
  }
};
