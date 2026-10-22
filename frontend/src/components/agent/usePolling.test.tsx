import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { usePolling } from './usePolling';

function setVisibility(state: 'visible' | 'hidden') {
  Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => state });
  document.dispatchEvent(new Event('visibilitychange'));
}

describe('usePolling (N8, E46)', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    setVisibility('visible');
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('loads on mount and refetches once after 31 s while visible', async () => {
    const load = vi.fn().mockResolvedValue('a');
    renderHook(() => usePolling(load, 30_000));
    await act(async () => {});
    expect(load).toHaveBeenCalledTimes(1);
    await act(async () => { await vi.advanceTimersByTimeAsync(31_000); });
    expect(load).toHaveBeenCalledTimes(2);
  });

  it('does not fetch while hidden and fetches once when shown again', async () => {
    const load = vi.fn().mockResolvedValue('a');
    renderHook(() => usePolling(load, 30_000));
    await act(async () => {});
    setVisibility('hidden');
    await act(async () => { await vi.advanceTimersByTimeAsync(60_000); });
    expect(load).toHaveBeenCalledTimes(1);
    setVisibility('visible');
    await act(async () => {});
    expect(load).toHaveBeenCalledTimes(2);
  });

  it('drops an older answer that arrives after a newer request', async () => {
    let resolveFirst: (v: string) => void = () => {};
    const load = vi.fn()
      .mockImplementationOnce(() => new Promise<string>((r) => { resolveFirst = r; }))
      .mockResolvedValueOnce('newer');
    const { result } = renderHook(() => usePolling(load, 30_000));
    await act(async () => { result.current.reload(); });
    expect(result.current.data).toBe('newer');
    await act(async () => { resolveFirst('older'); });
    expect(result.current.data).toBe('newer');
  });

  it('exposes the error and stops loading', async () => {
    const load = vi.fn().mockRejectedValue(new Error('boom'));
    const { result } = renderHook(() => usePolling(load, 30_000));
    await act(async () => {});
    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.loading).toBe(false);
  });

  it('clears its timer on unmount', async () => {
    const load = vi.fn().mockResolvedValue('a');
    const { unmount } = renderHook(() => usePolling(load, 30_000));
    await act(async () => {});
    unmount();
    await act(async () => { await vi.advanceTimersByTimeAsync(90_000); });
    expect(load).toHaveBeenCalledTimes(1);
  });
});
