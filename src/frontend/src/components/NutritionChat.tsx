import { FormEvent, useEffect, useState } from 'react';
import { API_URL } from '../config';
import './NutritionChat.css';
import { NutritionFacts } from './NutritionFacts';
import { trackedAiFetch } from '../utils/aiActivity';
import { RagTrace, ChatTrace } from './RagTrace';

type Citation = { id: string; kind: string; title: string; version: string; content?: string;
  basisGrams?: number; preparation?: string; nutrients?: Record<string, number> };
type Message = { role: 'user' | 'assistant'; content: string; citations?: Citation[] };
type Source = { id: string; documentId: string; title: string; content: string; version: string; updatedDate: string };
type Retrieval = { sources: Source[]; retrievalMs: number; message: string; queryVectorized: boolean };

export function NutritionChat() {
  const [csrf, setCsrf] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [retrievalEnabled, setRetrievalEnabled] = useState(false);
  const [retrieval, setRetrieval] = useState<Retrieval | null>(null);
  const [nutritionEnabled, setNutritionEnabled] = useState(false);
  const [mode, setMode] = useState('preview');
  const [trace, setTrace] = useState<ChatTrace | null>(null);
  const [chatPending, setChatPending] = useState(false);
  useEffect(() => {
    let active = true;
    void (async () => {
      try {
        const r = await fetch(`${API_URL}/api/chat/config`, { cache: 'no-store' });
        if (!r.ok) return;
        const config = await r.json();
        if (!config.enabled) return;
        const history = await fetch(`${API_URL}/api/chat`, { cache: 'no-store' });
        if (!history.ok) return;
        const data = await history.json();
        if (active) {
          setCsrf(config.csrfToken); setMessages(data.messages);
          setMode(config.mode);
          setTrace(data.lastTrace || null);
          setRetrievalEnabled(!!config.retrievalEnabled); setNutritionEnabled(!!config.nutritionEnabled);
        }
      } catch { /* An unavailable optional chat does not disable the cart. */ }
    })();
    return () => { active = false; };
  }, []);

  async function send(event: FormEvent) {
    event.preventDefault();
    if (!input.trim() || busy || mode === 'unavailable') return;
    setBusy(true); setError(''); setRetrieval(null); setTrace(null); setChatPending(true);
    try {
      const options = {
        method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': csrf },
        body: JSON.stringify({ message: input.trim() }),
      };
      const r = mode === 'grounded' ? await trackedAiFetch('/api/chat', options) : await fetch(`${API_URL}/api/chat`, options);
      if (!r.headers.get('content-type')?.includes('application/json')) throw new Error('Please refresh the page to sign in again.');
      const result = await r.json();
      setTrace(result.trace || null);
      if (!r.ok) throw new Error(r.status === 403 ? 'Your session expired. Refresh the page and try again.'
        : result.error || 'Chat is temporarily unavailable. Your cart is still available.');
      setMessages(previous => [...previous, { role: 'user', content: input.trim() } as Message,
        { role: 'assistant', content: result.answer, citations: result.citations || [] } as Message].slice(-12));
      setInput('');
    } catch (e) { setError(e instanceof Error ? e.message : 'Unable to send message.'); }
    finally { setBusy(false); setChatPending(false); }
  }

  async function reset() {
    if (busy) return;
    setBusy(true); setError('');
    try {
      const r = await fetch(`${API_URL}/api/chat`, { method: 'DELETE', headers: { 'X-CSRF-TOKEN': csrf } });
      if (!r.ok) throw new Error('Unable to clear chat. Refresh and try again.');
      setMessages([]); setRetrieval(null); setTrace(null);
    } catch (e) { setError(e instanceof Error ? e.message : 'Unable to clear chat.'); }
    finally { setBusy(false); }
  }

  async function retrieve() {
    if (busy || !input.trim()) return;
    setBusy(true); setError(''); setRetrieval(null);
    try {
      const r = await fetch(`${API_URL}/api/chat/retrieval`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': csrf },
        body: JSON.stringify({ message: input.trim() }),
      });
      const data = await r.json();
      if (!r.ok) throw new Error(data.error || 'Unable to retrieve documents. Refresh and try again.');
      setRetrieval(data);
    } catch (e) { setError(e instanceof Error ? e.message : 'Document retrieval is unavailable.'); }
    finally { setBusy(false); }
  }
  if (!csrf) return null;
  return <section className="nutrition-chat" aria-label="Nutrition chat">
    <div className="nutrition-chat-heading"><h2>Ask about your cart</h2><span>{mode === 'grounded' ? 'Grounded chat · Azure'
      : mode === 'unavailable' ? 'Chat unavailable' : nutritionEnabled ? 'Product facts · step 4'
      : retrievalEnabled ? 'Retrieval preview · step 3' : 'Preview · step 1'}</span></div>
    <p className="nutrition-chat-note">{mode === 'grounded'
      ? 'Answers use retrieved demo documents and current product records, with sources shown below each answer. All nutrition data is fictional workshop content.'
      : retrievalEnabled
      ? 'Explore the supporting demo documents before connecting answer generation. All content is synthetic.'
      : 'Test the chat experience. AI and document retrieval will be connected in the next steps.'}</p>
    {nutritionEnabled && <NutritionFacts csrf={csrf} />}
    <div className="nutrition-chat-messages" role="log" aria-live="polite">
      {messages.length === 0 && <p className="nutrition-chat-note">Try: “What could help lower sodium in my meal?”</p>}
      {messages.map((message, i) => <div key={i} className={`nutrition-chat-message ${message.role}`}>
        <strong>{message.role === 'user' ? 'You' : 'Cart assistant'}</strong><p>{message.content}</p>
        {!!message.citations?.length && <div className="nutrition-chat-citations">
          <span>Sources for this answer</span>
          {message.citations.map(citation => <details key={citation.id}>
            <summary>{citation.title} · v{citation.version}</summary>
            <p className="nutrition-chat-note">Synthetic demo {citation.kind}</p>
            {citation.content && <p>{citation.content}</p>}
            {citation.nutrients && <p>Per {citation.basisGrams} g, {citation.preparation}: {citation.nutrients.energy_kcal} kcal
              {' · '}{citation.nutrients.protein_g} g protein{' · '}{citation.nutrients.sodium_mg} mg sodium.</p>}
          </details>)}
        </div>}
      </div>)}
    </div>
    <form onSubmit={send}>
      <label htmlFor="nutrition-question">Your question</label>
      <textarea id="nutrition-question" value={input} onChange={e => setInput(e.target.value)}
        maxLength={2000} rows={2} disabled={busy} placeholder="Ask about ingredients, portions or alternatives…" />
      <div className="nutrition-chat-actions">
        <button type="submit" className="btn btn-primary" disabled={busy || !input.trim() || mode === 'unavailable'}>{busy ? 'Working…' : 'Send'}</button>
        {retrievalEnabled && <button type="button" className="btn btn-outline-primary" disabled={busy || !input.trim()} onClick={retrieve}>Retrieve documents only</button>}
        <button type="button" className="btn btn-outline-secondary" disabled={busy || !messages.length} onClick={reset}>Clear chat</button>
      </div>
    </form>
    {retrieval && <div className="nutrition-chat-evidence" aria-live="polite">
      <p><strong>{retrieval.message}</strong></p>
      <p className="nutrition-chat-note">{retrieval.sources.length} passages · {(retrieval.retrievalMs / 1000).toFixed(2)} s
        {retrieval.queryVectorized ? ' · Search embedded this question' : ' · No query embedding needed'}
        {' · No answer generated'}</p>
      {retrieval.sources.map(source => <details key={source.id}>
        <summary>{source.title} · v{source.version}</summary>
        <p className="nutrition-chat-note">Synthetic demo document · updated {source.updatedDate}</p>
        <p style={{ whiteSpace: 'pre-wrap' }}>{source.content}</p>
      </details>)}
    </div>}
    {error && <p className="text-danger mt-2" role="alert">{error}</p>}
    {mode === 'grounded' && <RagTrace trace={trace} pending={chatPending} />}
    <p className="nutrition-chat-note mt-2">Chat history stays in this browser session and resets after inactivity or an app restart.</p>
  </section>;
}
