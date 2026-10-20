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

export function RoleNavbar({ role }: { role: WorkspaceRole }) {
  const landingPath = `/${role.toLowerCase()}`;
  return (
    <nav aria-label="Workspace navigation" className="ss-role-navbar border-b border-[var(--border-muted)] bg-card px-6 py-3">
      <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-x-8 gap-y-3">
        <Link href={landingPath} className="text-base font-bold tracking-tight text-foreground">SnoozeShare</Link>
        <div className="flex min-w-0 flex-1 flex-wrap items-center gap-x-5 gap-y-2">
          {navigation[role].map((item, index) => (
            <Link
              key={item}
              href={landingPath}
              aria-current={index === 0 ? 'page' : undefined}
              className="text-sm text-muted-foreground transition-colors hover:text-primary aria-[current=page]:font-semibold aria-[current=page]:text-primary"
            >
              {item}
            </Link>
          ))}
        </div>
        {workspaceLabels[role] && <span className="ss-pill ss-status-neutral shrink-0">{workspaceLabels[role]}</span>}
      </div>
    </nav>
  );
}
