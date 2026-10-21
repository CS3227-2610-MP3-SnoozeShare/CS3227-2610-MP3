import { Fragment } from 'react';
import Link from 'next/link';

export type HostBreadcrumbItem = { label: string; href?: string };

export function HostBreadcrumbs({ items }: { items: HostBreadcrumbItem[] }) {
  return (
    <>
      {items.map((item, index) => (
        <Fragment key={`${item.label}-${index}`}>
          {index > 0 && <span aria-hidden="true">›</span>}
          {item.href ? <Link className="text-primary" href={item.href}>{item.label}</Link> : <span>{item.label}</span>}
        </Fragment>
      ))}
    </>
  );
}
