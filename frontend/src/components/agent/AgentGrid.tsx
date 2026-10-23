'use client';

import { createContext, useContext, useLayoutEffect, useRef, useState } from 'react';
import { cn } from 'cn';

export type GridColumn = {
  label: string;
  /** Narrowest width in px before the table scrolls sideways. */
  min: number;
  /** Share of the spare width. */
  fr: number;
};

const TemplateContext = createContext('');

/**
 * The table of every agent screen. Columns share the full width of the page; each has a minimum width, and only when
 * the minimums together exceed the page does the table scroll sideways. The header row stays put and the rows scroll
 * under it, so the slim vertical scroll bar starts below the header (the desktop table behaviour); the header gets the
 * bar's width as padding so its columns stay aligned with the rows.
 */
export function AgentGrid({ columns, label, children }: { columns: GridColumn[]; label: string; children: React.ReactNode }) {
  const body = useRef<HTMLDivElement>(null);
  const content = useRef<HTMLDivElement>(null);
  const [gutter, setGutter] = useState(0);
  const template = columns.map((column) => `minmax(${column.min}px, ${column.fr}fr)`).join(' ');
  const minWidth = columns.reduce((sum, column) => sum + column.min, 0);

  useLayoutEffect(() => {
    const element = body.current;
    if (!element) return;
    const measure = () => setGutter(element.offsetWidth - element.clientWidth);
    measure();
    if (typeof ResizeObserver === 'undefined') return;
    const observer = new ResizeObserver(measure);
    observer.observe(element);
    if (content.current) observer.observe(content.current);
    return () => observer.disconnect();
  }, []);

  return (
    <TemplateContext.Provider value={template}>
      <div className="agent-scroll overflow-x-auto rounded-xl border border-border bg-card">
        <div role="table" aria-label={label} style={{ minWidth: minWidth + gutter }} className="flex flex-col">
          <div role="row" className="grid bg-muted" style={{ gridTemplateColumns: template, paddingRight: gutter }}>
            {columns.map((column) => (
              <div key={column.label} role="columnheader" className="whitespace-nowrap px-[18px] py-2.5 text-[11px] font-bold uppercase text-[var(--text-subtle)]">
                {column.label}
              </div>
            ))}
          </div>
          <div ref={body} role="rowgroup" className="agent-scroll max-h-[max(14rem,calc(100vh-17rem))] overflow-y-auto overflow-x-hidden">
            <div ref={content}>{children}</div>
          </div>
        </div>
      </div>
    </TemplateContext.Provider>
  );
}

export function GridRow({ className, children }: { className?: string; children: React.ReactNode }) {
  const template = useContext(TemplateContext);
  return (
    <div role="row" className={cn('grid items-center border-t border-[var(--border-muted)]', className)} style={{ gridTemplateColumns: template }}>
      {children}
    </div>
  );
}

export function Cell({ className, children }: { className?: string; children?: React.ReactNode }) {
  return (
    <div role="cell" className={cn('min-w-0 break-words px-[18px] py-3.5 text-[13px]', className)}>
      {children}
    </div>
  );
}

/** One message spanning every column: loading and empty states. */
export function GridMessage({ children }: { children: React.ReactNode }) {
  return (
    <GridRow className="block">
      <Cell className="text-[var(--text-subtle)]">{children}</Cell>
    </GridRow>
  );
}
