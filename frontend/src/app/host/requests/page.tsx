'use client';

import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { HostPageFrame } from '@/components/host/HostPageFrame';
import { HostRequestsTable } from '@/components/host/HostRequestsTable';

export default function HostRequestsPage() { const result = useHostFixtures().getRequests(); return <HostPageFrame title="Booking requests">{result.state === 'loading' ? <div className="ss-card h-64 animate-pulse bg-[var(--surface-inset)]" /> : result.state === 'error' ? <div className="ss-card p-8 text-center">{result.error}</div> : <HostRequestsTable requests={result.data!} />}</HostPageFrame>; }
