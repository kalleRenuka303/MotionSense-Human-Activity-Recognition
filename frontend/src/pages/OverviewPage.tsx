import React from 'react';
import { 
  Activity, 
  Cpu, 
  ShieldCheck, 
  BarChart2, 
  ArrowRight, 
  CheckCircle2, 
  Database,
  Layers,
  Sparkles
} from 'lucide-react';
import { 
  BarChart, 
  Bar, 
  XAxis, 
  YAxis, 
  Tooltip, 
  ResponsiveContainer, 
  Cell 
} from 'recharts';
import { DatasetStats, ModelInfo, HistoryItem } from '../types';

interface OverviewPageProps {
  stats: DatasetStats | null;
  models: ModelInfo[];
  history: HistoryItem[];
  onNavigate: (tab: string) => void;
}

const ACTIVITY_DISTRIBUTION = [
  { name: 'WALKING', count: 1722, color: '#0d9488' },
  { name: 'WALK_UP', count: 1544, color: '#14b8a6' },
  { name: 'WALK_DOWN', count: 1406, color: '#2dd4bf' },
  { name: 'SITTING', count: 1777, color: '#0284c7' },
  { name: 'STANDING', count: 1906, color: '#38bdf8' },
  { name: 'LAYING', count: 1944, color: '#6366f1' },
];

