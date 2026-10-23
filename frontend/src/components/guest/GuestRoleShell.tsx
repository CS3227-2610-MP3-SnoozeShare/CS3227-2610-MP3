'use client';

import { useEffect, useState } from 'react';
import { usePathname } from 'next/navigation';
import { getWallet } from '@/lib/guest-api';
import { RoleShell } from '@/components/role/RoleShell';

export function GuestRoleShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname() ?? '/guest/search';
  const [balance, setBalance] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setFailed(false);
    void getWallet()
      .then((wallet) => { if (active) setBalance(wallet.balance); })
      .catch(() => { if (active) { setBalance(null); setFailed(true); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  const walletLabel = loading
    ? '—'
    : failed || balance === null
      ? 'unavailable'
      : `$${balance.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

  return <RoleShell role="GUEST" activePath={pathname} walletLabel={walletLabel}>{children}</RoleShell>;
}
