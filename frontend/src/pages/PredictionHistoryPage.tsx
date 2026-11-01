import React, { useState } from 'react';
import { 
  History, 
  Trash2, 
  ExternalLink, 
  FileText, 
  CheckCircle2, 
  AlertCircle,
  Download,
  Search,
  Filter
} from 'lucide-react';
import { HistoryItem } from '../types';
import { api } from '../services/api';

interface PredictionHistoryProps {
  history: HistoryItem[];
  onRefresh: () => void;
}

export const PredictionHistoryPage: React.FC<PredictionHistoryProps> = ({ history, onRefresh }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedRecord, setSelectedRecord] = useState<HistoryItem | null>(null);

  const handleDelete = async (id: number) => {
    if (confirm(`Delete prediction record #${id}?`)) {
      try {
        await api.deleteHistory(id);
        if (selectedRecord?.id === id) setSelectedRecord(null);
        onRefresh();
      } catch (err: any) {
        alert(err.message || 'Failed to delete record');
      }
    }
  };

  const filtered = history.filter(item => 
    item.filename.toLowerCase().includes(searchTerm.toLowerCase()) ||
    item.selected_model.toLowerCase().includes(searchTerm.toLowerCase()) ||
    String(item.id).includes(searchTerm)
  );

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Header */}
      <div className="bg-white border border-slate-200 p-6 rounded-2xl shadow-xs flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2 text-xs font-semibold text-teal-700 uppercase tracking-wider">
            <History className="w-4 h-4" />
            <span>SQLite Database Persistence</span>
          </div>
          <h1 className="text-2xl font-bold text-slate-900 mt-1">Prediction History</h1>
          <p className="text-sm text-slate-500 mt-1">
            Browse and inspect past classification runs and exported timelines.
          </p>
        </div>

        {/* Search */}
        <div className="relative w-full sm:w-64">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            placeholder="Search by filename or model..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-4 py-2 rounded-xl border border-slate-200 text-xs bg-slate-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-teal-500/20 focus:border-teal-500"
          />
        </div>
      </div>

      {/* Main Table */}
      <div className="bg-white border border-slate-200 rounded-2xl p-6 shadow-2xs">
        {filtered.length === 0 ? (
          <div className="text-center py-12 text-slate-400">
            <History className="w-12 h-12 mx-auto mb-3 opacity-40" />
            <p className="text-sm font-semibold">No prediction records found</p>
            <p className="text-xs mt-1">Run an inference in Activity Recognition to log predictions.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-xs text-left">
              <thead className="bg-slate-50 text-slate-500 uppercase font-semibold border-b border-slate-200">
                <tr>
                  <th className="py-3 px-3">Run ID</th>
                  <th className="py-3 px-3">File Name</th>
                  <th className="py-3 px-3">Pipeline / Schema</th>
                  <th className="py-3 px-3">Selected Model</th>
                  <th className="py-3 px-3">Windows</th>
                  <th className="py-3 px-3">Mean Confidence</th>
                  <th className="py-3 px-3">Timestamp</th>
                  <th className="py-3 px-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-slate-700">
                {filtered.map((item) => (
                  <tr key={item.id} className="hover:bg-slate-50/80 transition-colors">
                    <td className="py-3 px-3 font-mono font-bold text-slate-900">#{item.id}</td>
                    <td className="py-3 px-3 font-medium text-slate-900 max-w-[150px] truncate" title={item.filename}>
                      {item.filename}
                    </td>
                    <td className="py-3 px-3">
                      <span className="text-[10px] uppercase font-semibold px-2 py-0.5 rounded-md bg-slate-100 text-slate-600">
                        {item.input_format}
                      </span>
                    </td>
                    <td className="py-3 px-3 font-semibold text-teal-800">{item.selected_model}</td>
                    <td className="py-3 px-3 font-mono">{item.num_windows}</td>
                    <td className="py-3 px-3 font-mono font-bold text-teal-700">
                      {(item.avg_confidence * 100).toFixed(1)}%
                    </td>
                    <td className="py-3 px-3 text-slate-400">
                      {new Date(item.created_at).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' })}
                    </td>
                    <td className="py-3 px-3 text-right space-x-2">
                      <button
                        onClick={() => setSelectedRecord(item)}
                        className="p-1.5 rounded-lg text-slate-500 hover:text-teal-700 hover:bg-slate-100 transition-colors cursor-pointer"
                        title="View Details"
                      >
                        <ExternalLink className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleDelete(item.id)}
                        className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
                        title="Delete Record"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal / Slide-out Details */}
      {selectedRecord && (
        <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-xl border border-slate-200 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-slate-100 pb-4">
              <div>
                <h3 className="text-lg font-bold text-slate-900">Prediction Record #{selectedRecord.id}</h3>
                <p className="text-xs text-slate-500">{selectedRecord.filename} &bull; {selectedRecord.selected_model}</p>
              </div>
              <button
                onClick={() => setSelectedRecord(null)}
                className="text-slate-400 hover:text-slate-700 text-sm font-bold cursor-pointer"
              >
                &times; Close
              </button>
            </div>

            <div className="mt-4 space-y-4">
              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="p-3 bg-slate-50 rounded-xl">
                  <span className="text-slate-400 uppercase font-semibold">Total Windows</span>
                  <p className="text-lg font-bold text-slate-900 mt-0.5">{selectedRecord.num_windows}</p>
                </div>
                <div className="p-3 bg-slate-50 rounded-xl">
                  <span className="text-slate-400 uppercase font-semibold">Mean Confidence</span>
                  <p className="text-lg font-bold text-teal-700 mt-0.5">
                    {(selectedRecord.avg_confidence * 100).toFixed(1)}%
                  </p>
                </div>
              </div>

              <div>
                <h4 className="text-xs font-bold text-slate-700 uppercase mb-2">Activity Breakdown</h4>
                <div className="space-y-1.5">
                  {Object.entries(selectedRecord.activity_distribution).map(([act, count]) => (
                    <div key={act} className="flex justify-between text-xs py-1 px-2.5 rounded-lg bg-slate-50">
                      <span className="font-medium text-slate-700">{act}</span>
                      <span className="font-mono font-bold text-slate-900">{count} windows</span>
                    </div>
                  ))}
                </div>
              </div>

              {selectedRecord.timeline_summary && (
                <div>
                  <h4 className="text-xs font-bold text-slate-700 uppercase mb-2">Sample Window Timeline</h4>
                  <div className="max-h-48 overflow-y-auto border border-slate-100 rounded-xl">
                    <table className="min-w-full text-xs text-left">
                      <thead className="bg-slate-50 text-slate-500 sticky top-0">
                        <tr>
                          <th className="py-1.5 px-3">#</th>
                          <th className="py-1.5 px-3">Activity</th>
                          <th className="py-1.5 px-3">Confidence</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 font-mono">
                        {selectedRecord.timeline_summary.slice(0, 20).map((w: any) => (
                          <tr key={w.window_index}>
                            <td className="py-1 px-3 text-slate-400">#{w.window_index}</td>
                            <td className="py-1 px-3 font-semibold text-slate-800">{w.predicted_activity}</td>
                            <td className="py-1 px-3 font-bold text-teal-700">{(w.confidence * 100).toFixed(1)}%</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
