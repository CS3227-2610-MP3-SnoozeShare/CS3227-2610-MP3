import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { BackendStatus } from './BackendStatus';

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

/** A fetch that never answers and rejects like the browser when its signal aborts. */
function hangingFetch(_url: string, init?: RequestInit): Promise<Response> {
  return new Promise((_resolve, reject) => {
    init?.signal?.addEventListener('abort', () => reject(new DOMException('aborted', 'AbortError')));
  });
}

describe('BackendStatus', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });
  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('shows UP when the backend answers 200 with status UP (E24)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ status: 'UP' })));
    render(<BackendStatus />);
    expect(screen.getByText('Backend: checking...')).toBeInTheDocument();
    expect(await screen.findByText('Backend: UP')).toBeInTheDocument();
  });

  it.each([
    ['a network or CORS error', () => vi.fn().mockRejectedValue(new TypeError('Failed to fetch'))],
    ['a 503 response', () => vi.fn().mockResolvedValue(jsonResponse({ status: 'DOWN' }, 503))],
    ['a non-JSON body', () => vi.fn().mockResolvedValue(new Response('<html>', { status: 200 }))],
    ['a 200 with status DOWN', () => vi.fn().mockResolvedValue(jsonResponse({ status: 'DOWN' }))],
  ])('shows unreachable on %s (E25)', async (_name, makeFetch) => {
    vi.stubGlobal('fetch', makeFetch());
    render(<BackendStatus />);
    expect(await screen.findByText('Backend: unreachable')).toBeInTheDocument();
  });

  it('shows unreachable when the request takes longer than 5 s (E25)', async () => {
    vi.useFakeTimers();
    vi.stubGlobal('fetch', vi.fn(hangingFetch));
    render(<BackendStatus />);
    expect(screen.getByText('Backend: checking...')).toBeInTheDocument();
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });
    expect(screen.getByText('Backend: unreachable')).toBeInTheDocument();
  });

  it('disables Re-check while loading and repeats the check when clicked (E26)', async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(jsonResponse({ status: 'UP' })));
    vi.stubGlobal('fetch', fetchMock);
    render(<BackendStatus />);
    const button = screen.getByRole('button', { name: 'Re-check' });
    expect(button).toBeDisabled();
    await screen.findByText('Backend: UP');
    expect(button).toBeEnabled();
    await userEvent.click(button);
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    await screen.findByText('Backend: UP');
  });

  it('aborts the in-flight request on unmount (E26)', () => {
    const abort = vi.spyOn(AbortController.prototype, 'abort');
    vi.stubGlobal('fetch', vi.fn(hangingFetch));
    const { unmount } = render(<BackendStatus />);
    unmount();
    expect(abort).toHaveBeenCalled();
  });
});