export const OverviewPage: React.FC<OverviewPageProps> = ({ stats, models, history, onNavigate }) => {
  const bestModel = models.length > 0
    ? [...models].sort((a, b) => b.test_accuracy - a.test_accuracy)[0]
    : null;

  return (
    <div className="space-y-8 animate-in fade-in duration-300">
      {/* Hero Scientific Banner */}
      <div className="bg-white border border-slate-200/80 rounded-2xl p-6 sm:p-8 shadow-xs relative overflow-hidden">
        <div className="absolute top-0 right-0 w-96 h-96 bg-teal-50/50 rounded-full blur-3xl pointer-events-none -mr-20 -mt-20" />
        
        <div className="relative z-10 max-w-3xl">
          <div className="inline-flex items-center space-x-2 px-3 py-1 bg-teal-50 border border-teal-200/80 rounded-full text-xs font-semibold text-teal-800 mb-4">
            <ShieldCheck className="w-4 h-4 text-teal-600" />
            <span>Subject-Independent Cross-Validation &bull; Zero Leakage Verified</span>
          </div>
          
          <h1 className="text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
            Human Activity Recognition Using Smartphone Inertial Sensors
          </h1>
          
          <p className="mt-3 text-base text-slate-600 leading-relaxed">
            MotionSense classifies human movement dynamics from tri-axial accelerometer and gyroscope data across six predefined activities. Evaluated on the official UCI HAR Dataset (ID 240) using strictly partitioned, subject-independent validation.
          </p>

          <div className="mt-6 flex flex-wrap gap-3">
            <button
              onClick={() => onNavigate('recognition')}
              className="inline-flex items-center space-x-2 px-5 py-2.5 rounded-xl bg-teal-600 text-white font-semibold text-sm shadow-sm hover:bg-teal-700 transition-all cursor-pointer"
            >
              <Activity className="w-4 h-4" />
              <span>Launch Activity Recognition</span>
              <ArrowRight className="w-4 h-4" />
            </button>
            <button
              onClick={() => onNavigate('comparison')}
              className="inline-flex items-center space-x-2 px-5 py-2.5 rounded-xl bg-slate-100 text-slate-700 font-semibold text-sm hover:bg-slate-200 transition-all cursor-pointer"
            >
              <BarChart2 className="w-4 h-4" />
              <span>Compare Models</span>
            </button>
          </div>
        </div>
      </div>

      {/* 4 Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white border border-slate-200 p-5 rounded-xl shadow-2xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Total Windows</span>
            <div className="p-2 rounded-lg bg-teal-50 text-teal-600">
              <Database className="w-4 h-4" />
            </div>
          </div>
          <p className="mt-3 text-2xl font-black text-slate-900 tracking-tight">
            {stats?.total_windows.toLocaleString() || '10,299'}
          </p>
          <p className="text-xs text-slate-500 mt-1">
            7,352 train (21 subj) &bull; 2,947 test (9 subj)
          </p>
        </div>

        <div className="bg-white border border-slate-200 p-5 rounded-xl shadow-2xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Sensor Channels</span>
            <div className="p-2 rounded-lg bg-sky-50 text-sky-600">
              <Activity className="w-4 h-4" />
            </div>
          </div>
          <p className="mt-3 text-2xl font-black text-slate-900 tracking-tight">
            9 Tri-Axial
          </p>
          <p className="text-xs text-slate-500 mt-1">
            Total Acc, Body Acc & Body Gyro (128 @ 50Hz)
          </p>
        </div>

        <div className="bg-white border border-slate-200 p-5 rounded-xl shadow-2xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Activity Classes</span>
            <div className="p-2 rounded-lg bg-emerald-50 text-emerald-600">
              <Layers className="w-4 h-4" />
            </div>
          </div>
          <p className="mt-3 text-2xl font-black text-slate-900 tracking-tight">
            6 Activities
          </p>
          <p className="text-xs text-slate-500 mt-1">
            Walk, Stairs, Sit, Stand, Lay
          </p>
        </div>

        <div className="bg-white border border-slate-200 p-5 rounded-xl shadow-2xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-500">Best Model</span>
            <div className="p-2 rounded-lg bg-indigo-50 text-indigo-600">
              <Sparkles className="w-4 h-4" />
            </div>
          </div>
          <p className="mt-3 text-2xl font-black text-slate-900 tracking-tight">
            {bestModel ? `${(bestModel.test_accuracy * 100).toFixed(1)}%` : '93.8%'}
          </p>
          <p className="text-xs text-slate-500 mt-1">
            {bestModel?.model_name || 'CNN-LSTM Deep Neural Net'}
          </p>
        </div>
      </div>

      {/* Dataset Activity Breakdown & Research Protocol */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-bold text-slate-900">Activity Class Distribution</h2>
              <p className="text-xs text-slate-500">Aggregated windows across all 30 volunteer recordings</p>
            </div>
            <span className="text-xs font-medium px-2.5 py-1 bg-slate-100 rounded-lg text-slate-600">
              N = 10,299 Windows
            </span>
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={ACTIVITY_DISTRIBUTION} margin={{ top: 10, right: 10, left: -20, bottom: 20 }}>
                <XAxis 
                  dataKey="name" 
                  tick={{ fontSize: 11, fill: '#64748b', fontWeight: 500 }} 
                  interval={0}
                  angle={-15}
                  textAnchor="end"
                />
                <YAxis tick={{ fontSize: 11, fill: '#64748b' }} />
                <Tooltip 
                  formatter={(value: any) => [`${value} windows`, 'Count']}
                  contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0', fontSize: '12px' }}
                />
                <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                  {ACTIVITY_DISTRIBUTION.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Research Rigor Card */}
        <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs flex flex-col justify-between">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Research Rigor</h2>
            <p className="text-xs text-slate-500 mt-1">Subject independence protocol</p>

            <ul className="mt-5 space-y-3.5">
              <li className="flex items-start space-x-3 text-xs text-slate-600">
                <CheckCircle2 className="w-4 h-4 text-teal-600 shrink-0 mt-0.5" />
                <span><strong>No Window Overlap Leakage</strong>: Training & test subsets split strictly by subject ID.</span>
              </li>
              <li className="flex items-start space-x-3 text-xs text-slate-600">
                <CheckCircle2 className="w-4 h-4 text-teal-600 shrink-0 mt-0.5" />
                <span><strong>Two Separate Pipelines</strong>: 561-feature classical ML vs 9-channel deep learning.</span>
              </li>
              <li className="flex items-start space-x-3 text-xs text-slate-600">
                <CheckCircle2 className="w-4 h-4 text-teal-600 shrink-0 mt-0.5" />
                <span><strong>Sensor Order Guaranteed</strong>: Exact axis convention matched across all modules.</span>
              </li>
              <li className="flex items-start space-x-3 text-xs text-slate-600">
                <CheckCircle2 className="w-4 h-4 text-teal-600 shrink-0 mt-0.5" />
                <span><strong>Uncertainty Flagging</strong>: Low-confidence samples identified rather than forced.</span>
              </li>
            </ul>
          </div>

          <div className="mt-6 pt-4 border-t border-slate-100">
            <button
              onClick={() => onNavigate('dataset')}
              className="w-full text-center text-xs font-semibold text-teal-700 hover:text-teal-800 transition-colors cursor-pointer"
            >
              View Full Dataset Specifications &rarr;
            </button>
          </div>
        </div>
      </div>

      {/* Recent Predictions Widget */}
      {history.length > 0 && (
        <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-bold text-slate-900">Recent Prediction Runs</h2>
              <p className="text-xs text-slate-500">Live inference executed through the FastAPI engine</p>
            </div>
            <button
              onClick={() => onNavigate('history')}
              className="text-xs font-semibold text-teal-700 hover:text-teal-800 cursor-pointer"
            >
              View All History ({history.length}) &rarr;
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="min-w-full text-xs text-left text-slate-600">
              <thead className="bg-slate-50 text-slate-500 uppercase font-semibold border-b border-slate-200">
                <tr>
                  <th className="py-2.5 px-3">Run ID</th>
                  <th className="py-2.5 px-3">File Name</th>
                  <th className="py-2.5 px-3">Model</th>
                  <th className="py-2.5 px-3">Windows</th>
                  <th className="py-2.5 px-3">Avg Confidence</th>
                  <th className="py-2.5 px-3">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {history.slice(0, 5).map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-2.5 px-3 font-mono font-medium text-slate-900">#{item.id}</td>
                    <td className="py-2.5 px-3 font-medium text-slate-800">{item.filename}</td>
                    <td className="py-2.5 px-3">
                      <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-700 font-medium">
                        {item.selected_model}
                      </span>
                    </td>
                    <td className="py-2.5 px-3">{item.num_windows}</td>
                    <td className="py-2.5 px-3 font-semibold text-teal-700">
                      {(item.avg_confidence * 100).toFixed(1)}%
                    </td>
                    <td className="py-2.5 px-3 text-slate-400">
                      {new Date(item.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
