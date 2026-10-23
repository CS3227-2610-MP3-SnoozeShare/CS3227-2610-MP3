'use client';

import { useCallback, useEffect, useState } from 'react';
import { listCategories, fileTicket } from '@/lib/guest-api';
import { BackendApiError } from '@/lib/api';
import { Modal } from '@/components/ui/modal';

export function TicketFilingDialog({
  bookingId,
  onClose,
  onFiled,
}: {
  bookingId: string | null;
  onClose: () => void;
  onFiled: () => void;
}) {
  const [categories, setCategories] = useState<string[]>([]);
  const [category, setCategory] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [requestedRemedy, setRequestedRemedy] = useState('');
  const [supportingText, setSupportingText] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!bookingId) return;
    listCategories().then((cats) => {
      setCategories(cats);
      if (cats.length > 0 && !category) setCategory(cats[0]);
    }).catch(() => {});
  }, [bookingId]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSubmit = useCallback(async () => {
    if (!bookingId || !title.trim() || !description.trim() || !requestedRemedy.trim()) return;
    setSubmitting(true);
    setError(null);
    try {
      await fileTicket({
        bookingId,
        category,
        title: title.trim(),
        description: description.trim(),
        requestedRemedy: requestedRemedy.trim(),
        supportingText: supportingText.trim() || undefined,
      });
      onFiled();
    } catch (e) {
      setError(e instanceof BackendApiError ? e.message : 'Failed to file ticket');
    } finally {
      setSubmitting(false);
    }
  }, [bookingId, category, title, description, requestedRemedy, supportingText, onFiled]);

  if (!bookingId) return null;

  return (
    <Modal
      open
      title="File a Dispute Ticket"
      description="Describe your issue and the resolution you are requesting."
      onClose={onClose}
      onConfirm={() => void handleSubmit()}
      confirmLabel={submitting ? 'Filing…' : 'File Ticket'}
      confirmTone="primary"
      confirmDisabled={submitting || !title.trim() || !description.trim() || !requestedRemedy.trim()}
    >
      <div className="mt-4 space-y-3">
        <div>
          <label className="text-xs font-semibold">Category</label>
          <select value={category} onChange={(e) => setCategory(e.target.value)}
            className="mt-1 w-full rounded-lg border border-input bg-background px-2 py-1.5 text-sm">
            {categories.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>
        <div>
          <label className="text-xs font-semibold">Title</label>
          <input type="text" value={title} onChange={(e) => setTitle(e.target.value)}
            maxLength={200} className="mt-1 w-full rounded-lg border border-input bg-background px-2 py-1.5 text-sm" />
        </div>
        <div>
          <label className="text-xs font-semibold">Description</label>
          <textarea value={description} onChange={(e) => setDescription(e.target.value)}
            maxLength={5000} rows={3} className="mt-1 w-full rounded-lg border border-input bg-background px-2 py-1.5 text-sm" />
        </div>
        <div>
          <label className="text-xs font-semibold">Requested remedy</label>
          <input type="text" value={requestedRemedy} onChange={(e) => setRequestedRemedy(e.target.value)}
            className="mt-1 w-full rounded-lg border border-input bg-background px-2 py-1.5 text-sm" />
        </div>
        <div>
          <label className="text-xs font-semibold">Supporting details (optional)</label>
          <textarea value={supportingText} onChange={(e) => setSupportingText(e.target.value)}
            maxLength={5000} rows={2} className="mt-1 w-full rounded-lg border border-input bg-background px-2 py-1.5 text-sm" />
        </div>
      </div>
      {error && <p className="mt-2 text-sm text-destructive" role="alert">{error}</p>}
    </Modal>
  );
}
