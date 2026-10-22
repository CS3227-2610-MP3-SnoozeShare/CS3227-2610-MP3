'use client';

import { useEffect, useId, useRef } from 'react';

const FOCUSABLE = 'button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [href], [tabindex]:not([tabindex="-1"])';

type Props = { title: string; onClose: () => void; children: React.ReactNode };

/** Modal dialog: named, focus-trapped, closes on Escape and Cancel, returns focus to the opener (N7, E59). */
export function AgentDialog({ title, onClose, children }: Props) {
  const titleId = useId();
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const opener = document.activeElement as HTMLElement | null;
    const first = ref.current?.querySelector<HTMLElement>(FOCUSABLE);
    first?.focus();
    return () => opener?.focus();
  }, []);

  function onKeyDown(event: React.KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape') {
      event.stopPropagation();
      onClose();
      return;
    }
    if (event.key !== 'Tab') return;
    const items = Array.from(ref.current?.querySelectorAll<HTMLElement>(FOCUSABLE) ?? []);
    if (items.length === 0) return;
    const firstItem = items[0];
    const lastItem = items[items.length - 1];
    if (event.shiftKey && document.activeElement === firstItem) {
      event.preventDefault();
      lastItem.focus();
    } else if (!event.shiftKey && document.activeElement === lastItem) {
      event.preventDefault();
      firstItem.focus();
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45">
      <div
        ref={ref}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        onKeyDown={onKeyDown}
        className="flex w-[500px] max-w-[92vw] flex-col gap-3 rounded-xl bg-card p-6 shadow-xl"
      >
        <h2 id={titleId} className="text-base font-extrabold">{title}</h2>
        {children}
        <div className="flex justify-end">
          <button type="button" className="rounded-md border border-[var(--border)] px-4 py-2 text-xs font-semibold" onClick={onClose}>
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}
