import type { ReactNode } from 'react';

export function HostPageFrame({ title, breadcrumbs, children }: { title: string; breadcrumbs?: ReactNode; children: ReactNode }) {
  return (
    <main className="w-full max-w-none px-6 py-6">
      {breadcrumbs && <nav aria-label="Breadcrumb" className="mb-4 text-xs text-[var(--text-subtle)]">{breadcrumbs}</nav>}
      <div className="mb-5 flex items-center justify-between gap-4"><h1 className="text-2xl font-bold tracking-tight">{title}</h1></div>
      {children}
    </main>
  );
}
