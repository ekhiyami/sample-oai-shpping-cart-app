import { API_URL } from '../config';

function apiFetch(path: string, options: RequestInit = {}): Promise<Response> {
  return fetch(`${API_URL}${path}`, { ...options, credentials: 'same-origin' });
}

export const AI_ACTIVITY_EVENT = 'ai-activity-change';
let pendingRequests = 0;

function changed() {
  window.dispatchEvent(new CustomEvent(AI_ACTIVITY_EVENT, { detail: pendingRequests }));
}

export async function trackedAiFetch(path: string, options: RequestInit = {}): Promise<Response> {
  pendingRequests += 1;
  changed();
  try {
    return await apiFetch(path, options);
  } finally {
    pendingRequests -= 1;
    changed();
  }
}

export async function readAiActivity() {
  const response = await apiFetch('/api/ai-activity', { cache: 'no-store' });
  if (!response.ok) throw new Error('AI activity unavailable');
  return response.json();
}
