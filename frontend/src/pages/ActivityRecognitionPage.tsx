import React, { useState, useRef } from 'react';
import { 
  Upload, 
  FileText, 
  CheckCircle2, 
  AlertTriangle, 
  Cpu, 
  Activity, 
  Download, 
  RefreshCw,
  Clock,
  Layers,
  Sparkles,
  HelpCircle
} from 'lucide-react';
import { 
  ResponsiveContainer, 
  PieChart, 
  Pie, 
  Cell, 
  Tooltip 
} from 'recharts';
import { ValidationResult, PredictionResult, ModelInfo } from '../types';
import { api } from '../services/api';

interface ActivityRecognitionProps {
  models: ModelInfo[];
  onPredictionComplete?: () => void;
}

const COLOR_MAP: Record<string, string> = {
  WALKING: '#0d9488',
  WALKING_UPSTAIRS: '#14b8a6',
  WALKING_DOWNSTAIRS: '#2dd4bf',
  SITTING: '#0284c7',
  STANDING: '#38bdf8',
  LAYING: '#6366f1',
  UNCERTAIN: '#f59e0b'
};

export const ActivityRecognitionPage: React.FC<ActivityRecognitionProps> = ({ models, onPredictionComplete }) => {
  const [file, setFile] = useState<File | null>(null);
  const [isValidating, setIsValidating] = useState(false);
  const [validation, setValidation] = useState<ValidationResult | null>(null);
  const [selectedModel, setSelectedModel] = useState<string>('cnn_lstm');
  const [isPredicting, setIsPredicting] = useState(false);
  const [prediction, setPrediction] = useState<PredictionResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const processSelectedFile = async (selected: File) => {
    setFile(selected);
    setError(null);
    setPrediction(null);
    setValidation(null);

    // 1. Check for empty files (0 bytes)
    if (selected.size === 0) {
      const emptyError = 'The selected file is empty (0 bytes). Please upload a valid CSV file containing sensor recordings.';
      setError(emptyError);
      setValidation({
        valid: false,
        num_samples_or_windows: 0,
        detected_channels_or_features: 0,
        has_ground_truth: false,
        errors: [emptyError],
        warnings: []
      });
      return;
    }

    // 2. Validate file extension (.csv case-insensitive) without restricting on Android MIME type
    const filename = selected.name || '';
    if (!filename.toLowerCase().endsWith('.csv')) {
      const extError = `Unsupported file type: "${filename}". Only CSV files (.csv) are supported.`;
      setError(extError);
      setValidation({
        valid: false,
        num_samples_or_windows: 0,
        detected_channels_or_features: 0,
        has_ground_truth: false,
        errors: [extError],
        warnings: []
      });
      return;
    }

    // 3. Validate contents via backend
    await validateFile(selected);
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selected = e.target.files[0];
      await processSelectedFile(selected);
    }
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const dropped = e.dataTransfer.files[0];
      await processSelectedFile(dropped);
    }
  };

  const validateFile = async (f: File) => {
    setIsValidating(true);
    try {
      const result = await api.validateCSV(f);
      setValidation(result);
      if (result.valid) {
        if (result.input_format === 'features') {
          setSelectedModel('random_forest');
        } else if (result.input_format === 'raw_inertial') {
          setSelectedModel('cnn_lstm');
        }
      } else {
        if (result.errors && result.errors.length > 0) {
          setError(result.errors.join('; '));
        } else {
          setError('CSV schema validation failed. Please check your columns.');
        }
      }
    } catch (err: any) {
      const msg = err.message || 'Error validating file';
      setError(msg);
      setValidation({
        valid: false,
        num_samples_or_windows: 0,
        detected_channels_or_features: 0,
        has_ground_truth: false,
        errors: [msg],
        warnings: []
      });
    } finally {
      setIsValidating(false);
    }
  };

  const runInference = async () => {
    if (!file || !validation?.valid) return;
    setIsPredicting(true);
    setError(null);
    try {
      let res: PredictionResult;
      if (validation.input_format === 'features') {
        res = await api.predictFeatures(file);
      } else {
        res = await api.predictRaw(file, selectedModel);
      }
      setPrediction(res);
      if (onPredictionComplete) onPredictionComplete();
    } catch (err: any) {
      setError(err.message || 'Error during inference');
    } finally {
      setIsPredicting(false);
    }
  };

  const exportJSON = () => {
    if (!prediction) return;
    const blob = new Blob([JSON.stringify(prediction, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `motionsense_prediction_${prediction.prediction_id}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const pieData = prediction
    ? Object.entries(prediction.summary.activity_counts).map(([name, count]) => ({
        name,
        value: count,
        color: COLOR_MAP[name] || '#94a3b8'
      })).filter(x => x.value > 0)
    : [];

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Title */}
      <div className="bg-white border border-slate-200 p-6 rounded-2xl shadow-xs">
        <h1 className="text-2xl font-bold text-slate-900">Activity Recognition Inference</h1>
        <p className="text-sm text-slate-500 mt-1">
          Upload pre-recorded smartphone sensor data in CSV format for automated physical activity classification.
        </p>
      </div>

      {/* Grid: Upload & Settings */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Upload Box */}
        <div className="lg:col-span-2 bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
          <h2 className="text-base font-bold text-slate-900 mb-2">1. Select or Drop Sensor CSV</h2>
          <p className="text-xs text-slate-500 mb-4">
            Supports Pipeline A (561 features) or Pipeline B (9 raw inertial signal channels).
          </p>

          <label
            htmlFor="sensor-csv-upload-input"
            onDragOver={(e) => {
              e.preventDefault();
              e.stopPropagation();
            }}
            onDragEnter={(e) => {
              e.preventDefault();
              e.stopPropagation();
            }}
            onDrop={handleDrop}
            className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all block select-none ${
              file
                ? 'border-teal-500 bg-teal-50/20'
                : 'border-slate-300 hover:border-teal-400 bg-slate-50/50 hover:bg-slate-50 active:bg-teal-50/10'
            }`}
          >
            <input
              type="file"
              id="sensor-csv-upload-input"
              ref={fileInputRef}
              onChange={handleFileChange}
              onClick={(e) => {
                // Clear input value to ensure re-selecting same file triggers onChange
                (e.target as HTMLInputElement).value = '';
              }}
              accept=".csv,text/csv"
              className="sr-only"
            />
            <div className="w-12 h-12 mx-auto rounded-full bg-teal-50 text-teal-600 flex items-center justify-center mb-3">
              <Upload className="w-6 h-6" />
            </div>
            {file ? (
              <div>
                <p className="font-semibold text-sm text-slate-900 break-all">{file.name}</p>
                <div className="flex flex-wrap items-center justify-center gap-2 text-xs text-slate-500 mt-1.5">
                  <span>{(file.size / 1024).toFixed(1)} KB</span>
                  <span>&bull;</span>
                  {isValidating ? (
                    <span className="text-teal-600 font-semibold flex items-center gap-1">
                      <RefreshCw className="w-3 h-3 animate-spin" />
                      Validating...
                    </span>
                  ) : validation?.valid ? (
                    <span className="text-emerald-700 font-semibold bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-200">
                      ✓ Valid {validation.input_format === 'features' ? 'Pipeline A (Features)' : 'Pipeline B (Raw Signals)'}
                    </span>
                  ) : validation?.valid === false ? (
                    <span className="text-rose-700 font-semibold bg-rose-50 px-2 py-0.5 rounded-md border border-rose-200">
                      ✗ Invalid Schema
                    </span>
                  ) : (
                    <span>Ready</span>
                  )}
                  <span>&bull;</span>
                  <span className="text-teal-600 underline font-medium">Tap to change</span>
                </div>
              </div>
            ) : (
              <div>
                <p className="font-semibold text-sm text-slate-800">Select or drop smartphone sensor CSV</p>
                <p className="text-xs text-slate-400 mt-1">Supports Pipeline A (561 features) or Pipeline B (9 raw channels)</p>
                <div className="mt-3">
                  <span className="inline-flex items-center px-4 py-2 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-xs font-semibold shadow-xs transition-all pointer-events-none">
                    Browse CSV Files
                  </span>
                </div>
              </div>
            )}
          </label>

          {/* Validation Feedback */}
          {isValidating && (
            <div className="mt-4 p-4 rounded-xl bg-slate-50 border border-slate-200 flex items-center space-x-3 text-xs text-slate-600">
              <RefreshCw className="w-4 h-4 text-teal-600 animate-spin" />
              <span>Analyzing CSV headers, data types, and sensor dimensions...</span>
            </div>
          )}

          {validation && !isValidating && (
            <div className={`mt-4 p-4 rounded-xl border text-xs ${
              validation.valid
                ? 'bg-emerald-50/60 border-emerald-200 text-emerald-900'
                : 'bg-rose-50/60 border-rose-200 text-rose-900'
            }`}>
              <div className="flex items-start space-x-2.5">
                {validation.valid ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                ) : (
                  <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                )}
                <div>
                  <h4 className="font-bold">
                    {validation.valid ? 'File Validation Passed' : 'Validation Failed'}
                  </h4>
                  {validation.valid ? (
                    <div className="mt-1 space-y-0.5 text-slate-600">
                      <p>
                        Detected Schema: <strong className="uppercase">{validation.input_format?.replace('_', ' ')}</strong> ({validation.detected_channels_or_features} columns)
                      </p>
                      <p>
                        Input Size: {validation.num_samples_or_windows.toLocaleString()} rows {validation.has_ground_truth ? '(Ground truth labels detected)' : '(Unlabeled sensor recording)'}
                      </p>
                    </div>
                  ) : (
                    <ul className="mt-1 list-disc list-inside space-y-0.5 text-rose-700">
                      {validation.errors.map((e, i) => (
                        <li key={i}>{e}</li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Model Selection & Run Panel */}
        <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs flex flex-col justify-between">
          <div>
            <h2 className="text-base font-bold text-slate-900 mb-2">2. Inference Configuration</h2>
            <p className="text-xs text-slate-500 mb-4">
              Select model architecture corresponding to the validated input schema.
            </p>

            <div className="space-y-3">
              <label className="text-xs font-semibold text-slate-700 uppercase tracking-wider block">
                Target Classification Model
              </label>

              {validation?.input_format === 'features' ? (
                <div className="p-3.5 rounded-xl border-2 border-teal-500 bg-teal-50/20 text-xs">
                  <div className="flex items-center justify-between font-bold text-slate-900">
                    <span>Random Forest Baseline</span>
                    <span className="text-[10px] bg-teal-100 text-teal-800 px-2 py-0.5 rounded-full">Pipeline A</span>
                  </div>
                  <p className="text-slate-500 mt-1">Optimized for 561 precomputed features. 150 ensemble trees.</p>
                </div>
              ) : (
                <div className="space-y-2">
                  <button
                    onClick={() => setSelectedModel('cnn_lstm')}
                    className={`w-full p-3.5 rounded-xl border text-left transition-all cursor-pointer ${
                      selectedModel === 'cnn_lstm'
                        ? 'border-teal-500 bg-teal-50/30'
                        : 'border-slate-200 hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-center justify-between text-xs font-bold text-slate-900">
                      <span>CNN-LSTM (Proposed Research)</span>
                      <span className="text-[10px] bg-teal-100 text-teal-800 px-2 py-0.5 rounded-full">Recommended</span>
                    </div>
                    <p className="text-[11px] text-slate-500 mt-0.5">Spatial Conv1D + 2-layer temporal LSTM. Best overall accuracy.</p>
                  </button>

                  <button
                    onClick={() => setSelectedModel('1d_cnn')}
                    className={`w-full p-3.5 rounded-xl border text-left transition-all cursor-pointer ${
                      selectedModel === '1d_cnn'
                        ? 'border-teal-500 bg-teal-50/30'
                        : 'border-slate-200 hover:border-slate-300'
                    }`}
                  >
                    <div className="flex items-center justify-between text-xs font-bold text-slate-900">
                      <span>1D CNN Neural Net</span>
                      <span className="text-[10px] bg-slate-100 text-slate-700 px-2 py-0.5 rounded-full">Deep Learning</span>
                    </div>
                    <p className="text-[11px] text-slate-500 mt-0.5">3-layer 1D CNN with Batch Normalization. Ultra-fast inference.</p>
                  </button>
                </div>
              )}
            </div>
          </div>

          <div className="mt-6 pt-4 border-t border-slate-100">
            {error && (
              <p className="text-xs text-rose-600 font-medium mb-3">{error}</p>
            )}

            <button
              disabled={!file || !validation?.valid || isPredicting}
              onClick={runInference}
              className={`w-full py-3 rounded-xl font-bold text-sm flex items-center justify-center space-x-2 shadow-xs transition-all cursor-pointer ${
                !file || !validation?.valid || isPredicting
                  ? 'bg-slate-200 text-slate-400 cursor-not-allowed'
                  : 'bg-teal-600 hover:bg-teal-700 text-white'
              }`}
            >
              {isPredicting ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Processing Sensors...</span>
                </>
              ) : (
                <>
                  <Activity className="w-4 h-4" />
                  <span>Run Activity Recognition</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Results View */}
      {prediction && (
        <div className="space-y-6 animate-in slide-in-from-bottom-4 duration-300">
          {/* Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
            <div className="bg-white border border-slate-200 p-4 rounded-xl">
              <span className="text-xs font-semibold text-slate-400 uppercase">Classified Windows</span>
              <p className="text-2xl font-black text-slate-900 mt-1">{prediction.summary.total_windows}</p>
              <p className="text-[11px] text-slate-500 mt-0.5">128 samples per window</p>
            </div>

            <div className="bg-white border border-slate-200 p-4 rounded-xl">
              <span className="text-xs font-semibold text-slate-400 uppercase">Mean Confidence</span>
              <p className="text-2xl font-black text-teal-700 mt-1">
                {(prediction.summary.average_confidence * 100).toFixed(1)}%
              </p>
              <p className="text-[11px] text-slate-500 mt-0.5">Model softmax confidence</p>
            </div>

            <div className="bg-white border border-slate-200 p-4 rounded-xl">
              <span className="text-xs font-semibold text-slate-400 uppercase">Uncertain Samples</span>
              <p className="text-2xl font-black text-amber-600 mt-1">{prediction.summary.uncertain_count}</p>
              <p className="text-[11px] text-slate-500 mt-0.5">&lt; 55% confidence threshold</p>
            </div>

            <div className="bg-white border border-slate-200 p-4 rounded-xl">
              <span className="text-xs font-semibold text-slate-400 uppercase">Inference Time</span>
              <p className="text-2xl font-black text-slate-900 mt-1">{prediction.inference_time_ms.toFixed(1)} ms</p>
              <p className="text-[11px] text-slate-500 mt-0.5">Total execution latency</p>
            </div>

            <div className="bg-white border border-slate-200 p-4 rounded-xl">
              <span className="text-xs font-semibold text-slate-400 uppercase">Ground Truth Match</span>
              <p className="text-2xl font-black text-slate-900 mt-1">
                {prediction.summary.ground_truth_accuracy !== null && prediction.summary.ground_truth_accuracy !== undefined
                  ? `${(prediction.summary.ground_truth_accuracy * 100).toFixed(1)}%`
                  : 'N/A'}
              </p>
              <p className="text-[11px] text-slate-500 mt-0.5">
                {prediction.summary.ground_truth_accuracy !== null ? 'Evaluated vs labels' : 'Unlabeled input'}
              </p>
            </div>
          </div>

          {/* Activity Breakdown & Donut */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs flex flex-col items-center justify-center">
              <h3 className="text-sm font-bold text-slate-900 mb-2 text-center w-full">Activity Class Distribution</h3>
              <div className="w-56 h-56">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={pieData}
                      dataKey="value"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      innerRadius={55}
                      outerRadius={80}
                      paddingAngle={3}
                    >
                      {pieData.map((entry, idx) => (
                        <Cell key={`cell-${idx}`} fill={entry.color} />
                      ))}
                    </Pie>
                    <Tooltip formatter={(v: any) => [`${v} windows`, 'Count']} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Distribution Table */}
            <div className="lg:col-span-2 bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-bold text-slate-900">Activity Breakdown</h3>
                <button
                  onClick={exportJSON}
                  className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-xs font-semibold text-slate-700 transition-all cursor-pointer"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>Export JSON</span>
                </button>
              </div>

              <div className="space-y-2.5">
                {Object.entries(prediction.summary.activity_percentages).map(([act, pct]) => {
                  const count = prediction.summary.activity_counts[act] || 0;
                  const color = COLOR_MAP[act] || '#94a3b8';
                  return (
                    <div key={act} className="text-xs">
                      <div className="flex justify-between font-semibold text-slate-700 mb-1">
                        <span>{act}</span>
                        <span>{pct}% ({count} windows)</span>
                      </div>
                      <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                        <div
                          className="h-full rounded-full transition-all duration-500"
                          style={{ width: `${pct}%`, backgroundColor: color }}
                        />
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Activity Timeline */}
          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
            <h3 className="text-base font-bold text-slate-900 mb-1">Sequential Activity Timeline</h3>
            <p className="text-xs text-slate-500 mb-4">
              Temporal window sequence ($2.56$ seconds per window with 50% overlap).
            </p>

            <div className="overflow-x-auto max-h-96">
              <table className="min-w-full text-xs text-left">
                <thead className="bg-slate-50 text-slate-500 uppercase font-semibold sticky top-0 border-b border-slate-200">
                  <tr>
                    <th className="py-2.5 px-3">Window #</th>
                    <th className="py-2.5 px-3">Predicted Activity</th>
                    <th className="py-2.5 px-3">Confidence</th>
                    <th className="py-2.5 px-3">Status</th>
                    {prediction.timeline[0]?.ground_truth && (
                      <th className="py-2.5 px-3">Ground Truth</th>
                    )}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 text-slate-700">
                  {prediction.timeline.map((win) => {
                    const color = COLOR_MAP[win.predicted_activity] || '#94a3b8';
                    return (
                      <tr key={win.window_index} className="hover:bg-slate-50/80">
                        <td className="py-2 px-3 font-mono font-medium text-slate-900">
                          #{win.window_index}
                        </td>
                        <td className="py-2 px-3">
                          <span
                            className="inline-block px-2.5 py-0.5 rounded-full text-white font-semibold text-[10px]"
                            style={{ backgroundColor: color }}
                          >
                            {win.predicted_activity}
                          </span>
                        </td>
                        <td className="py-2 px-3 font-mono font-semibold">
                          {(win.confidence * 100).toFixed(1)}%
                        </td>
                        <td className="py-2 px-3">
                          {win.is_uncertain ? (
                            <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800">
                              Uncertain
                            </span>
                          ) : (
                            <span className="text-[10px] font-semibold text-emerald-700">Confident</span>
                          )}
                        </td>
                        {win.ground_truth && (
                          <td className="py-2 px-3 font-medium">
                            <span className={win.ground_truth.toUpperCase() === win.predicted_activity.toUpperCase() ? 'text-emerald-700' : 'text-rose-600 font-bold'}>
                              {win.ground_truth}
                            </span>
                          </td>
                        )}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
