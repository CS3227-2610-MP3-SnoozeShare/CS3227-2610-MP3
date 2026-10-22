'use client';

import { useEffect, useId, useRef } from 'react';
import { outlineButton } from './agentUi';

const FOCUSABLE = 'button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [href], [tabindex]:not([tabindex="-1"])';

type Props = {
  title: string;
  subtitle?: string;
  width?: number;
  onClose: () => void;
  /** The confirming button, right of Cancel. */
  confirm: React.ReactNode;
  /** Optional control at the far left of the footer (for example Delete). */
  leftAction?: React.ReactNode;
  children?: React.ReactNode;
};

/** Modal dialog in the agent boards' look: named, focus-trapped, closes on Escape and Cancel, returns focus (N7, E59). */
export function AgentDialog({ title, subtitle, width = 500, onClose, confirm, leftAction, children }: Props) {
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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[rgba(16,6,4,0.45)]">
      <div
        ref={ref}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        onKeyDown={onKeyDown}
        style={{ width }}
        className="flex max-h-[calc(100vh-2rem)] max-w-[92vw] flex-col gap-3.5 overflow-y-auto rounded-xl bg-card p-[26px] shadow-[0_20px_60px_rgba(16,6,4,0.35)]"
      >
        <div className="flex flex-col gap-1.5">
          <h2 id={titleId} className="m-0 text-base font-extrabold">{title}</h2>
          {subtitle && <div className="text-xs text-[var(--text-subtle)]">{subtitle}</div>}
        </div>
        {children}
        <div className="flex items-center gap-2">
          {leftAction}
          <span className="flex-1" />
          <button type="button" className={`${outlineButton} px-[18px] py-[9px] text-xs`} onClick={onClose}>
            Cancel
          </button>
          {confirm}
        </div>
      </div>
    </div>
  );
}

/** Label above a dialog field, as in the boards. */
export function FieldLabel({ children }: { children: React.ReactNode }) {
  return <span className="mb-1 block text-[11px] font-bold text-[var(--text-muted)]">{children}</span>;
}

export const dialogInput = 'box-border w-full rounded-lg border border-border bg-background px-2.5 py-[9px] text-[13px]';
export const dialogArea = `${dialogInput} h-16 resize-none`;

export function confirmButton(tone: 'success' | 'danger' | 'accent', disabled?: boolean) {
  const bg = tone === 'success' ? 'bg-[var(--success)]' : tone === 'danger' ? 'bg-destructive' : 'bg-primary';
  return `rounded-lg px-[18px] py-[9px] text-xs font-bold text-[#fdf8f0] ${bg} ${disabled ? 'opacity-50' : ''}`;
}
