'use client';

import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { HostPageFrame } from '@/components/host/HostPageFrame';
import { HostRequestsTable } from '@/components/host/HostRequestsTable';

export default function HostRequestsPage() { const result = useHostFixtures().getRequests(); const hasData = result.state === 'populated' || result.state === 'empty'; return <HostPageFrame fillHeight title="Booking requests" titleAddon={hasData ? <span className="ss-pill ss-status-pending">{result.data.pending.length} pending</span> : undefined}>{result.state === 'loading' ? <div className="ss-card h-64 animate-pulse bg-[var(--surface-inset)]" /> : result.state === 'error' ? <div className="ss-card p-8 text-center">{result.error}</div> : <HostRequestsTable requests={result.data!} />}</HostPageFrame>; }
