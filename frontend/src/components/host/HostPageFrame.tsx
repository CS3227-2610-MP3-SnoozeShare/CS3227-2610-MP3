import type { ReactNode } from 'react';

export function HostPageFrame({ title, breadcrumbs, breadcrumbActions, actions, children }: { title: string; breadcrumbs?: ReactNode; breadcrumbActions?: ReactNode; actions?: ReactNode; children: ReactNode }) {
  return (
    <main className="w-full max-w-none px-6 py-6">
      {breadcrumbs && breadcrumbActions && <div className="mb-4 flex flex-wrap items-start justify-between gap-3"><nav aria-label="Breadcrumb" className="flex flex-wrap items-center gap-2 text-xs text-[var(--text-subtle)]">{breadcrumbs}</nav><div className="flex flex-wrap items-center gap-2">{breadcrumbActions}</div></div>}
      {breadcrumbs && !breadcrumbActions && <nav aria-label="Breadcrumb" className="mb-4 flex flex-wrap items-center gap-2 text-xs text-[var(--text-subtle)]">{breadcrumbs}</nav>}
      {(title || actions) && <div className="mb-5 flex flex-wrap items-center justify-between gap-4"><h1 className="text-2xl font-bold">{title}</h1>{actions}</div>}
      {children}
    </main>
  );
}
