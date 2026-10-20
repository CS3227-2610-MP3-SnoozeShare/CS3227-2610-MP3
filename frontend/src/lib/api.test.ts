import { beforeEach, describe, expect, it, vi } from 'vitest';

const getIdToken = vi.fn();
const auth = { currentUser: { getIdToken } };

vi.mock('./firebase', () => ({ auth }));

describe('authenticatedFetch', () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
    process.env.NEXT_PUBLIC_API_BASE_URL = 'http://localhost:8080';
    auth.currentUser = { getIdToken };
  });

  it('attaches the Firebase bearer token and preserves request options', async () => {
    getIdToken.mockResolvedValue('id-token');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{"ok":true}', { status: 200 })));
    const { authenticatedFetch } = await import('./api');
    const response = await authenticatedFetch('/api/auth/me', { method: 'GET' });
    expect(response.ok).toBe(true);
    const [, options] = vi.mocked(fetch).mock.calls[0];
    expect(options?.method).toBe('GET');
    expect((options?.headers as Headers).get('Authorization')).toBe('Bearer id-token');
  });

  it('rejects before making a request when no Firebase user exists', async () => {
    auth.currentUser = null;
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const { authenticatedFetch } = await import('./api');
    await expect(authenticatedFetch('/api/auth/me')).rejects.toMatchObject({ code: 'AUTH_REQUIRED' });
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('maps the backend stable error response', async () => {
    getIdToken.mockResolvedValue('id-token');
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ code: 'PROFILE_INACTIVE', message: 'Request could not be completed', requestId: 'request-id' }),
      { status: 403, headers: { 'Content-Type': 'application/json' } },
    )));
    const { authenticatedFetch, BackendApiError } = await import('./api');
    await expect(authenticatedFetch('/api/auth/me')).rejects.toEqual(
      new BackendApiError('PROFILE_INACTIVE', 'Request could not be completed', 'request-id', 403),
    );
  });
});

describe('validateRegistrationCode', () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
    process.env.NEXT_PUBLIC_API_BASE_URL = 'http://localhost:8080';
    auth.currentUser = null;
  });

  it('validates without requiring a Firebase user', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })));
    const { validateRegistrationCode } = await import('./api');
    await expect(validateRegistrationCode('HOST', 'host-code')).resolves.toBeUndefined();
    const [, options] = vi.mocked(fetch).mock.calls[0];
    expect(options?.method).toBe('POST');
    expect(JSON.parse(String(options?.body))).toEqual({ role: 'HOST', registrationCode: 'host-code' });
  });

  it('maps invalid-code and rate-limit responses', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ code: 'INVALID_REGISTRATION_CODE', message: 'Request could not be completed', requestId: 'id' }),
      { status: 403, headers: { 'Content-Type': 'application/json' } },
    )));
    const { validateRegistrationCode } = await import('./api');
    await expect(validateRegistrationCode('HOST', 'wrong')).rejects.toMatchObject({
      code: 'INVALID_REGISTRATION_CODE', status: 403, requestId: 'id',
    });
  });
});
