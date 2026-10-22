'use client';

import { useEffect, useId, useRef, useState } from 'react';

type Option = { value: string; label: string };

type Props = {
  options: Option[];
  value: string;
  onChange: (value: string) => void;
  label: string;
  className?: string;
};

/** A single-choice drop-down in the agent look (the desktop combo box): same popup as the multi-select. */
export function SelectMenu({ options, value, onChange, label, className = '' }: Props) {
  const [open, setOpen] = useState(false);
  const root = useRef<HTMLDivElement>(null);
  const listId = useId();

  useEffect(() => {
    if (!open) return;
    const close = (event: MouseEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [open]);

  const current = options.find((option) => option.value === value) ?? options[0];
  return (
    <div
      ref={root}
      className={`relative ${className}`}
      onKeyDown={(event) => {
        if (event.key === 'Escape' && open) {
          event.stopPropagation();
          setOpen(false);
        }
      }}
    >
      <button
        type="button"
        aria-label={label}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        onClick={() => setOpen((state) => !state)}
        className="box-border flex h-7 w-full items-center justify-between gap-2 rounded-lg border border-border bg-card px-3 text-left text-xs text-[var(--text-muted)]"
      >
        <span className="truncate">{current?.label}</span>
        <span aria-hidden className="text-[10px] text-[var(--text-subtle)]">▾</span>
      </button>
      {open && (
        <div
          id={listId}
          role="listbox"
          aria-label={`${label} options`}
          className="agent-scroll absolute right-0 top-full z-20 mt-1 max-h-[280px] min-w-full overflow-y-auto rounded-lg border border-border bg-card py-1 shadow-[0_8px_24px_rgba(16,6,4,0.18)]"
        >
          {options.map((option) => (
            <button
              key={option.value}
              type="button"
              role="option"
              aria-selected={option.value === value}
              onClick={() => {
                onChange(option.value);
                setOpen(false);
              }}
              className={`block w-full whitespace-nowrap px-3 py-1.5 text-left text-xs hover:bg-muted ${
                option.value === value ? 'font-bold text-primary' : 'text-foreground'
              }`}
            >
              {option.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
