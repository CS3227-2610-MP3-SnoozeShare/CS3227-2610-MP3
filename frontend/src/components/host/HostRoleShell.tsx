'use client';

import { usePathname } from 'next/navigation';
import { useHostFixtures } from './HostFixtureProvider';
import { RoleShell } from '@/components/role/RoleShell';

export function HostRoleShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname() ?? '/host/listings';
  const wallet = useHostFixtures().getWalletSummary();
  const walletLabel = wallet.state === 'populated' || wallet.state === 'empty'
    ? `$${wallet.data.balance.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
    : wallet.state === 'loading' ? '—' : 'unavailable';
  return <RoleShell role="HOST" activePath={pathname} walletLabel={walletLabel}>{children}</RoleShell>;
}
