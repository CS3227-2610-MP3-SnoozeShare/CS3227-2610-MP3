'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

export type PollingState<T> = {
  data: T | undefined;
  error: unknown;
  loading: boolean;
  reload: () => void;
};

/**
 * Loads once, then again every `intervalMs` while the tab is visible (C78, N8).
 * A response older than the newest request is ignored. Reloading keeps the last data on screen.
 */
export function usePolling<T>(load: () => Promise<T>, intervalMs: number): PollingState<T> {
  const [data, setData] = useState<T | undefined>(undefined);
  const [error, setError] = useState<unknown>(undefined);
  const [loading, setLoading] = useState(true);
  const generation = useRef(0);
  const loadRef = useRef(load);
  loadRef.current = load;

  const run = useCallback(() => {
    const mine = ++generation.current;
    loadRef.current().then(
      (value) => {
        if (mine !== generation.current) return;
        setData(value);
        setError(undefined);
        setLoading(false);
      },
      (caught: unknown) => {
        if (mine !== generation.current) return;
        setError(caught);
        setLoading(false);
      },
    );
  }, []);

  useEffect(() => {
    let timer: ReturnType<typeof setInterval> | undefined;
    const stop = () => {
      if (timer !== undefined) clearInterval(timer);
      timer = undefined;
    };
    const start = () => {
      stop();
      timer = setInterval(run, intervalMs);
    };
    const onVisibility = () => {
      if (document.visibilityState === 'visible') {
        run();
        start();
      } else {
        stop();
      }
    };
    run();
    if (document.visibilityState === 'visible') start();
    document.addEventListener('visibilitychange', onVisibility);
    return () => {
      stop();
      generation.current += 1;
      document.removeEventListener('visibilitychange', onVisibility);
    };
  }, [intervalMs, run]);

  return { data, error, loading, reload: run };
}
