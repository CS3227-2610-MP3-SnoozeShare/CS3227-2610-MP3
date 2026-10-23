'use client';

import type { GuestTrip } from '@/lib/guest-api';

const statusLabels: Record<string, { label: string; className: string }> = {
  PENDING: { label: 'Pending', className: 'ss-status-pending' },
  CONFIRMED: { label: 'Confirmed', className: 'ss-status-active' },
  COMPLETED: { label: 'Completed', className: 'ss-status-success' },
  REJECTED: { label: 'Rejected', className: 'ss-status-danger' },
  CANCELLED_BY_GUEST: { label: 'Cancelled', className: 'ss-status-neutral' },
  CANCELLED_BY_HOST: { label: 'Cancelled by host', className: 'ss-status-warning' },
  FORCE_CANCELLED: { label: 'Force cancelled', className: 'ss-status-danger' },
  FORCE_COMPLETED: { label: 'Force completed', className: 'ss-status-warning' },
};

export function TripCard({
  trip,
  onCancel,
  onReview,
}: {
  trip: GuestTrip;
  onCancel: (bookingId: string) => void;
  onReview: (bookingId: string) => void;
}) {
  const b = trip.booking;
  const status = statusLabels[b.status] ?? { label: b.status, className: 'ss-status-neutral' };

  return (
    <div className="ss-card flex flex-col gap-2 p-4">
      <div className="flex items-start justify-between">
        <div>
          <h4 className="font-semibold">{trip.listingTitle}</h4>
          <p className="text-sm text-muted-foreground">Host: {trip.hostDisplayName}</p>
        </div>
        <span className={`ss-pill ${status.className} text-xs`}>{status.label}</span>
      </div>
      <div className="text-sm text-muted-foreground">
        {b.startDate} → {b.endDate} · ${b.totalAmount.toFixed(2)}
      </div>
      <div className="flex flex-wrap gap-2 pt-1">
        {trip.canCancel && (
          <button type="button" onClick={() => onCancel(b.id)}
            className="rounded-lg border border-destructive/30 px-3 py-1 text-xs font-semibold text-destructive hover:bg-destructive/5">
            Cancel
          </button>
        )}
        {!trip.hasReview && (b.status === 'COMPLETED' || b.status === 'FORCE_COMPLETED') && (
          <button type="button" onClick={() => onReview(b.id)}
            className="rounded-lg border border-primary/30 px-3 py-1 text-xs font-semibold text-primary hover:bg-primary/5">
            Leave review
          </button>
        )}
        {trip.hasReview && (
          <span className="text-xs text-muted-foreground">Reviewed</span>
        )}
      </div>
    </div>
  );
}
