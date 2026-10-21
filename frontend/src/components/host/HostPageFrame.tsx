import type { ReactNode } from 'react';

export function HostPageFrame({ title, breadcrumbs, actions, children }: { title: string; breadcrumbs?: ReactNode; actions?: ReactNode; children: ReactNode }) {
  return (
    <main className="w-full max-w-none px-6 py-6">
      {breadcrumbs && <nav aria-label="Breadcrumb" className="mb-4 flex flex-wrap items-center gap-2 text-xs text-[var(--text-subtle)]">{breadcrumbs}</nav>}
      <div className="mb-5 flex flex-wrap items-center justify-between gap-4"><h1 className="text-2xl font-bold tracking-tight">{title}</h1>{actions}</div>
      {children}
    </main>
  );
}
