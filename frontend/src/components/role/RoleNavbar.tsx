import Link from 'next/link';

export type WorkspaceRole = 'GUEST' | 'HOST' | 'AGENT';

const navigation = {
  GUEST: ['Search', 'Trips', 'Messages', 'Wallet'],
  HOST: ['Listings', 'Requests', 'Messages', 'Wallet'],
  AGENT: ['Disputes', 'Accounts', 'Audit Log', 'Categories'],
} as const;

const workspaceLabels: Partial<Record<WorkspaceRole, string>> = {
  HOST: 'Host workspace',
  AGENT: 'Support Agent workspace',
};

export function RoleNavbar({ role, onLogout }: { role: WorkspaceRole; onLogout?: () => void }) {
  const landingPath = `/${role.toLowerCase()}`;
  return (
    <nav aria-label="Workspace navigation" className="ss-role-navbar border-b border-[var(--border-muted)] bg-card px-6 pb-0 pt-3">
      <div className="w-full">
        <div data-testid="workspace-navbar-top" className="flex min-h-7 items-center gap-3">
          <Link href={landingPath} className="text-sm font-extrabold tracking-[0.04em] text-primary">SnoozeShare</Link>
          {workspaceLabels[role] && <span className="ss-pill ss-status-neutral">{workspaceLabels[role]}</span>}
          <span aria-label="Wallet" className="ss-pill ss-status-neutral ml-auto">Wallet</span>
          <button type="button" className="h-6 shrink-0 border-0 bg-transparent px-0 text-xs font-medium text-foreground hover:bg-transparent hover:text-primary" onClick={onLogout}>
            Log out
          </button>
        </div>
        <div data-testid="workspace-navbar-divider" className="-mx-6 mt-3 border-t border-[var(--border-muted)]" />
        <div data-testid="workspace-navbar-tabs" className="mt-3 flex flex-wrap items-center gap-x-6 gap-y-2">
          {navigation[role].map((item, index) => (
            <Link
              key={item}
              href={landingPath}
              aria-current={index === 0 ? 'page' : undefined}
              className="border-b-2 border-transparent px-3 pb-3 text-xs text-muted-foreground transition-colors hover:text-primary aria-[current=page]:border-primary aria-[current=page]:font-semibold aria-[current=page]:text-primary"
            >
              {item}
            </Link>
          ))}
        </div>
      </div>
    </nav>
  );
}
