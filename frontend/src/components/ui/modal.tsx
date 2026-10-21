import type { ReactNode } from 'react';

type ModalProps = {
  open: boolean;
  title: string;
  description: string;
  onClose: () => void;
  onConfirm: () => void;
  confirmLabel?: string;
  children?: ReactNode;
};

export function Modal({ open, title, description, onClose, onConfirm, confirmLabel = 'Confirm', children }: ModalProps) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4" role="presentation">
      <div aria-describedby="modal-description" aria-labelledby="modal-title" aria-modal="true" className="ss-card w-full max-w-md p-5" role="dialog">
        <h2 id="modal-title" className="text-lg font-semibold">{title}</h2>
        <p id="modal-description" className="mt-2 text-sm text-[var(--text-subtle)]">{description}</p>
        {children}
        <div className="mt-5 flex justify-end gap-2">
          <button className="rounded-lg border border-border px-3 py-2 text-sm" onClick={onClose} type="button">Cancel</button>
          <button className="rounded-lg bg-primary px-3 py-2 text-sm font-semibold text-primary-foreground" onClick={onConfirm} type="button">{confirmLabel}</button>
        </div>
      </div>
    </div>
  );
}
