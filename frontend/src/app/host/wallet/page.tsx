'use client';

import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { HostPageFrame } from '@/components/host/HostPageFrame';
import { HostWalletPanel } from '@/components/host/HostWalletPanel';

export default function HostWalletPage() { const fixtures = useHostFixtures(); const wallet = fixtures.getWalletSummary(); const statement = fixtures.getStatement(); return <HostPageFrame fillHeight title="">{wallet.state === 'loading' || statement.state === 'loading' ? <div className="ss-card h-64 animate-pulse bg-[var(--surface-inset)]" /> : wallet.state === 'error' || statement.state === 'error' ? <div className="ss-card p-8 text-center">Unable to load wallet.</div> : <HostWalletPanel wallet={wallet.data!} statement={statement.data!} />}</HostPageFrame>; }
