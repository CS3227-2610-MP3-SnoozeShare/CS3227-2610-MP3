'use client';

import { useCallback, useEffect, useState } from 'react';
import { cancelPreview, cancelBooking, type CancellationRefund } from '@/lib/guest-api';
import { BackendApiError } from '@/lib/api';
import { Modal } from '@/components/ui/modal';

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
    <Modal
      open
      title="Cancel Booking"
      description={loading ? 'Loading refund details…' : preview ? `Refund: $${preview.amount.toFixed(2)} (${preview.percent}% of booking total)` : error ?? 'Unable to load refund details'}
      onClose={onClose}
      onConfirm={() => void handleCancel()}
      confirmLabel={cancelling ? 'Cancelling…' : 'Confirm Cancel'}
      confirmTone="danger"
      confirmDisabled={cancelling || loading || !preview}
    >
      {error && <p className="mt-2 text-sm text-destructive" role="alert">{error}</p>}
    </Modal>
  );
}
