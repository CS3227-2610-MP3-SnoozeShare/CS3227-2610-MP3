import { apiBaseUrl } from './env';
import { auth } from './firebase';

export class BackendApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly requestId: string | undefined,
    public readonly status: number,
  ) {
    super(message);
    this.name = 'BackendApiError';
  }
}

/** Fetches a backend resource with the current Firebase ID token. */
export async function authenticatedFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const user = auth.currentUser;
  if (!user) throw new BackendApiError('AUTH_REQUIRED', 'Sign-in is required', undefined, 401);
  const token = await user.getIdToken();
  const headers = new Headers(init.headers);
  headers.set('Authorization', `Bearer ${token}`);
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  const response = await fetch(`${apiBaseUrl()}${path}`, { ...init, headers });
  if (response.ok) return response;
  let body: { code?: unknown; message?: unknown; requestId?: unknown } = {};
  try {
    body = await response.json();
  } catch {
    // Preserve the HTTP status when the backend did not return JSON.
  }
  throw new BackendApiError(
    typeof body.code === 'string' ? body.code : 'API_ERROR',
    typeof body.message === 'string' ? body.message : 'Request could not be completed',
    typeof body.requestId === 'string' ? body.requestId : undefined,
    response.status,
  );
}

export async function registerProfile(payload: {
  role: 'GUEST' | 'HOST' | 'AGENT';
  registrationCode?: string;
  displayName?: string;
}) {
  const response = await authenticatedFetch('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return response.json() as Promise<AuthProfile>;
}

export async function fetchCurrentProfile() {
  const response = await authenticatedFetch('/api/auth/me');
  return response.json() as Promise<AuthProfile>;
}

export type AuthProfile = {
  email: string;
  role: 'GUEST' | 'HOST' | 'AGENT';
  displayName: string | null;
  walletId: string | null;
};

/** True only for a 200 response whose JSON body has status UP; every other outcome is false. */
export async function fetchBackendHealth(signal: AbortSignal): Promise<boolean> {
  try {
    const response = await fetch(`${apiBaseUrl()}/actuator/health`, { signal });
    if (!response.ok) return false;
    const body: unknown = await response.json();
    return typeof body === 'object' && body !== null && (body as { status?: unknown }).status === 'UP';
  } catch {
    return false;
  }
}
