'use client';

import { useCallback, useEffect, useState } from 'react';
import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { useOptionalHostApi } from '@/components/host/HostApiProvider';
import { HostPageFrame } from '@/components/host/HostPageFrame';
import { HostRequestsTable } from '@/components/host/HostRequestsTable';
import { mapHostBooking, type HostBookingDto, type HostPage } from '@/components/host/hostTypes';

export default function HostRequestsPage() {
  const fixtures = useHostFixtures();
  const api = useOptionalHostApi();
  const [pendingPage, setPendingPage] = useState(0);
  const [historyPage, setHistoryPage] = useState(0);
  const pendingKey = `host:requests:pending:${pendingPage}`;
  const historyKey = `host:requests:history:${historyPage}`;
  const loadResource = api?.loadResource;
  const fetchRequests = api?.api.fetchHostRequests;
  useEffect(() => {
    if (!loadResource || !fetchRequests) return;
    void loadResource(pendingKey, () => fetchRequests('pending', pendingPage, 50)).catch(() => undefined);
    void loadResource(historyKey, () => fetchRequests('history', historyPage, 50)).catch(() => undefined);
  }, [fetchRequests, historyKey, historyPage, loadResource, pendingKey, pendingPage]);
  const pendingResource = api?.getResource<HostPage<HostBookingDto>>(pendingKey);
  const historyResource = api?.getResource<HostPage<HostBookingDto>>(historyKey);
  const fixture = fixtures.getRequests();
  const apiMode = Boolean(api);
  const loading = apiMode
    ? !pendingResource || !historyResource || pendingResource.status === 'loading' || historyResource.status === 'loading'
    : fixture.state === 'loading';
  const error = apiMode
    ? pendingResource?.status === 'error' ? pendingResource.message : historyResource?.status === 'error' ? historyResource.message : ''
    : fixture.state === 'error' ? fixture.error : '';
  const requests = apiMode
    ? {
      pending: pendingResource?.status === 'ready' || pendingResource?.status === 'empty' ? pendingResource.data.items.map(mapHostBooking) : [],
      past: historyResource?.status === 'ready' || historyResource?.status === 'empty' ? historyResource.data.items.map(mapHostBooking) : [],
    }
    : fixture.data ?? { pending: [], past: [] };
  const pendingHasMore = apiMode && (pendingResource?.status === 'ready' || pendingResource?.status === 'empty') && pendingResource.data.hasMore;
  const historyHasMore = apiMode && (historyResource?.status === 'ready' || historyResource?.status === 'empty') && historyResource.data.hasMore;
  const refresh = useCallback(() => {
    if (!api || !loadResource || !fetchRequests) return;
    void loadResource(pendingKey, () => fetchRequests('pending', pendingPage, 50)).catch(() => undefined);
    void loadResource(historyKey, () => fetchRequests('history', historyPage, 50)).catch(() => undefined);
  }, [api, fetchRequests, historyKey, historyPage, loadResource, pendingKey, pendingPage]);
  const decide = async (requestId: string, approve: boolean, message?: string) => {
    if (!api) return;
    await api.api.decideHostBooking(requestId, approve, message);
    refresh();
  };
  const hasData = !loading && !error;
  return <HostPageFrame fillHeight title="Booking requests" titleAddon={hasData ? <span className="ss-pill ss-status-pending">{requests.pending.length} pending</span> : undefined}>
    {loading ? <div className="ss-card h-64 animate-pulse bg-[var(--surface-inset)]" /> : error ? <div className="ss-card space-y-3 p-8 text-center"><p>{error}</p><button className="rounded-lg border border-border px-3 py-2 text-sm" onClick={refresh} type="button">Retry</button></div> : <HostRequestsTable
      requests={requests}
      onDecision={api ? (request, approve, message) => decide(request.id, approve, message) : undefined}
      pendingHasMore={pendingHasMore}
      historyHasMore={historyHasMore}
      onNextPending={() => setPendingPage((value) => value + 1)}
      onPreviousPending={() => setPendingPage((value) => Math.max(0, value - 1))}
      onNextHistory={() => setHistoryPage((value) => value + 1)}
      onPreviousHistory={() => setHistoryPage((value) => Math.max(0, value - 1))}
      pendingPage={pendingPage}
      historyPage={historyPage}
    />}
  </HostPageFrame>;
}
