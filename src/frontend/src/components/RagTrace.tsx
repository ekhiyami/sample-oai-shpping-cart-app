import './RagTrace.css';

export type ChatTrace = {
  requestId: string; requestedAt?: string; status: string; message: string;
  ragUsed: boolean; generationSent: boolean; lookupStatus: string;
  retrievalStatus: string; generationStatus: string; nutritionRecords?: number;
  documents?: number; passages?: number; queryVectorized?: boolean;
  factLookupMs?: number; retrievalMs?: number; generationMs?: number; totalMs?: number;
  model?: string; inputTokens?: number | null; outputTokens?: number | null;
  queryEmbeddingTokens?: number | null; embeddingUsageNote: string;
  index?: string; corpusVersion?: string;
  sourceVersions?: { title: string; version: string; kind: string }[];
};

const duration = (ms?: number) => ms === undefined ? '—' : `${(ms / 1000).toFixed(2)} s`;
const status = (value: string) => ({
  SUCCESS: 'Completed', FAILED: 'Failed', RUNNING: 'In progress', NOT_REQUESTED: 'Not requested',
}[value] || value);

export function RagTrace({ trace, pending }: { trace: ChatTrace | null; pending: boolean }) {
  const sources = trace?.sourceVersions?.filter((source, i, all) =>
    all.findIndex(other => other.title === source.title && other.version === source.version && other.kind === source.kind) === i) || [];
  return <aside className="rag-trace" aria-label="Last chat request details" aria-live="polite">
    <div className="rag-trace-heading"><strong>How this chat request worked</strong>
      <span>{pending ? 'In progress' : !trace ? 'Ready' : trace.status === 'SUCCESS' ? 'Completed'
        : trace.status === 'FAILED' ? 'Failed' : 'More information needed'}</span></div>
    {pending ? <p>Checking the available evidence. Request details will appear when this finishes.</p>
      : !trace ? <p>Send a question to see the retrieval, evidence and generation steps used.</p>
      : <>
        <p>{trace.message}</p>
        <dl className="rag-trace-summary">
          <div><dt>Documents / passages</dt><dd>{trace.documents ?? '—'} / {trace.passages ?? '—'}</dd></div>
          <div><dt>Product records read</dt><dd>{trace.nutritionRecords ?? '—'}</dd></div>
          <div><dt>Evidence added to AI request</dt><dd>{trace.ragUsed ? 'Yes' : 'No'}</dd></div>
          <div><dt>Generation requested</dt><dd>{trace.generationSent ? 'Yes' : 'No'}</dd></div>
        </dl>
        <details><summary>Explore the RAG steps</summary>
          <ol>
            <li><strong>Read current product facts</strong> — {status(trace.lookupStatus)} · {duration(trace.factLookupMs)}.</li>
            <li><strong>Search demo documents</strong> — {status(trace.retrievalStatus)} · {duration(trace.retrievalMs)}.
              {' '}Query embedding: {trace.queryVectorized === undefined ? 'not confirmed' : trace.queryVectorized ? 'used' : 'not needed'}.
              {trace.index && <> Index: {trace.index}; corpus v{trace.corpusVersion}.</>}</li>
            <li><strong>Generate with Azure OpenAI</strong> — {status(trace.generationStatus)} · {duration(trace.generationMs)}.
              {' '}{trace.ragUsed ? 'Retrieved passages and product facts were included in this request.' : 'No evidence was sent for generation.'}</li>
          </ol>
          <p>Model returned by Azure: <strong>{trace.model || 'Not reported'}</strong>.
            {' '}Generation tokens: {trace.inputTokens ?? 'not reported'} input / {trace.outputTokens ?? 'not reported'} output.</p>
          <p>Query embedding tokens: {trace.queryEmbeddingTokens ?? 'not reported'}. {trace.embeddingUsageNote}</p>
          {!!sources.length && <div><strong>Sources in the generation request</strong>
            <ul>{sources.map(source => <li key={`${source.kind}-${source.title}-${source.version}`}>
              {source.title} · v{source.version} · {source.kind}</li>)}</ul></div>}
          <p className="rag-trace-id">Total: {duration(trace.totalMs)}
            {trace.requestedAt && <> · {new Date(trace.requestedAt).toLocaleString()}</>}
            <br />Request: {trace.requestId}</p>
        </details>
      </>}
  </aside>;
}
