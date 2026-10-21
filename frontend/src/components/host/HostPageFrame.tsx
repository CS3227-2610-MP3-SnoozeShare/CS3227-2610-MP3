import type { ReactNode } from 'react';

export function HostPageFrame({ title, titleAddon, breadcrumbs, breadcrumbActions, actions, fillHeight = false, children }: { title: string; titleAddon?: ReactNode; breadcrumbs?: ReactNode; breadcrumbActions?: ReactNode; actions?: ReactNode; fillHeight?: boolean; children: ReactNode }) {
  return (
    <main className={`w-full max-w-none px-6 py-6 ${fillHeight ? 'flex min-h-0 flex-1 flex-col' : ''}`}>
      {breadcrumbs && breadcrumbActions && <div className="mb-4 flex flex-wrap items-start justify-between gap-3"><nav aria-label="Breadcrumb" className="flex flex-wrap items-center gap-2 text-xs text-[var(--text-subtle)]">{breadcrumbs}</nav><div className="flex flex-wrap items-center gap-2">{breadcrumbActions}</div></div>}
      {breadcrumbs && !breadcrumbActions && <nav aria-label="Breadcrumb" className="mb-4 flex flex-wrap items-center gap-2 text-xs text-[var(--text-subtle)]">{breadcrumbs}</nav>}
      {(title || actions) && <div className="mb-5 flex shrink-0 flex-wrap items-center justify-between gap-4"><div className="inline-flex items-center gap-2"><h1 className="text-2xl font-bold">{title}</h1>{titleAddon}</div>{actions}</div>}
      {children}
    </main>
  );
}
