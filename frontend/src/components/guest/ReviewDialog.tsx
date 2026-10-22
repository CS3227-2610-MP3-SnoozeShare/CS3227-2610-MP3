'use client';

import { useCallback, useState } from 'react';
import { submitReview } from '@/lib/guest-api';
import { BackendApiError } from '@/lib/api';

export function ReviewDialog({
  bookingId,
  onClose,
  onReviewed,
}: {
  bookingId: string | null;
  onClose: () => void;
  onReviewed: () => void;
}) {
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = useCallback(async () => {
    if (!bookingId) return;
    setSubmitting(true);
    setError(null);
    try {
      await submitReview(bookingId, rating, comment);
      onReviewed();
    } catch (e) {
      setError(e instanceof BackendApiError ? e.message : 'Failed to submit review');
    } finally {
      setSubmitting(false);
    }
  }, [bookingId, rating, comment, onReviewed]);

  if (!bookingId) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center" role="dialog" aria-label="Leave review">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative z-10 w-full max-w-sm rounded-lg bg-background p-6 shadow-xl">
        <h3 className="text-lg font-semibold">Leave a Review</h3>
        <div className="mt-3 space-y-3">
          <div>
            <label className="text-sm font-medium">Rating</label>
            <div className="mt-1 flex gap-1">
              {[1, 2, 3, 4, 5].map((star) => (
                <button key={star} type="button" onClick={() => setRating(star)}
                  className={`text-2xl ${star <= rating ? 'text-yellow-500' : 'text-gray-300'}`}
                  aria-label={`${star} star${star > 1 ? 's' : ''}`}>
                  ★
                </button>
              ))}
            </div>
          </div>
          <div>
            <label className="text-sm font-medium">Comment</label>
            <textarea value={comment} onChange={(e) => setComment(e.target.value)}
              maxLength={2000} rows={4} placeholder="Share your experience..."
              className="mt-1 w-full rounded border px-3 py-2 text-sm" />
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex gap-2 pt-1">
            <button type="button" onClick={handleSubmit} disabled={submitting || !comment.trim()}
              className="rounded bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50">
              {submitting ? 'Submitting...' : 'Submit Review'}
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
