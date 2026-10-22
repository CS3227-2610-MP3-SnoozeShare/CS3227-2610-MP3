'use client';

import { useCallback, useEffect, useState } from 'react';
import { cancelPreview, cancelBooking, type CancellationRefund } from '@/lib/guest-api';
import { BackendApiError } from '@/lib/api';

export function CancelDialog({
  bookingId,
  onClose,
  onCancelled,
}: {
  bookingId: string | null;
  onClose: () => void;
  onCancelled: () => void;
}) {
  const [preview, setPreview] = useState<CancellationRefund | null>(null);
  const [loading, setLoading] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!bookingId) return;
    setLoading(true);
    setError(null);
    setPreview(null);
    cancelPreview(bookingId)
      .then(setPreview)
      .catch((e) => setError(e instanceof BackendApiError ? e.message : 'Failed to load'))
      .finally(() => setLoading(false));
  }, [bookingId]);

  const handleCancel = useCallback(async () => {
    if (!bookingId) return;
    setCancelling(true);
    setError(null);
    try {
      await cancelBooking(bookingId);
      onCancelled();
    } catch (e) {
      setError(e instanceof BackendApiError ? e.message : 'Cancellation failed');
    } finally {
      setCancelling(false);
    }
  }, [bookingId, onCancelled]);

  if (!bookingId) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center" role="dialog" aria-label="Cancel booking">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative z-10 w-full max-w-sm rounded-lg bg-background p-6 shadow-xl">
        <h3 className="text-lg font-semibold">Cancel Booking</h3>
        {loading && <p className="mt-3 text-sm text-muted-foreground">Loading refund details...</p>}
        {error && <p className="mt-3 text-sm text-red-600">{error}</p>}
        {preview && (
          <div className="mt-3 space-y-2">
            <p className="text-sm">Refund: <span className="font-semibold">${preview.amount.toFixed(2)}</span> ({preview.percent}%)</p>
            <div className="flex gap-2 pt-2">
              <button type="button" onClick={handleCancel} disabled={cancelling}
                className="rounded bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50">
                {cancelling ? 'Cancelling...' : 'Confirm Cancel'}
              </button>
              <button type="button" onClick={onClose}
                className="rounded border px-4 py-2 text-sm font-medium hover:bg-muted">
                Keep booking
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
