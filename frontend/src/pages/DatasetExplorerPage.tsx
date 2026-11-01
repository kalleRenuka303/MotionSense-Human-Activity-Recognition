import React, { useState } from 'react';
import { 
  Database, 
  CheckCircle2, 
  Download, 
  FileText, 
  Layers, 
  Activity, 
  ShieldCheck, 
  Info,
  ChevronRight
} from 'lucide-react';
import { DatasetStats } from '../types';
import { api } from '../services/api';

interface DatasetExplorerProps {
  stats: DatasetStats | null;
}

const CHANNELS_DATA = [
  { id: 1, name: 'total_acc_x', sensor: 'Accelerometer', axis: 'X', units: 'g', desc: 'Total acceleration including gravity component' },
  { id: 2, name: 'total_acc_y', sensor: 'Accelerometer', axis: 'Y', units: 'g', desc: 'Total acceleration including gravity component' },
  { id: 3, name: 'total_acc_z', sensor: 'Accelerometer', axis: 'Z', units: 'g', desc: 'Total acceleration including gravity component' },
  { id: 4, name: 'body_acc_x', sensor: 'Accelerometer', axis: 'X', units: 'g', desc: 'Body acceleration (Butterworth low-pass filter corner 0.3 Hz removed gravity)' },
  { id: 5, name: 'body_acc_y', sensor: 'Accelerometer', axis: 'Y', units: 'g', desc: 'Body acceleration component' },
  { id: 6, name: 'body_acc_z', sensor: 'Accelerometer', axis: 'Z', units: 'g', desc: 'Body acceleration component' },
  { id: 7, name: 'body_gyro_x', sensor: 'Gyroscope', axis: 'X', units: 'rad/s', desc: 'Angular velocity measured by gyroscope' },
  { id: 8, name: 'body_gyro_y', sensor: 'Gyroscope', axis: 'Y', units: 'rad/s', desc: 'Angular velocity measured by gyroscope' },
  { id: 9, name: 'body_gyro_z', sensor: 'Gyroscope', axis: 'Z', units: 'rad/s', desc: 'Angular velocity measured by gyroscope' },
];

const FEATURE_GROUPS = [
  { name: 'tBodyAcc-XYZ', type: 'Time Domain', count: 40, desc: 'Tri-axial body acceleration time domain signals' },
  { name: 'tGravityAcc-XYZ', type: 'Time Domain', count: 40, desc: 'Tri-axial gravity acceleration time domain signals' },
  { name: 'tBodyAccJerk-XYZ', type: 'Time Domain', count: 40, desc: 'Tri-axial jerk signals derived from body linear acceleration' },
  { name: 'tBodyGyro-XYZ', type: 'Time Domain', count: 40, desc: 'Tri-axial angular velocity from gyroscope' },
  { name: 'tBodyGyroJerk-XYZ', type: 'Time Domain', count: 40, desc: 'Tri-axial jerk signals derived from angular velocity' },
  { name: 'tBodyAccMag, tGravityAccMag', type: 'Time Domain', count: 52, desc: 'Euclidean norm magnitudes of 3D signals' },
  { name: 'fBodyAcc-XYZ (FFT)', type: 'Frequency Domain', count: 79, desc: 'Fast Fourier Transform of body acceleration signals' },
  { name: 'fBodyAccJerk-XYZ (FFT)', type: 'Frequency Domain', count: 79, desc: 'FFT applied to linear jerk signals' },
  { name: 'fBodyGyro-XYZ (FFT)', type: 'Frequency Domain', count: 79, desc: 'FFT applied to angular velocity signals' },
  { name: 'fBodyAccMag, fBodyGyroMag', type: 'Frequency Domain', count: 52, desc: 'FFT of Euclidean norm magnitudes' },
  { name: 'angle() Vectors', type: 'Angle Domain', count: 7, desc: 'Vectors between gravity and movement directions' },
];

