'use client';

import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { fetchBackendHealth } from '@/lib/api';

type Status = 'loading' | 'up' | 'unreachable';

const LABELS: Record<Status, string> = {
  loading: 'Backend: checking...',
  up: 'Backend: UP',
  unreachable: 'Backend: unreachable',
};

const TIMEOUT_MS = 5000;

export function BackendStatus() {
  const [status, setStatus] = useState<Status>('loading');
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    // A manual timer, not AbortSignal.timeout, so tests can control it with fake timers.
    const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
    fetchBackendHealth(controller.signal).then((up) => {
      if (active) setStatus(up ? 'up' : 'unreachable');
    });
    return () => {
      active = false;
      clearTimeout(timer);
      controller.abort();
    };
  }, [attempt]);

  function recheck() {
    setStatus('loading');
    setAttempt((n) => n + 1);
  }

  return (
    <div className="flex flex-col items-center gap-4">
      <p role="status">{LABELS[status]}</p>
      <Button onClick={recheck} disabled={status === 'loading'}>
        Re-check
      </Button>
    </div>
  );
}
