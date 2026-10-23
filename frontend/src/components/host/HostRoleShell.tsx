'use client';

import { usePathname } from 'next/navigation';
import { useHostFixtures } from './HostFixtureProvider';
import { useOptionalHostApi } from './HostApiProvider';
import { RoleShell } from '@/components/role/RoleShell';

export function HostRoleShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname() ?? '/host/listings';
  const wallet = useHostFixtures().getWalletSummary();
  const api = useOptionalHostApi();
  const balance = api?.wallet.status === 'ready' || api?.wallet.status === 'empty'
    ? api.wallet.data.wallet.balance
    : wallet.state === 'populated' || wallet.state === 'empty' ? wallet.data.balance : null;
  const walletLabel = balance !== null
    ? `$${balance.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
    : api?.wallet.status === 'loading' || wallet.state === 'loading' ? '—' : 'unavailable';
  return <RoleShell role="HOST" activePath={pathname} walletLabel={walletLabel}>{children}</RoleShell>;
}
