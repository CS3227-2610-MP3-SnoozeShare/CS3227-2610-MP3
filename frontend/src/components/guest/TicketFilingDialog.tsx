'use client';

import { useCallback, useEffect, useState } from 'react';
import { listCategories, fileTicket } from '@/lib/guest-api';
import { BackendApiError } from '@/lib/api';

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
    <div className="fixed inset-0 z-50 flex items-center justify-center" role="dialog" aria-label="File a ticket">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative z-10 w-full max-w-md rounded-lg bg-background p-6 shadow-xl">
        <h3 className="text-lg font-semibold">File a Dispute Ticket</h3>
        <div className="mt-3 space-y-3">
          <div>
            <label className="text-sm font-medium">Category</label>
            <select value={category} onChange={(e) => setCategory(e.target.value)}
              className="mt-1 w-full rounded border px-2 py-1.5 text-sm">
              {categories.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
          <div>
            <label className="text-sm font-medium">Title</label>
            <input type="text" value={title} onChange={(e) => setTitle(e.target.value)}
              maxLength={200} className="mt-1 w-full rounded border px-2 py-1.5 text-sm" />
          </div>
          <div>
            <label className="text-sm font-medium">Description</label>
            <textarea value={description} onChange={(e) => setDescription(e.target.value)}
              maxLength={5000} rows={3} className="mt-1 w-full rounded border px-2 py-1.5 text-sm" />
          </div>
          <div>
            <label className="text-sm font-medium">Requested remedy</label>
            <input type="text" value={requestedRemedy} onChange={(e) => setRequestedRemedy(e.target.value)}
              className="mt-1 w-full rounded border px-2 py-1.5 text-sm" />
          </div>
          <div>
            <label className="text-sm font-medium">Supporting details (optional)</label>
            <textarea value={supportingText} onChange={(e) => setSupportingText(e.target.value)}
              maxLength={5000} rows={2} className="mt-1 w-full rounded border px-2 py-1.5 text-sm" />
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex gap-2 pt-1">
            <button type="button" onClick={handleSubmit}
              disabled={submitting || !title.trim() || !description.trim() || !requestedRemedy.trim()}
              className="rounded bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50">
              {submitting ? 'Filing...' : 'File Ticket'}
            </button>
            <button type="button" onClick={onClose}
              className="rounded border px-4 py-2 text-sm font-medium hover:bg-muted">
              Cancel
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
