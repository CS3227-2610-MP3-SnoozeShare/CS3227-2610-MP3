const DEFAULT_API_BASE_URL = 'http://localhost:8080';

/** The only reader of NEXT_PUBLIC_API_BASE_URL. Next inlines this literal access into client code. */
export function apiBaseUrl(): string {
  return process.env.NEXT_PUBLIC_API_BASE_URL ?? DEFAULT_API_BASE_URL;
}

/** Called from next.config.ts: a production build must not fall back to localhost. */
export function validateEnv(env: Record<string, string | undefined> = process.env): void {
  if (env.NODE_ENV !== 'production') return;
  const value = env.NEXT_PUBLIC_API_BASE_URL;
  if (!value) throw new Error('NEXT_PUBLIC_API_BASE_URL is required');
  if (!/^https?:\/\/[^/]+$/.test(value)) {
    throw new Error('NEXT_PUBLIC_API_BASE_URL must be an http(s) URL without a trailing slash');
  }
}
