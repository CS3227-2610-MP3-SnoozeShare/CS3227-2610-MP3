import { apiBaseUrl } from './env';

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
