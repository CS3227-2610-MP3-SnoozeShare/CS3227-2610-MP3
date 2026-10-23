'use client';

import { useCallback, useState } from 'react';
import { submitReview } from '@/lib/guest-api';
import { BackendApiError } from '@/lib/api';
import { Modal } from '@/components/ui/modal';

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
    <Modal
      open
      title="Leave a Review"
      description="Share your experience with this stay."
      onClose={onClose}
      onConfirm={() => void handleSubmit()}
      confirmLabel={submitting ? 'Submitting…' : 'Submit Review'}
      confirmTone="success"
      confirmDisabled={submitting || !comment.trim()}
    >
      <div className="mt-4 space-y-3">
        <div>
          <label className="text-sm font-medium">Rating</label>
          <div className="mt-1 flex gap-1">
            {[1, 2, 3, 4, 5].map((star) => (
              <button key={star} type="button" onClick={() => setRating(star)}
                className={`text-2xl ${star <= rating ? 'text-[var(--warning)]' : 'text-[var(--border-muted)]'}`}
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
            className="mt-1 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm" />
        </div>
      </div>
      {error && <p className="mt-2 text-sm text-destructive" role="alert">{error}</p>}
    </Modal>
  );
}
