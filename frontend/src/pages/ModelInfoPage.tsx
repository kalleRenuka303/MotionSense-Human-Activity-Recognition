import React from 'react';
import { 
  Layers, 
  Cpu, 
  Activity, 
  ShieldAlert, 
  HelpCircle, 
  CheckCircle2, 
  Code,
  Zap,
  Sparkles
} from 'lucide-react';

export const ModelInfoPage: React.FC = () => {
  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Header */}
      <div className="bg-white border border-slate-200 p-6 rounded-2xl shadow-xs">
        <div className="flex items-center space-x-2 text-xs font-semibold text-teal-700 uppercase tracking-wider">
          <Layers className="w-4 h-4" />
          <span>Technical Architecture</span>
        </div>
        <h1 className="text-2xl font-bold text-slate-900 mt-1">Deep Learning & ML Model Specifications</h1>
        <p className="text-sm text-slate-500 mt-1">
          Detailed mathematical formulation, tensor shapes, and architectural design of the MotionSense classifiers.
        </p>
      </div>

      {/* Model 3: CNN-LSTM (Primary) */}
      <div className="bg-white border-2 border-teal-500/80 rounded-2xl p-6 sm:p-8 shadow-xs relative overflow-hidden">
        <div className="absolute top-0 right-0 bg-teal-600 text-white text-[10px] font-bold uppercase tracking-wider px-4 py-1 rounded-bl-xl">
          Primary Proposed Research Model
        </div>

        <div className="flex items-center space-x-3 mb-4">
          <div className="p-3 bg-teal-50 text-teal-600 rounded-xl">
            <Sparkles className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-slate-900">CNN-LSTM Spatio-Temporal Network</h2>
            <p className="text-xs text-slate-500">Pipeline B &bull; 9 Tri-Axial Inertial Channels &bull; PyTorch Implementation</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-xs text-slate-600 leading-relaxed">
          <div>
            <h3 className="font-bold text-slate-900 text-sm mb-2">1. Convolutional Spatial Feature Extraction</h3>
            <p>
              The input tensor is shaped <code className="bg-slate-100 px-1.5 py-0.5 rounded text-teal-800 font-mono">(batch_size, 128, 9)</code>, permuted to <code className="bg-slate-100 px-1.5 py-0.5 rounded text-teal-800 font-mono">(B, 9, 128)</code> for 1D convolutions across the time dimension:
            </p>
            <ul className="mt-2 space-y-1.5 list-disc list-inside">
              <li><strong>Conv1D Layer 1</strong>: 9 in-channels &rarr; 64 filters, kernel size 5, stride 1, padding 2.</li>
              <li><strong>Batch Normalization & ReLU</strong>: Accelerates training convergence and standardizes activation distributions.</li>
              <li><strong>Conv1D Layer 2</strong>: 64 in-channels &rarr; 64 filters, kernel size 5, padding 2.</li>
              <li><strong>MaxPool1D (stride 2)</strong>: Halves temporal dimension from 128 to 64 steps, retaining dominant local kinetic peaks.</li>
            </ul>
          </div>

          <div>
            <h3 className="font-bold text-slate-900 text-sm mb-2">2. Recurrent Temporal Transition Modeling (LSTM)</h3>
            <p>
              Feature maps are transposed to <code className="bg-slate-100 px-1.5 py-0.5 rounded text-teal-800 font-mono">(B, 64, 64)</code> and processed through a 2-layer stacked Long Short-Term Memory (LSTM) network:
            </p>
            <ul className="mt-2 space-y-1.5 list-disc list-inside">
              <li><strong>LSTM Parameters</strong>: <code className="font-mono">input_size=64, hidden_size=128, num_layers=2, dropout=0.35</code>.</li>
              <li><strong>Hidden State Extraction</strong>: The final hidden state vector <code className="font-mono">h_T</code> (128 dimensions) encodes the entire sequence dynamics.</li>
              <li><strong>Dense Classifier</strong>: Linear(128 &rarr; 64) &rarr; ReLU &rarr; Dropout(0.35) &rarr; Linear(64 &rarr; 6 classes).</li>
              <li><strong>Trainable Parameters</strong>: 263,814 parameters.</li>
            </ul>
          </div>
        </div>
      </div>

      {/* Model 2: 1D CNN */}
      <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
        <div className="flex items-center space-x-3 mb-4">
          <div className="p-3 bg-sky-50 text-sky-600 rounded-xl">
            <Cpu className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-slate-900">1D Convolutional Neural Network (CNN)</h2>
            <p className="text-xs text-slate-500">Pipeline B &bull; Hierarchical Multi-Scale Feature Extraction</p>
          </div>
        </div>

        <p className="text-xs text-slate-600 leading-relaxed mb-4">
          A pure convolutional time-series model consisting of three hierarchical 1D convolutional blocks with Batch Normalization, Max Pooling, and Global Average Pooling (GAP) before the dense classification head.
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
          <div className="p-3 bg-slate-50 rounded-xl border border-slate-100">
            <span className="font-bold text-slate-900">Block 1: Conv1D (9 &rarr; 64)</span>
            <p className="text-slate-500 mt-0.5">Kernel 5, BN, ReLU, MaxPool2, Dropout(0.2)</p>
          </div>
          <div className="p-3 bg-slate-50 rounded-xl border border-slate-100">
            <span className="font-bold text-slate-900">Block 2: Conv1D (64 &rarr; 128)</span>
            <p className="text-slate-500 mt-0.5">Kernel 5, BN, ReLU, MaxPool2, Dropout(0.25)</p>
          </div>
          <div className="p-3 bg-slate-50 rounded-xl border border-slate-100">
            <span className="font-bold text-slate-900">Block 3: Conv1D (128 &rarr; 128)</span>
            <p className="text-slate-500 mt-0.5">Kernel 3, BN, ReLU, Global Avg Pooling</p>
          </div>
        </div>
      </div>

      {/* Model 1: Random Forest Baseline */}
      <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
        <div className="flex items-center space-x-3 mb-4">
          <div className="p-3 bg-slate-100 text-slate-700 rounded-xl">
            <Activity className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-slate-900">Random Forest Classifier Baseline</h2>
            <p className="text-xs text-slate-500">Pipeline A &bull; 561 Statistical Variables &bull; Scikit-learn</p>
          </div>
        </div>

        <p className="text-xs text-slate-600 leading-relaxed">
          Ensemble of 150 decision trees trained with Gini impurity splitting criteria. Uses 561 pre-computed time-domain and frequency-domain features (means, standard deviations, signal magnitude areas, energy bands, autoregression coefficients). Provides direct feature importance attribution.
        </p>
      </div>

      {/* Scientific Limitations Banner */}
      <div className="bg-amber-50/60 border border-amber-200/80 rounded-2xl p-6 shadow-2xs">
        <div className="flex items-start space-x-3">
          <ShieldAlert className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
          <div>
            <h3 className="text-sm font-bold text-amber-900">Research Scope & Scientific Limitations</h3>
            <ul className="mt-2 space-y-1.5 text-xs text-amber-800 list-disc list-inside">
              <li><strong>Waist-mounted Phone Placement</strong>: UCI HAR data was recorded with a Samsung Galaxy S II mounted at the waist. Pocket, arm, or handheld orientations will encounter coordinate system rotations.</li>
              <li><strong>Sampling Frequency</strong>: Models are calibrated for 50 Hz inertial streams (20 ms interval). Sensors operating at different rates require resampling or window interpolation.</li>
              <li><strong>Non-Medical Scope</strong>: MotionSense is an experimental research platform and does not constitute medical diagnostic equipment.</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};
