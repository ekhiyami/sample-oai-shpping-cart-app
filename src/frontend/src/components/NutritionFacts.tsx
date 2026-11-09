import { useEffect, useRef, useState } from 'react';
import { API_URL } from '../config';

type Fact = { productId: string; productName: string; preparation: string; version: string;
  basisGrams: number; nutrients: Record<string, number> };
type Line = { itemId: number; name: string; quantity: number; productId: string | null; totalGrams: number | null;
  message: string; recordVersion: string | null; totals: Record<string, number> | null };
type Snapshot = { records: Fact[]; lines: Line[]; cartTotals: Record<string, number> | null };
type Draft = { productId: string; grams: string };

export function NutritionFacts({ csrf }: { csrf: string }) {
  const [data, setData] = useState<Snapshot | null>(null);
  const [drafts, setDrafts] = useState<Record<number, Draft>>({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const sequence = useRef(0);

  function accept(value: Snapshot) {
    setData(value);
    setDrafts(Object.fromEntries(value.lines.map(line => [line.itemId,
      { productId: line.productId || '', grams: line.totalGrams == null ? '' : String(line.totalGrams) }])));
  }
  async function refresh() {
    const n = ++sequence.current;
    try {
      const r = await fetch(`${API_URL}/api/chat/nutrition`, { cache: 'no-store' });
      if (!r.ok) throw new Error('Product facts are unavailable. Refresh the page to try again.');
      const value = await r.json();
      if (n === sequence.current) { accept(value); setError(''); }
    } catch (e) { if (n === sequence.current) setError(e instanceof Error ? e.message : 'Unable to load product facts.'); }
  }
  useEffect(() => {
    const changed = () => { void refresh(); };
    window.addEventListener('cart-change', changed);
    void refresh();
    return () => { sequence.current++; window.removeEventListener('cart-change', changed); };
  }, [csrf]);

  async function save(line: Line) {
    if (busy) return;
    const draft = drafts[line.itemId];
    setBusy(true); setError('');
    ++sequence.current;
    try {
      const r = await fetch(`${API_URL}/api/chat/portions`, {
        method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': csrf },
        body: JSON.stringify({ itemId: line.itemId, productId: draft.productId || null,
          totalGrams: draft.productId && draft.grams !== '' ? Number(draft.grams) : null }),
      });
      if (!r.ok) throw new Error('Unable to save. Choose a demo product and a valid weight, or refresh if your session expired.');
      accept(await r.json());
    } catch (e) { setError(e instanceof Error ? e.message : 'Unable to save product selection.'); }
    finally { setBusy(false); }
  }
  function edit(id: number, patch: Partial<Draft>) {
    setDrafts(previous => ({ ...previous, [id]: { ...previous[id], ...patch } }));
  }
  function totals(values: Record<string, number>) {
    return `${values.energy_kcal} kcal · ${values.protein_g} g protein · ${values.sodium_mg} mg sodium`;
  }
  return <details className="nutrition-facts" open>
    <summary>Demo product facts and portions</summary>
    <p className="nutrition-chat-note">These fictional records come from the demo database. Totals are calculated by the app, without an AI call. Choose the exact product and total weight for each cart line.</p>
    {data?.lines.map(line => <div className="nutrition-fact-line" key={line.itemId}>
      <strong>{line.name} × {line.quantity}</strong>
      <label htmlFor={`product-${line.itemId}`}>Demo product</label>
      <select id={`product-${line.itemId}`} value={drafts[line.itemId]?.productId || ''}
        onChange={e => edit(line.itemId, { productId: e.target.value })} disabled={busy}>
        <option value="">Not selected</option>
        {data.records.map(record => <option key={record.productId} value={record.productId}>
          {record.productName} · {record.preparation}
        </option>)}
      </select>
      <label htmlFor={`grams-${line.itemId}`}>Total grams for this cart line</label>
      <div className="nutrition-chat-actions">
        <input id={`grams-${line.itemId}`} type="number" min="0.001" max="100000" step="0.001"
          value={drafts[line.itemId]?.grams || ''} disabled={busy || !drafts[line.itemId]?.productId}
          onChange={e => edit(line.itemId, { grams: e.target.value })} placeholder="e.g. 150" />
        <button type="button" className="btn btn-outline-primary" disabled={busy}
          onClick={() => save(line)}>Save portion</button>
      </div>
      <p className="nutrition-chat-note">{line.totals ? `${totals(line.totals)} · record v${line.recordVersion}` : line.message}</p>
    </div>)}
    {data?.lines.length === 0 && <p>Add an item to the cart to select a product and weight.</p>}
    {data?.cartTotals && <p><strong>Complete cart: {totals(data.cartTotals)}</strong></p>}
    {data && !data.cartTotals && data.lines.length > 0 && <p className="nutrition-chat-note">A full cart total needs a product and weight for every line. Item counts are not treated as grams or servings.</p>}
    <button type="button" className="btn btn-sm btn-link" disabled={busy} onClick={() => { void refresh(); }}>Refresh product records</button>
    {error && <p className="text-danger" role="alert">{error}</p>}
  </details>;
}
