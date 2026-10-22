'use client';

import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { useOptionalHostApi } from '@/components/host/HostApiProvider';
import { HostPageFrame } from '@/components/host/HostPageFrame';
import { HostWalletPanel } from '@/components/host/HostWalletPanel';
import { mapHostWallet } from '@/components/host/hostTypes';

export default function HostWalletPage() {
  const fixtures = useHostFixtures();
  const api = useOptionalHostApi();
  const walletData = api?.wallet.status === 'ready' ? mapHostWallet(api.wallet.data) : undefined;
  const fixtureWallet = fixtures.getWalletSummary();
  const fixtureStatement = fixtures.getStatement();
  const loading = api ? !walletData && api.wallet.status === 'loading' : fixtureWallet.state === 'loading' || fixtureStatement.state === 'loading';
  const error = api ? api.wallet.status === 'error' : fixtureWallet.state === 'error' || fixtureStatement.state === 'error';
  return <HostPageFrame fillHeight title="">
    {loading ? <div className="ss-card h-64 animate-pulse bg-[var(--surface-inset)]" /> : error ? <div className="ss-card p-8 text-center">Unable to load wallet.</div> : <HostWalletPanel
      wallet={walletData?.wallet ?? fixtureWallet.data!}
      statement={walletData?.statement ?? fixtureStatement.data!}
      onMutation={api ? (operation, amount) => api.mutateWallet(operation, amount) : undefined}
      mutationPending={api?.walletMutationPending}
      recoveryRequired={api?.walletRecoveryRequired}
      onRetryRecovery={api ? api.retryWalletRecovery : undefined}
      hasMore={false}
    />}
  </HostPageFrame>;
}
