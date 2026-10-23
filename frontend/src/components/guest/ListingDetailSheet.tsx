'use client';

import { useCallback, useEffect, useState } from 'react';
import type { PropertyDetail, PriceBreakdown } from '@/lib/guest-api';
import { getListingDetail, estimateCost, createBooking } from '@/lib/guest-api';
import { BackendApiError } from '@/lib/api';

export function ListingDetailSheet({
  listingId,
  onClose,
}: {
  listingId: string | null;
  onClose: () => void;
}) {
  const [detail, setDetail] = useState<PropertyDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [estimate, setEstimate] = useState<PriceBreakdown | null>(null);
  const [estimateError, setEstimateError] = useState<string | null>(null);

  const [booking, setBooking] = useState(false);
  const [bookingSuccess, setBookingSuccess] = useState(false);
  const [bookingError, setBookingError] = useState<string | null>(null);

  useEffect(() => {
    if (!listingId) return;
    setLoading(true);
    setError(null);
    setDetail(null);
    setEstimate(null);
    setBookingSuccess(false);
    setBookingError(null);
    getListingDetail(listingId)
      .then(setDetail)
      .catch((e) => setError(e instanceof BackendApiError ? e.message : 'Failed to load'))
      .finally(() => setLoading(false));
  }, [listingId]);

  const handleEstimate = useCallback(async () => {
    if (!listingId || !startDate || !endDate) return;
    setEstimateError(null);
    try {
      const result = await estimateCost(listingId, startDate, endDate);
      setEstimate(result);
    } catch (e) {
      setEstimateError(e instanceof BackendApiError ? e.message : 'Failed to estimate');
    }
  }, [listingId, startDate, endDate]);

  const handleBook = useCallback(async () => {
    if (!listingId || !startDate || !endDate) return;
    setBooking(true);
    setBookingError(null);
    try {
      await createBooking(listingId, startDate, endDate);
      setBookingSuccess(true);
    } catch (e) {
      setBookingError(e instanceof BackendApiError ? e.message : 'Booking failed');
    } finally {
      setBooking(false);
    }
  }, [listingId, startDate, endDate]);

  if (!listingId) return null;

  const today = new Date().toISOString().split('T')[0];

  return (
    <div className="fixed inset-0 z-50 flex justify-end" role="dialog" aria-label="Listing detail">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative z-10 flex h-full w-full max-w-lg flex-col overflow-y-auto bg-background shadow-xl">
        <div className="flex items-center justify-between border-b p-4">
          <h2 className="text-lg font-semibold">Listing Detail</h2>
          <button type="button" onClick={onClose} className="text-muted-foreground hover:text-foreground">✕</button>
        </div>

        <div className="flex-1 space-y-6 p-4">
          {loading && <p className="text-muted-foreground">Loading...</p>}
          {error && <p className="text-destructive">{error}</p>}

          {detail && (
            <>
              <div>
                <h3 className="text-xl font-bold">{detail.property.title}</h3>
                <p className="mt-1 text-sm text-muted-foreground">{detail.property.city} · {detail.property.district}</p>
                <p className="mt-2 text-sm">{detail.property.description}</p>
              </div>

              <div className="grid grid-cols-2 gap-3 text-sm">
                <div><span className="font-medium">Type:</span> {detail.property.propertyType}</div>
                <div><span className="font-medium">Guests:</span> Up to {detail.property.maxGuests}</div>
                <div><span className="font-medium">Bedrooms:</span> {detail.property.bedrooms}</div>
                <div><span className="font-medium">Bathrooms:</span> {detail.property.bathrooms}</div>
                <div><span className="font-medium">Check-in:</span> {detail.property.checkInTime}</div>
                <div><span className="font-medium">Check-out:</span> {detail.property.checkOutTime}</div>
              </div>

              <div className="ss-card p-3">
                <p className="text-sm font-medium">Host: {detail.hostDisplayName}</p>
                <p className="text-xs text-muted-foreground">Hosting since {detail.hostingSinceYear}</p>
              </div>

              <div className="text-2xl font-bold text-primary">
                ${detail.property.baseNightlyRate.toFixed(0)} <span className="text-sm font-normal text-muted-foreground">/ night</span>
              </div>

              {/* Reviews */}
              {detail.reviewCount > 0 && (
                <div>
                  <h4 className="font-semibold">Reviews ({detail.reviewCount}) · {detail.averageRating.toFixed(1)}★</h4>
                  <div className="mt-2 space-y-2">
                    {detail.reviews.map((r, i) => (
                      <div key={i} className="ss-card p-3 text-sm">
                        <div className="flex items-center justify-between">
                          <span className="font-medium">{r.guestDisplayName}</span>
                          <span className="text-primary">{r.rating}★</span>
                        </div>
                        <p className="mt-1 text-muted-foreground">{r.comment}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}
              {detail.reviewCount === 0 && (
                <p className="text-sm text-muted-foreground">No reviews yet.</p>
              )}

              {/* Price estimator & booking */}
              <div className="ss-card space-y-3 p-4">
                <h4 className="font-semibold">Book this listing</h4>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-muted-foreground">Check-in</label>
                    <input type="date" min={today} value={startDate} onChange={(e) => { setStartDate(e.target.value); setEstimate(null); setBookingSuccess(false); }}
                      className="mt-1 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm" />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-muted-foreground">Check-out</label>
                    <input type="date" min={startDate || today} value={endDate} onChange={(e) => { setEndDate(e.target.value); setEstimate(null); setBookingSuccess(false); }}
                      className="mt-1 w-full rounded-lg border border-input bg-background px-2 py-1 text-sm" />
                  </div>
                </div>
                <button type="button" onClick={handleEstimate} disabled={!startDate || !endDate}
                  className="w-full rounded-lg border border-[var(--border-muted)] bg-card px-4 py-2 text-sm font-semibold hover:bg-[var(--surface-inset)] disabled:opacity-50">
                  Estimate price
                </button>
                {estimateError && <p className="text-sm text-destructive">{estimateError}</p>}
                {estimate && (
                  <div className="rounded-lg bg-[var(--surface-inset)] p-3 text-sm">
                    <p>{estimate.nights} night{estimate.nights !== 1 ? 's' : ''} × ${estimate.nightlyRate.toFixed(0)}</p>
                    <p className="mt-1 text-lg font-bold">Total: ${estimate.totalAmount.toFixed(2)}</p>
                  </div>
                )}
                {estimate && !bookingSuccess && (
                  <button type="button" onClick={handleBook} disabled={booking}
                    className="w-full rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50">
                    {booking ? 'Booking...' : 'Confirm Booking'}
                  </button>
                )}
                {bookingError && <p className="text-sm text-destructive">{bookingError}</p>}
                {bookingSuccess && <p className="text-sm font-medium text-[var(--success)]">Booking submitted! Check your trips.</p>}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
