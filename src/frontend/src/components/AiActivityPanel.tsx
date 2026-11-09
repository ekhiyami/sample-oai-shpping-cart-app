import { useEffect, useState } from 'react';
import { AI_ACTIVITY_EVENT, readAiActivity } from '../utils/aiActivity';
import './AiActivityPanel.css';

interface Activity {
  provider: 'azure';
  mockRecommendations: boolean;
  providerName: string;
  modelName: string;
  model: string;
  message: string;
  requests: number;
  lastRequest: null | {
    feature: string;
    status: 'RUNNING' | 'SUCCESS' | 'FAILED';
    durationMs: number | null;
    inputTokens: number | null;
    outputTokens: number | null;
  };
}

export function AiActivityPanel() {
  const [activity, setActivity] = useState<Activity | null>(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState(false);

  useEffect(() => {
    let mounted = true;
    let sequence = 0;
    let timer: ReturnType<typeof setInterval> | undefined;
    const refresh = async () => {
      const request = ++sequence;
      try {
        const data = await readAiActivity();
        if (mounted && request === sequence) {
          setActivity(data);
          setError(false);
        }
      } catch {
        if (mounted && request === sequence) setError(true);
      }
    };
    const changed = (event: Event) => {
      const active = (event as CustomEvent<number>).detail > 0;
      setPending(active);
      clearInterval(timer);
      if (active) timer = setInterval(refresh, 1000);
      void refresh();
    };
    window.addEventListener(AI_ACTIVITY_EVENT, changed);
    void refresh();
    return () => {
      mounted = false;
      clearInterval(timer);
      window.removeEventListener(AI_ACTIVITY_EVENT, changed);
    };
  }, []);

  const last = activity?.lastRequest;
  const running = pending || last?.status === 'RUNNING';
  const result = running ? 'In progress' : last?.status === 'SUCCESS' ? 'Successful'
    : last?.status === 'FAILED' ? 'Failed' : '—';
  const tokens = (value: number | null | undefined) =>
    value == null ? 'Not reported' : value.toLocaleString();

  return (
    <section className={`ai-activity ai-activity--${activity?.provider || 'loading'}`}
      aria-label="AI activity" aria-busy={pending}>
      <div className="ai-activity-heading">
        <span className="ai-activity-eyebrow">AI CONNECTION</span>
        <span className="ai-activity-cloud">{activity ? activity.mockRecommendations ? 'LOCAL DEMO' : 'AZURE' : '…'}</span>
      </div>
      <h2>{activity ? `Powered by ${activity.providerName}` : 'Loading AI connection…'}</h2>
      {activity && <>
        <p className="ai-activity-model">Model: <strong title={activity.model}>{activity.modelName}</strong></p>
        <p className="ai-activity-description">{activity.message}</p>
        <div className="ai-activity-stats" aria-live="polite">
          <div><span>Generation requests this session</span><strong>{activity.requests.toLocaleString()}</strong></div>
          <div><span>Last request</span><strong>{last?.feature || '—'}</strong></div>
          <div><span>Result</span><strong className={`ai-result-${running ? 'running' : last?.status.toLowerCase()}`}>{result}</strong></div>
          <div><span>Response time</span><strong>{running ? 'Waiting…'
            : last?.durationMs != null ? `${(last.durationMs / 1000).toFixed(2)} s` : '—'}</strong></div>
          <div><span>Input tokens · last request</span><strong>{running ? '—' : last ? tokens(last.inputTokens) : '—'}</strong></div>
          <div><span>Output tokens · last request</span><strong>{running ? '—' : last ? tokens(last.outputTokens) : '—'}</strong></div>
        </div>
        {!last && !pending && <p className="ai-activity-empty">No generation requests yet. Try nutrition analysis, recipes or grounded chat.</p>}
        <p className="ai-activity-footnote">Live request statistics. Session counts reset after inactivity or an app restart.</p>
      </>}
      {error && <p role="status" className="ai-activity-error">AI activity is temporarily unavailable. Refresh to try again.</p>}
    </section>
  );
}
