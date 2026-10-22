'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from 'cn';

export type WorkspaceRole = 'GUEST' | 'HOST' | 'AGENT';

const navigation: Record<WorkspaceRole, { label: string; href: string }[]> = {
  GUEST: ['Search', 'Trips', 'Messages', 'Wallet'].map((label) => ({ label, href: '/guest' })),
  HOST: [
    { label: 'Listings', href: '/host/listings' }, { label: 'Requests', href: '/host/requests' },
    { label: 'Messages', href: '/host/messages' }, { label: 'Wallet', href: '/host/wallet' },
  ],
  AGENT: [
    { label: 'Disputes', href: '/agent/disputes' }, { label: 'Accounts', href: '/agent/accounts' },
    { label: 'Audit Log', href: '/agent/audit' }, { label: 'Categories', href: '/agent/categories' },
  ],
} as const;

const workspaceLabels: Partial<Record<WorkspaceRole, string>> = {
  HOST: 'Host workspace',
  AGENT: 'Support Agent workspace',
};

export function RoleNavbar({ role, onLogout, walletLabel, activePath, avatarInitials }: { role: WorkspaceRole; onLogout?: () => void; walletLabel?: string; activePath?: string; avatarInitials?: string }) {
  const landingPath = role === 'HOST' ? '/host/listings' : `/${role.toLowerCase()}`;
  const pathname = usePathname();
  return (
    <nav aria-label="Workspace navigation" className="ss-role-navbar sticky top-0 z-40 shrink-0 border-b border-[var(--border-muted)] bg-card px-6 pb-0 pt-3">
      <div className="w-full">
        <div data-testid="workspace-navbar-top" className="flex min-h-7 flex-wrap items-center gap-3">
          <Link href={landingPath} className="text-base font-extrabold tracking-[0.04em] text-primary">SnoozeShare</Link>
          {workspaceLabels[role] && <span className="ss-pill ss-status-neutral text-xs">{workspaceLabels[role]}</span>}
          {role !== 'AGENT' && <span aria-label={walletLabel ? `Wallet: ${walletLabel}` : 'Wallet'} className="ss-pill ss-status-neutral ml-auto text-xs">{walletLabel ? `Wallet: ${walletLabel}` : 'Wallet'}</span>}
          <button type="button" className={cn('h-6 shrink-0 border-0 bg-transparent px-0 text-sm font-medium text-foreground hover:bg-transparent hover:text-primary', role === 'AGENT' ? 'ml-auto' : 'ml-2')} onClick={onLogout}>
            Log out
          </button>
          {role === 'AGENT' && avatarInitials && (
            <span aria-hidden className="flex size-8 items-center justify-center rounded-full bg-accent text-[13px] font-bold text-primary">{avatarInitials}</span>
          )}
        </div>
        <div data-testid="workspace-navbar-divider" className="-mx-6 mt-3 border-t border-[var(--border-muted)]" />
        <div data-testid="workspace-navbar-tabs" className="mt-3 flex flex-wrap items-center gap-x-6 gap-y-2">
          {navigation[role].map((item, index) => {
            const isActive = role === 'AGENT' ? (pathname?.startsWith(item.href) ?? false) : activePath ? (role === 'HOST' ? activePath === item.href || activePath.startsWith(`${item.href}/`) : index === 0) : index === 0;
            return (
            <Link
              key={item.label}
              href={item.href}
              aria-current={isActive ? 'page' : undefined}
              className={cn('border-b-2 border-transparent px-3 pb-3 text-sm text-muted-foreground transition-colors hover:text-primary', isActive && 'border-primary font-semibold text-primary')}
            >
              {item.label}
            </Link>
            );
          })}
        </div>
      </div>
    </nav>
  );
}
