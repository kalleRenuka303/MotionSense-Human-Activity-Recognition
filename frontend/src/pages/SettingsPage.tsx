import React, { useState } from 'react';
import { 
  Settings as SettingsIcon, 
  ShieldCheck, 
  Save, 
  Server, 
  Sliders, 
  Moon, 
  Sun,
  Database,
  CheckCircle,
  XCircle,
  Loader2
} from 'lucide-react';
import { api } from '../services/api';

export const SettingsPage: React.FC = () => {
  const [uncertaintyThreshold, setUncertaintyThreshold] = useState<number>(() => {
    const val = localStorage.getItem('motionsense_threshold');
    return val ? parseFloat(val) : 0.55;
  });
  const [defaultModel, setDefaultModel] = useState<string>(() => {
    return localStorage.getItem('motionsense_default_model') || 'cnn_lstm';
  });
  const [apiUrl, setApiUrl] = useState<string>(() => {
    return localStorage.getItem('motionsense_api_url') || (import.meta.env.VITE_API_URL as string) || '/api';
  });
  const [saved, setSaved] = useState(false);
  const [testStatus, setTestStatus] = useState<'idle' | 'testing' | 'success' | 'error'>('idle');
  const [testMessage, setTestMessage] = useState<string>('');

  const handleSave = () => {
    localStorage.setItem('motionsense_threshold', String(uncertaintyThreshold));
    localStorage.setItem('motionsense_default_model', defaultModel);
    localStorage.setItem('motionsense_api_url', apiUrl.trim());
    setSaved(true);
    setTimeout(() => setSaved(false), 2500);
  };

  const testConnection = async () => {
    setTestStatus('testing');
    setTestMessage('');
    try {
      localStorage.setItem('motionsense_api_url', apiUrl.trim());
      const res = await api.getHealth();
      setTestStatus('success');
      setTestMessage(`Connected to ${res.service || 'MotionSense'} (Status: ${res.status})`);
    } catch (err: any) {
      setTestStatus('error');
      setTestMessage(err.message || 'Connection failed');
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300 max-w-4xl">
      {/* Header */}
      <div className="bg-white border border-slate-200 p-6 rounded-2xl shadow-xs">
        <div className="flex items-center space-x-2 text-xs font-semibold text-teal-700 uppercase tracking-wider">
          <SettingsIcon className="w-4 h-4" />
          <span>System Preferences</span>
        </div>
        <h1 className="text-2xl font-bold text-slate-900 mt-1">Platform Settings</h1>
        <p className="text-sm text-slate-500 mt-1">
          Configure model uncertainty thresholds, API endpoints, and inference defaults.
        </p>
      </div>

      {/* Main Settings Card */}
      <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs space-y-6">
        {/* Uncertainty Threshold */}
        <div>
          <div className="flex justify-between items-center mb-1">
            <label className="text-sm font-bold text-slate-900 flex items-center space-x-2">
              <Sliders className="w-4 h-4 text-teal-600" />
              <span>Model Uncertainty Flagging Threshold</span>
            </label>
            <span className="text-xs font-mono font-bold px-2 py-0.5 rounded-md bg-teal-50 text-teal-700 border border-teal-200">
              {(uncertaintyThreshold * 100).toFixed(0)}%
            </span>
          </div>
          <p className="text-xs text-slate-500 mb-3">
            Predictions with maximum class probability below this value will be flagged as uncertain rather than forced into a definitive activity category.
          </p>
          <input
            type="range"
            min="0.30"
            max="0.85"
            step="0.05"
            value={uncertaintyThreshold}
            onChange={(e) => setUncertaintyThreshold(parseFloat(e.target.value))}
            className="w-full accent-teal-600 cursor-pointer"
          />
          <div className="flex justify-between text-[10px] text-slate-400 mt-1 font-mono">
            <span>30% (Permissive)</span>
            <span>55% (Recommended)</span>
            <span>85% (Conservative)</span>
          </div>
        </div>

        <div className="border-t border-slate-100 pt-6">
          <label className="text-sm font-bold text-slate-900 flex items-center space-x-2 mb-1">
            <Server className="w-4 h-4 text-teal-600" />
            <span>Default Deep Learning Model</span>
          </label>
          <p className="text-xs text-slate-500 mb-3">
            Architecture preselected when uploading raw 9-channel sensor recordings.
          </p>
          <select
            value={defaultModel}
            onChange={(e) => setDefaultModel(e.target.value)}
            className="w-full sm:w-80 px-3.5 py-2.5 rounded-xl border border-slate-200 bg-slate-50 text-xs font-medium text-slate-800 focus:bg-white focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500 cursor-pointer"
          >
            <option value="cnn_lstm">CNN-LSTM (Proposed Research - Highest Accuracy)</option>
            <option value="cnn_1d">1D CNN (Fastest Deep Learning Inference)</option>
          </select>
        </div>

        <div className="border-t border-slate-100 pt-6">
          <label className="text-sm font-bold text-slate-900 flex items-center space-x-2 mb-1">
            <Database className="w-4 h-4 text-teal-600" />
            <span>FastAPI Backend Endpoint URL</span>
          </label>
          <p className="text-xs text-slate-500 mb-3">
            Target REST API endpoint for local development or production deployment (e.g. <code>https://your-api.onrender.com/api</code> or <code>/api</code>).
          </p>
          <div className="flex flex-col sm:flex-row items-start sm:items-center space-y-2 sm:space-y-0 sm:space-x-3">
            <input
              type="text"
              value={apiUrl}
              onChange={(e) => setApiUrl(e.target.value)}
              placeholder="/api or https://your-backend.com/api"
              className="w-full sm:w-96 px-3.5 py-2 rounded-xl border border-slate-200 bg-slate-50 text-xs font-mono text-slate-800 focus:bg-white focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
            />
            <button
              onClick={testConnection}
              disabled={testStatus === 'testing'}
              className="px-4 py-2 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 text-slate-700 font-semibold text-xs transition-all flex items-center space-x-1.5 cursor-pointer disabled:opacity-50"
            >
              {testStatus === 'testing' ? (
                <>
                  <Loader2 className="w-3.5 h-3.5 animate-spin text-teal-600" />
                  <span>Testing...</span>
                </>
              ) : (
                <>
                  <Server className="w-3.5 h-3.5 text-slate-600" />
                  <span>Test Connection</span>
                </>
              )}
            </button>
          </div>
          {testStatus === 'success' && (
            <p className="text-xs font-semibold text-emerald-600 mt-2 flex items-center space-x-1.5">
              <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
              <span>{testMessage}</span>
            </p>
          )}
          {testStatus === 'error' && (
            <p className="text-xs font-semibold text-rose-600 mt-2 flex items-center space-x-1.5">
              <XCircle className="w-3.5 h-3.5 text-rose-600" />
              <span>{testMessage}</span>
            </p>
          )}
        </div>

        <div className="border-t border-slate-100 pt-6 flex items-center justify-between">
          <button
            onClick={handleSave}
            className="px-6 py-2.5 rounded-xl bg-teal-600 hover:bg-teal-700 text-white font-semibold text-xs shadow-xs transition-all flex items-center space-x-2 cursor-pointer"
          >
            <Save className="w-4 h-4" />
            <span>Save Preferences</span>
          </button>

          {saved && (
            <span className="text-xs font-bold text-emerald-600 animate-in fade-in">
              ✓ Preferences saved successfully
            </span>
          )}
        </div>
      </div>
    </div>
  );
};
