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
  throw await toBackendError(response);
}

/** Calls the backend and parses the JSON body; a 204 response resolves to undefined. */
export async function apiJson<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await authenticatedFetch(path, init);
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

const SESSION_FAILURE_CODES = new Set(['PROFILE_INACTIVE', 'PROFILE_NOT_REGISTERED', 'ROLE_MISMATCH']);

/** True when the error means the signed-in session can no longer use the API. */
export function isSessionFailure(error: unknown): boolean {
  return error instanceof BackendApiError && (error.status === 401 || SESSION_FAILURE_CODES.has(error.code));
}

export async function validateRegistrationCode(
  role: 'HOST' | 'AGENT',
  registrationCode: string,
): Promise<void> {
  let response: Response;
  try {
    response = await fetch(`${apiBaseUrl()}/api/auth/registration-code/validate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ role, registrationCode }),
    });
  } catch {
    throw new BackendApiError('API_UNAVAILABLE', 'Unable to validate registration right now', undefined, 0);
  }
  if (response.ok) return;
  throw await toBackendError(response);
}

async function toBackendError(response: Response): Promise<BackendApiError> {
  let body: { code?: unknown; message?: unknown; requestId?: unknown } = {};
  try {
    body = await response.json();
  } catch {
    // Preserve the HTTP status when the backend did not return JSON.
  }
  return new BackendApiError(
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
  id: string;
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
