import { useEffect, useRef, type ReactNode } from 'react';

type ModalProps = {
  open: boolean;
  title: string;
  description: string;
  onClose: () => void;
  onConfirm: () => void;
  confirmLabel?: string;
  confirmTone?: 'primary' | 'success' | 'danger';
  layout?: 'default' | 'decision';
  summary?: ReactNode;
  children?: ReactNode;
};

export function Modal({ open, title, description, onClose, onConfirm, confirmLabel = 'Confirm', confirmTone = 'primary', layout = 'default', summary, children }: ModalProps) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const onCloseRef = useRef(onClose);
  const backdropPointerDown = useRef(false);
  onCloseRef.current = onClose;

  useEffect(() => {
    if (!open) return;
    const previouslyFocused = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const dialog = dialogRef.current;
    const focusable = dialog?.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])');
    (focusable?.[0] ?? dialog)?.focus();

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        onCloseRef.current();
        return;
      }
      if (event.key !== 'Tab' || !dialog) return;
      const elements = dialog.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])');
      if (!elements.length) {
        event.preventDefault();
        dialog.focus();
        return;
      }
      const first = elements[0];
      const last = elements[elements.length - 1];
      if (event.shiftKey && (document.activeElement === first || !dialog.contains(document.activeElement))) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && (document.activeElement === last || !dialog.contains(document.activeElement))) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      previouslyFocused?.focus();
    };
  }, [open]);

  if (!open) return null;
  const decisionLayout = layout === 'decision';
  const confirmStyle = confirmTone === 'success'
    ? 'bg-[var(--success)] text-white'
    : confirmTone === 'danger'
      ? 'bg-destructive text-white'
      : 'bg-primary text-primary-foreground';
  return (
    <div
      className={`fixed inset-0 z-50 flex items-center justify-center p-4 ${decisionLayout ? 'bg-black/40' : 'bg-black/30'}`}
      onPointerDown={(event) => { backdropPointerDown.current = event.target === event.currentTarget; }}
      onClick={(event) => {
        if (event.target === event.currentTarget && backdropPointerDown.current) onClose();
        backdropPointerDown.current = false;
      }}
      role="presentation"
    >
      <div ref={dialogRef} aria-describedby="modal-description" aria-labelledby="modal-title" aria-modal="true" className={`ss-card w-full ${decisionLayout ? 'max-h-[calc(100vh-2rem)] max-w-[31rem] overflow-y-auto rounded-xl p-6' : 'max-w-md p-5'}`} role="dialog" tabIndex={-1}>
        <h2 id="modal-title" className={decisionLayout ? 'text-base font-bold' : 'text-lg font-semibold'}>{title}</h2>
        {decisionLayout && summary}
        <p id="modal-description" className={decisionLayout ? 'mt-4 rounded-md bg-[var(--surface-inset)] px-3 py-2 text-xs leading-tight text-foreground' : 'mt-2 text-sm text-[var(--text-subtle)]'}>{description}</p>
        {children}
        <div className={`${decisionLayout ? 'mt-4' : 'mt-5'} flex justify-end gap-2`}>
          <button className="cursor-pointer rounded-md border border-border px-3 py-2 text-sm" onClick={onClose} type="button">Cancel</button>
          <button className={`cursor-pointer rounded-md px-3 py-2 text-sm font-semibold ${confirmStyle}`} onClick={onConfirm} type="button">{confirmLabel}</button>
        </div>
      </div>
    </div>
  );
}
