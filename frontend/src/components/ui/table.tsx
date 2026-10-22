import type { HTMLAttributes, TableHTMLAttributes } from 'react';
import { cn } from 'cn';

export function Table({ className, scrollX = true, ...props }: TableHTMLAttributes<HTMLTableElement> & { scrollX?: boolean }) {
  return <div className={cn('w-full', scrollX ? 'overflow-x-auto' : 'overflow-visible')}><table className={cn('w-full text-left text-sm', className)} {...props} /></div>;
}

export function Thead({ className, ...props }: HTMLAttributes<HTMLTableSectionElement>) {
  return <thead className={cn('border-b border-[var(--border-muted)] text-xs uppercase tracking-wide text-[var(--text-subtle)]', className)} {...props} />;
}

export function Tbody({ className, ...props }: HTMLAttributes<HTMLTableSectionElement>) {
  return <tbody className={cn('[&_tr+tr]:border-t [&_tr+tr]:border-[var(--border-muted)]', className)} {...props} />;
}

export function Tr({ className, ...props }: HTMLAttributes<HTMLTableRowElement>) {
  return <tr className={cn('align-middle', className)} {...props} />;
}

export function Th({ className, ...props }: HTMLAttributes<HTMLTableCellElement>) {
  return <th className={cn('px-3 py-2.5 font-semibold', className)} {...props} />;
}

export function Td({ className, ...props }: HTMLAttributes<HTMLTableCellElement>) {
  return <td className={cn('px-3 py-3', className)} {...props} />;
}
