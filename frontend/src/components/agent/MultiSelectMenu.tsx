'use client';

import { useEffect, useId, useRef, useState } from 'react';

type Props = {
  options: string[];
  selected: string[];
  onChange: (selected: string[]) => void;
  allLabel: string;
  label: string;
};

/**
 * A drop-down of check boxes (the desktop MultiSelectMenu): click options to add or remove them, click outside or
 * press Escape to close. An empty selection means "everything": it shows the all label, one selection shows its
 * name, several show a count.
 */
export function MultiSelectMenu({ options, selected, onChange, allLabel, label }: Props) {
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

  const text = selected.length === 0 ? allLabel : selected.length === 1 ? selected[0] : `${selected.length} selected`;
  const toggle = (option: string) =>
    onChange(selected.includes(option) ? selected.filter((item) => item !== option) : [...selected, option]);
  const row = 'flex cursor-pointer items-center gap-2 px-3 py-1.5 text-xs hover:bg-muted';

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
      <button
        type="button"
        aria-label={label}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        onClick={() => setOpen((value) => !value)}
        className="box-border flex h-[34px] w-full items-center justify-between gap-2 rounded-lg border border-border bg-card px-3 text-left text-xs font-normal normal-case text-foreground"
      >
        <span className="truncate">{text}</span>
        <span aria-hidden className="text-[10px] text-[var(--text-subtle)]">▾</span>
      </button>
      {open && (
        <div
          id={listId}
          role="group"
          aria-label={`${label} options`}
          className="agent-scroll absolute left-0 top-full z-20 mt-1 max-h-[280px] min-w-full overflow-y-auto rounded-lg border border-border bg-card py-1 text-foreground shadow-[0_8px_24px_rgba(16,6,4,0.18)]"
        >
          <label className={`${row} font-semibold normal-case`}>
            <input type="checkbox" checked={selected.length === 0} onChange={() => onChange([])} />
            {allLabel}
          </label>
          {options.map((option) => (
            <label key={option} className={`${row} whitespace-nowrap font-normal normal-case`}>
              <input type="checkbox" checked={selected.includes(option)} onChange={() => toggle(option)} />
              {option}
            </label>
          ))}
        </div>
      )}
    </div>
  );
}
