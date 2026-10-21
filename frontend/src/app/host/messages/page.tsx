'use client';

import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { HostMessagesPanel } from '@/components/host/HostMessagesPanel';
import { HostPageFrame } from '@/components/host/HostPageFrame';

export default function HostMessagesPage() { const result = useHostFixtures().getMessages(); return <HostPageFrame fillHeight title="Messages">{result.state === 'loading' ? <div className="ss-card h-64 animate-pulse bg-[var(--surface-inset)]" /> : result.state === 'error' ? <div className="ss-card p-8 text-center">{result.error}</div> : <HostMessagesPanel conversations={result.data!} />}</HostPageFrame>; }
