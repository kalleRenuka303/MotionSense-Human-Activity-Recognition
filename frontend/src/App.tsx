import React, { useState, useEffect } from 'react';
import { Navbar } from './components/Navbar';
import { OverviewPage } from './pages/OverviewPage';
import { DatasetExplorerPage } from './pages/DatasetExplorerPage';
import { ActivityRecognitionPage } from './pages/ActivityRecognitionPage';
import { ModelComparisonPage } from './pages/ModelComparisonPage';
import { EvaluationPage } from './pages/EvaluationPage';
import { PredictionHistoryPage } from './pages/PredictionHistoryPage';
import { ModelInfoPage } from './pages/ModelInfoPage';
import { SettingsPage } from './pages/SettingsPage';
import { api } from './services/api';
import { DatasetStats, ModelInfo, HistoryItem } from './types';
import { ExternalLink, ShieldCheck } from 'lucide-react';

export const App: React.FC = () => {
  const [currentTab, setCurrentTab] = useState<string>('overview');
  const [systemStatus, setSystemStatus] = useState<string>('checking...');
  const [datasetStats, setDatasetStats] = useState<DatasetStats | null>(null);
  const [models, setModels] = useState<ModelInfo[]>([]);
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [evaluationSummary, setEvaluationSummary] = useState<any>(null);

  const fetchInitialData = async () => {
    try {
      const health = await api.getHealth();
      setSystemStatus(health.status);
    } catch {
      setSystemStatus('offline');
    }

    try {
      const stats = await api.getDatasetInfo();
      setDatasetStats(stats);
    } catch (e) {
      console.warn('Dataset info pending:', e);
    }

    try {
      const mList = await api.getModels();
      setModels(mList);
    } catch (e) {
      console.warn('Models info pending:', e);
    }

    try {
      const hist = await api.getHistory();
      setHistory(hist.items || []);
    } catch (e) {
      console.warn('History pending:', e);
    }

    try {
      const evalSum = await api.getEvaluationSummary();
      setEvaluationSummary(evalSum);
    } catch (e) {
      console.warn('Evaluation pending:', e);
    }
  };

  useEffect(() => {
    fetchInitialData();
  }, []);

  return (
    <div className="min-h-screen bg-[#f8fafc] text-slate-800 flex flex-col font-sans">
      <Navbar
        currentTab={currentTab}
        setCurrentTab={setCurrentTab}
        systemStatus={systemStatus}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {currentTab === 'overview' && (
          <OverviewPage
            stats={datasetStats}
            models={models}
            history={history}
            onNavigate={setCurrentTab}
          />
        )}
        {currentTab === 'dataset' && (
          <DatasetExplorerPage stats={datasetStats} />
        )}
        {currentTab === 'recognition' && (
          <ActivityRecognitionPage
            models={models}
            onPredictionComplete={fetchInitialData}
          />
        )}
        {currentTab === 'comparison' && (
          <ModelComparisonPage models={models} />
        )}
        {currentTab === 'evaluation' && (
          <EvaluationPage
            models={models}
            evaluationSummary={evaluationSummary}
          />
        )}
        {currentTab === 'history' && (
          <PredictionHistoryPage
            history={history}
            onRefresh={fetchInitialData}
          />
        )}
        {currentTab === 'models' && <ModelInfoPage />}
        {currentTab === 'settings' && <SettingsPage />}
      </main>

      {/* Academic Footer */}
      <footer className="bg-white border-t border-slate-200 mt-16 py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row items-center justify-between text-xs text-slate-500 gap-4">
          <div className="flex items-center space-x-2">
            <span className="font-bold text-slate-800">MotionSense HAR</span>
            <span>&bull;</span>
            <span>UCI Machine Learning Repository Dataset ID 240</span>
          </div>

          <div className="flex items-center space-x-4">
            <a
              href="https://archive.ics.uci.edu/dataset/240/human+activity+recognition+using+smartphones"
              target="_blank"
              rel="noreferrer"
              className="hover:text-teal-700 flex items-center space-x-1"
            >
              <span>UCI HAR Archive</span>
              <ExternalLink className="w-3 h-3" />
            </a>
            <a
              href="https://github.com/leelaprasad6629/human-activity-recognition"
              target="_blank"
              rel="noreferrer"
              className="hover:text-teal-700 flex items-center space-x-1"
            >
              <span>GitHub Repository</span>
              <ExternalLink className="w-3 h-3" />
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
};
