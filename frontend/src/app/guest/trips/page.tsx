'use client';

import { useCallback, useEffect, useState } from 'react';
import { listTrips, type GuestTrip } from '@/lib/guest-api';
import { BackendApiError } from '@/lib/api';
import { TripCard } from '@/components/guest/TripCard';
import { CancelDialog } from '@/components/guest/CancelDialog';
import { ReviewDialog } from '@/components/guest/ReviewDialog';

type TripGroup = { upcoming: GuestTrip[]; active: GuestTrip[]; past: GuestTrip[] };

function groupTrips(trips: GuestTrip[]): TripGroup {
  const today = new Date().toISOString().split('T')[0];
  const upcoming: GuestTrip[] = [];
  const active: GuestTrip[] = [];
  const past: GuestTrip[] = [];

  for (const t of trips) {
    const { status, startDate, endDate } = t.booking;
    if (
      (status === 'PENDING' || status === 'CONFIRMED') &&
      startDate > today
    ) {
      upcoming.push(t);
    } else if (status === 'CONFIRMED' && startDate <= today && endDate >= today) {
      active.push(t);
    } else {
      past.push(t);
    }
  }
  return { upcoming, active, past };
}

function TripSection({ title, trips, defaultOpen, onCancel, onReview }: {
  title: string;
  trips: GuestTrip[];
  defaultOpen: boolean;
  onCancel: (id: string) => void;
  onReview: (id: string) => void;
}) {
  const [open, setOpen] = useState(defaultOpen);
  if (trips.length === 0) return null;

  return (
    <section data-testid={`section-${title.toLowerCase()}`}>
      <button type="button" onClick={() => setOpen(!open)}
        className="flex w-full items-center gap-2 text-left text-lg font-semibold">
        <span className={`inline-block transition-transform ${open ? 'rotate-90' : ''}`}>▶</span>
        {title} ({trips.length})
      </button>
      {open && (
        <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {trips.map((t) => (
            <TripCard key={t.booking.id} trip={t} onCancel={onCancel} onReview={onReview} />
          ))}
        </div>
      )}
    </section>
  );
}

export default function TripsPage() {
  const [trips, setTrips] = useState<GuestTrip[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancelId, setCancelId] = useState<string | null>(null);
  const [reviewId, setReviewId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setTrips(await listTrips());
    } catch (e) {
      setError(e instanceof BackendApiError ? e.message : 'Failed to load trips');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const grouped = groupTrips(trips);

  return (
    <main aria-label="My trips" className="min-h-[calc(100vh-4rem)] p-6">
      <h2 className="mb-6 text-2xl font-bold">My Trips</h2>

      {loading && <p className="text-muted-foreground">Loading trips...</p>}
      {error && <p className="text-red-600">{error}</p>}
      {!loading && !error && trips.length === 0 && (
        <p data-testid="empty-state" className="text-muted-foreground">No trips yet. Search for a listing to book your first stay!</p>
      )}

      {!loading && !error && trips.length > 0 && (
        <div data-testid="trip-sections" className="space-y-6">
          <TripSection title="Upcoming" trips={grouped.upcoming} defaultOpen onCancel={setCancelId} onReview={setReviewId} />
          <TripSection title="Active" trips={grouped.active} defaultOpen onCancel={setCancelId} onReview={setReviewId} />
          <TripSection title="Past" trips={grouped.past} defaultOpen={false} onCancel={setCancelId} onReview={setReviewId} />
        </div>
      )}

      <CancelDialog bookingId={cancelId} onClose={() => setCancelId(null)} onCancelled={() => { setCancelId(null); load(); }} />
      <ReviewDialog bookingId={reviewId} onClose={() => setReviewId(null)} onReviewed={() => { setReviewId(null); load(); }} />
    </main>
  );
}