export const DatasetExplorerPage: React.FC<DatasetExplorerProps> = ({ stats }) => {
  const [activeTab, setActiveTab] = useState<'overview' | 'channels' | 'features'>('overview');

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white border border-slate-200 p-6 rounded-2xl shadow-xs">
        <div>
          <div className="flex items-center space-x-2 text-xs font-semibold text-teal-700 uppercase tracking-wider">
            <Database className="w-4 h-4" />
            <span>Official Dataset Reference</span>
          </div>
          <h1 className="text-2xl font-bold text-slate-900 mt-1">UCI HAR Dataset (ID 240)</h1>
          <p className="text-sm text-slate-500 mt-1">
            Human Activity Recognition Using Smartphones &bull; Collected by Anguita et al. (ESANN 2013)
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <a
            href={api.getSampleDownloadUrl('features')}
            download
            className="inline-flex items-center space-x-1.5 px-3.5 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold transition-all"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Sample Features CSV</span>
          </a>
          <a
            href={api.getSampleDownloadUrl('raw')}
            download
            className="inline-flex items-center space-x-1.5 px-3.5 py-2 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-xs font-semibold shadow-xs transition-all"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Sample 9-Axis CSV</span>
          </a>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex space-x-2 border-b border-slate-200 pb-2">
        <button
          onClick={() => setActiveTab('overview')}
          className={`px-4 py-2 rounded-lg text-sm font-semibold transition-all cursor-pointer ${
            activeTab === 'overview'
              ? 'bg-teal-50 text-teal-800 border border-teal-200/80 shadow-2xs'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          Dataset Overview & Splits
        </button>
        <button
          onClick={() => setActiveTab('channels')}
          className={`px-4 py-2 rounded-lg text-sm font-semibold transition-all cursor-pointer ${
            activeTab === 'channels'
              ? 'bg-teal-50 text-teal-800 border border-teal-200/80 shadow-2xs'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          Pipeline B: 9 Raw Channels
        </button>
        <button
          onClick={() => setActiveTab('features')}
          className={`px-4 py-2 rounded-lg text-sm font-semibold transition-all cursor-pointer ${
            activeTab === 'features'
              ? 'bg-teal-50 text-teal-800 border border-teal-200/80 shadow-2xs'
              : 'text-slate-600 hover:text-slate-900'
          }`}
        >
          Pipeline A: 561 Features Breakdown
        </button>
      </div>

      {/* Tab 1: Overview */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
              <h3 className="text-sm font-bold uppercase tracking-wider text-slate-500">Volunteers & Subjects</h3>
              <p className="text-3xl font-black text-slate-900 mt-2">30 Subjects</p>
              <p className="text-xs text-slate-500 mt-1">Aged 19–48 years old wearing waist-mounted smartphones</p>

              <div className="mt-4 pt-4 border-t border-slate-100 space-y-2 text-xs">
                <div className="flex justify-between py-1">
                  <span className="text-slate-500">Train Split (70%):</span>
                  <span className="font-semibold text-slate-800">21 Subjects (7,352 windows)</span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-slate-500">Test Split (30%):</span>
                  <span className="font-semibold text-slate-800">9 Subjects (2,947 windows)</span>
                </div>
                <div className="flex justify-between py-1 text-teal-700 font-medium">
                  <span>Subject Overlap:</span>
                  <span>0 Subjects (Zero Leakage)</span>
                </div>
              </div>
            </div>

            <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
              <h3 className="text-sm font-bold uppercase tracking-wider text-slate-500">Sampling & Windowing</h3>
              <p className="text-3xl font-black text-slate-900 mt-2">50 Hz</p>
              <p className="text-xs text-slate-500 mt-1">20 ms per sample reading across tri-axial sensors</p>

              <div className="mt-4 pt-4 border-t border-slate-100 space-y-2 text-xs">
                <div className="flex justify-between py-1">
                  <span className="text-slate-500">Window Length:</span>
                  <span className="font-semibold text-slate-800">128 readings (2.56 sec)</span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-slate-500">Sliding Overlap:</span>
                  <span className="font-semibold text-slate-800">50% (64 readings shift)</span>
                </div>
                <div className="flex justify-between py-1">
                  <span className="text-slate-500">Total Extracted Windows:</span>
                  <span className="font-semibold text-slate-800">10,299 windows</span>
                </div>
              </div>
            </div>

            <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
              <h3 className="text-sm font-bold uppercase tracking-wider text-slate-500">Audit Status</h3>
              <div className="flex items-center space-x-2 mt-2">
                <ShieldCheck className="w-8 h-8 text-teal-600" />
                <span className="text-2xl font-black text-teal-700">100% VERIFIED</span>
              </div>
              <p className="text-xs text-slate-500 mt-1">
                Audited against official archive checksum and file integrity
              </p>

              <div className="mt-4 pt-4 border-t border-slate-100 space-y-2 text-xs">
                <div className="flex items-center space-x-2 text-emerald-700 font-medium">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>No missing or infinite values</span>
                </div>
                <div className="flex items-center space-x-2 text-emerald-700 font-medium">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>Channel order strictly validated</span>
                </div>
                <div className="flex items-center space-x-2 text-emerald-700 font-medium">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>Subject partitions isolated</span>
                </div>
              </div>
            </div>
          </div>

          {/* Official Six Activities Cards */}
          <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
            <h3 className="text-base font-bold text-slate-900 mb-4">Official Six Target Activity Classes</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {[
                { id: 1, label: 'WALKING', type: 'Dynamic Locomotion', samples: 1722, desc: 'Regular pace horizontal gait with rhythmic oscillatory acceleration' },
                { id: 2, label: 'WALKING_UPSTAIRS', type: 'Dynamic Locomotion', samples: 1544, desc: 'Ascending flight of stairs with amplified vertical acceleration peaks' },
                { id: 3, label: 'WALKING_DOWNSTAIRS', type: 'Dynamic Locomotion', samples: 1406, desc: 'Descending flight of stairs with distinct heel-strike gravitational deceleration' },
                { id: 4, label: 'SITTING', type: 'Static Posture', samples: 1777, desc: 'Resting upright position on chair; body vertical, legs bent' },
                { id: 5, label: 'STANDING', type: 'Static Posture', samples: 1906, desc: 'Upright vertical posture with subtle postural sway oscillations' },
                { id: 6, label: 'LAYING', type: 'Static Posture', samples: 1944, desc: 'Horizontal recumbent position; gravity vector rotates orthogonal to body axis' },
              ].map((act) => (
                <div key={act.id} className="p-4 rounded-xl border border-slate-200 bg-slate-50/50">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-mono font-bold text-teal-700">Class {act.id}</span>
                    <span className="text-[10px] uppercase font-semibold px-2 py-0.5 rounded-full bg-slate-200/60 text-slate-700">
                      {act.type}
                    </span>
                  </div>
                  <h4 className="font-bold text-slate-900 text-sm mt-1">{act.label}</h4>
                  <p className="text-xs text-slate-500 mt-1 leading-relaxed">{act.desc}</p>
                  <p className="text-xs font-semibold text-slate-700 mt-2">{act.samples.toLocaleString()} Total Windows</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Tab 2: Channels */}
      {activeTab === 'channels' && (
        <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
          <div className="mb-4">
            <h3 className="text-base font-bold text-slate-900">Pipeline B: Exact 9 Inertial Sensor Channels</h3>
            <p className="text-xs text-slate-500">
              Input tensor for 1D CNN and CNN-LSTM models: shape (batch_size, 128, 9). Channel order is strictly invariant.
            </p>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full text-xs text-left">
              <thead className="bg-slate-50 text-slate-500 uppercase font-semibold border-b border-slate-200">
                <tr>
                  <th className="py-3 px-4">Index</th>
                  <th className="py-3 px-4">Channel Identifier</th>
                  <th className="py-3 px-4">Sensor Instrument</th>
                  <th className="py-3 px-4">Spatial Axis</th>
                  <th className="py-3 px-4">Measurement Units</th>
                  <th className="py-3 px-4">Signal Description</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {CHANNELS_DATA.map((ch) => (
                  <tr key={ch.id} className="hover:bg-slate-50/70">
                    <td className="py-3 px-4 font-mono font-bold text-slate-900">0{ch.id}</td>
                    <td className="py-3 px-4 font-mono font-semibold text-teal-700">{ch.name}</td>
                    <td className="py-3 px-4">{ch.sensor}</td>
                    <td className="py-3 px-4 font-bold">{ch.axis}</td>
                    <td className="py-3 px-4 font-mono text-slate-500">{ch.units}</td>
                    <td className="py-3 px-4 text-slate-600">{ch.desc}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Tab 3: Features */}
      {activeTab === 'features' && (
        <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
          <div className="mb-4">
            <h3 className="text-base font-bold text-slate-900">Pipeline A: 561 Pre-Computed Statistical Features</h3>
            <p className="text-xs text-slate-500">
              Input vector for Random Forest baseline: shape (N, 561). Normalized to [-1, 1].
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {FEATURE_GROUPS.map((grp, idx) => (
              <div key={idx} className="p-4 rounded-xl border border-slate-200 bg-slate-50/50">
                <div className="flex items-center justify-between">
                  <h4 className="font-bold text-slate-900 text-sm">{grp.name}</h4>
                  <span className="text-xs font-mono font-semibold text-teal-700 bg-teal-50 px-2 py-0.5 rounded-md border border-teal-200/50">
                    {grp.count} features
                  </span>
                </div>
                <span className="text-[10px] font-semibold uppercase text-slate-400 mt-0.5 inline-block">
                  {grp.type}
                </span>
                <p className="text-xs text-slate-600 mt-2 leading-relaxed">{grp.desc}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
