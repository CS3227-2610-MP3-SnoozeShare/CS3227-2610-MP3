'use client';

import { useEffect, useId, useRef, useState } from 'react';

type Props = {
  /** `yyyy-MM-dd`, or empty for no date. */
  value: string;
  onChange: (value: string) => void;
  label: string;
};

const WEEKDAYS = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'];
const pad = (value: number) => String(value).padStart(2, '0');
const iso = (year: number, month: number, day: number) => `${year}-${pad(month + 1)}-${pad(day)}`;

function parse(value: string): { year: number; month: number; day: number } | undefined {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  return match ? { year: Number(match[1]), month: Number(match[2]) - 1, day: Number(match[3]) } : undefined;
}

/** A date picker in the agent look (the desktop DatePicker): a field that opens a month grid. */
export function DateField({ value, onChange, label }: Props) {
  const selected = parse(value);
  const today = new Date();
  const [open, setOpen] = useState(false);
  const [view, setView] = useState({ year: selected?.year ?? today.getFullYear(), month: selected?.month ?? today.getMonth() });
  const root = useRef<HTMLDivElement>(null);
  const gridId = useId();

  useEffect(() => {
    if (!open) return;
    const close = (event: MouseEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [open]);

  const first = new Date(view.year, view.month, 1);
  const lead = first.getDay();
  const days = new Date(view.year, view.month + 1, 0).getDate();
  const cells: (number | null)[] = [...Array(lead).fill(null), ...Array.from({ length: days }, (_, index) => index + 1)];
  const monthName = first.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  const shift = (by: number) => {
    const next = new Date(view.year, view.month + by, 1);
    setView({ year: next.getFullYear(), month: next.getMonth() });
  };
  const shown = selected ? `${pad(selected.day)}/${pad(selected.month + 1)}/${selected.year}` : '';

  return (
    <div
      ref={root}
      className="relative"
      onKeyDown={(event) => {
        if (event.key === 'Escape' && open) {
          event.stopPropagation();
          setOpen(false);
        }
      }}
    >
      <div className="flex h-9 items-center rounded-lg border border-border bg-background text-[13px] font-normal normal-case text-foreground">
        <button
          type="button"
          aria-label={label}
          aria-haspopup="dialog"
          aria-expanded={open}
          aria-controls={gridId}
          onClick={() => setOpen((state) => !state)}
          className="h-full min-w-0 flex-1 truncate px-3 text-left"
        >
          {shown || <span className="text-[var(--text-subtle)]">dd/mm/yyyy</span>}
        </button>
        {value && (
          <button type="button" aria-label={`Clear ${label}`} onClick={() => onChange('')} className="h-full px-2 text-[var(--text-subtle)]">
            ×
          </button>
        )}
        <span aria-hidden className="pr-3 text-[10px] text-[var(--text-subtle)]">▾</span>
      </div>
      {open && (
        <div
          id={gridId}
          role="dialog"
          aria-label={`${label} calendar`}
          className="absolute left-0 top-full z-20 mt-1 w-[248px] rounded-lg border border-border bg-card p-3 shadow-[0_8px_24px_rgba(16,6,4,0.18)]"
        >
          <div className="mb-2 flex items-center justify-between text-xs font-bold">
            <button type="button" aria-label="Previous month" onClick={() => shift(-1)} className="rounded-md px-2 py-1 hover:bg-muted">‹</button>
            <span>{monthName}</span>
            <button type="button" aria-label="Next month" onClick={() => shift(1)} className="rounded-md px-2 py-1 hover:bg-muted">›</button>
          </div>
          <div className="grid grid-cols-7 gap-y-1 text-center text-[11px]">
            {WEEKDAYS.map((weekday) => (
              <span key={weekday} className="py-1 font-bold text-[var(--text-subtle)]">{weekday}</span>
            ))}
            {cells.map((day, index) =>
              day === null ? (
                <span key={`blank-${index}`} />
              ) : (
                <button
                  key={day}
                  type="button"
                  aria-label={iso(view.year, view.month, day)}
                  aria-pressed={selected?.year === view.year && selected.month === view.month && selected.day === day}
                  onClick={() => {
                    onChange(iso(view.year, view.month, day));
                    setOpen(false);
                  }}
                  className={`rounded-md py-1.5 hover:bg-muted ${
                    selected?.year === view.year && selected.month === view.month && selected.day === day
                      ? 'bg-primary font-bold text-primary-foreground hover:bg-primary'
                      : ''
                  }`}
                >
                  {day}
                </button>
              ),
            )}
          </div>
        </div>
      )}
    </div>
  );
}
