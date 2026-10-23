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
const isoOf = (date: Date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

function parse(value: string): Date | undefined {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  return match ? new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3])) : undefined;
}

function CalendarIcon({ open }: { open: boolean }) {
  return (
    <svg aria-hidden width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.4" className={open ? 'text-primary' : 'text-[var(--text-subtle)]'}>
      <rect x="2" y="3" width="12" height="11" rx="2" />
      <path d="M2 6.5h12M5.5 1.5v3M10.5 1.5v3" />
    </svg>
  );
}

/**
 * The agent date picker (board component "Date picker", desktop `.date-picker`): one bordered field with a calendar
 * icon, and a 12px card with an accent border holding a month header, 34px day cells, a ringed today, a filled
 * selected day and muted days of the neighbouring months. A click on a day selects it and closes the card.
 */
export function DateField({ value, onChange, label }: Props) {
  const selected = parse(value);
  const [open, setOpen] = useState(false);
  const [month, setMonth] = useState(() => {
    const base = selected ?? new Date();
    return new Date(base.getFullYear(), base.getMonth(), 1);
  });
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

  const todayIso = isoOf(new Date());
  const selectedIso = selected ? isoOf(selected) : '';
  // Always six week rows, starting on the Sunday on or before the 1st.
  const start = new Date(month.getFullYear(), month.getMonth(), 1 - month.getDay());
  const days = Array.from({ length: 42 }, (_, index) => new Date(start.getFullYear(), start.getMonth(), start.getDate() + index));
  const title = month.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  const shift = (by: number) => setMonth(new Date(month.getFullYear(), month.getMonth() + by, 1));
  const shown = selected ? `${pad(selected.getDate())}/${pad(selected.getMonth() + 1)}/${selected.getFullYear()}` : '';

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
      <div
        className={`flex h-[34px] items-center rounded-lg border bg-card text-xs font-normal normal-case text-foreground ${
          open ? 'border-primary' : 'border-border'
        }`}
      >
        <button
          type="button"
          aria-label={label}
          aria-haspopup="dialog"
          aria-expanded={open}
          aria-controls={gridId}
          onClick={() => setOpen((state) => !state)}
          className="h-full min-w-0 flex-1 truncate pl-3 text-left"
        >
          {shown || <span className="text-[var(--text-subtle)]">dd/mm/yyyy</span>}
        </button>
        {value && (
          <button type="button" aria-label={`Clear ${label}`} onClick={() => onChange('')} className="h-full px-1.5 text-sm text-[var(--text-subtle)] hover:text-primary">
            ×
          </button>
        )}
        <span className="flex h-full items-center pl-1.5 pr-3">
          <CalendarIcon open={open} />
        </span>
      </div>
      {open && (
        <div
          id={gridId}
          role="dialog"
          aria-label={`${label} calendar`}
          className="absolute left-0 top-full z-20 mt-1 w-[266px] rounded-xl border border-primary bg-card p-3.5 text-foreground shadow-[0_4px_14px_rgba(16,6,4,0.08)]"
        >
          <div className="flex items-center justify-between pb-2.5">
            <button type="button" aria-label="Previous month" onClick={() => shift(-1)} className="size-7 rounded-lg border border-[var(--border-muted)] bg-card text-sm text-[var(--text-muted)] hover:bg-muted">
              ‹
            </button>
            <span className="px-1.5 text-[13px] font-extrabold normal-case">{title}</span>
            <button type="button" aria-label="Next month" onClick={() => shift(1)} className="size-7 rounded-lg border border-[var(--border-muted)] bg-card text-sm text-[var(--text-muted)] hover:bg-muted">
              ›
            </button>
          </div>
          <div className="grid grid-cols-7 gap-0.5">
            {WEEKDAYS.map((weekday) => (
              <span key={weekday} className="flex h-6 items-center justify-center text-[10px] font-bold normal-case text-[var(--text-subtle)]">
                {weekday}
              </span>
            ))}
            {days.map((day) => {
              const iso = isoOf(day);
              const inMonth = day.getMonth() === month.getMonth();
              const isSelected = iso === selectedIso;
              const isToday = iso === todayIso;
              return (
                <button
                  key={iso}
                  type="button"
                  aria-label={iso}
                  aria-pressed={isSelected}
                  onClick={() => {
                    onChange(iso);
                    setOpen(false);
                  }}
                  className={`h-[34px] rounded-lg border text-xs font-medium normal-case ${
                    isSelected
                      ? 'border-primary bg-primary font-bold text-primary-foreground'
                      : `${isToday ? 'border-primary font-bold' : 'border-transparent'} hover:bg-muted ${inMonth ? '' : 'text-[var(--text-subtle)] opacity-45'}`
                  }`}
                >
                  {day.getDate()}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
