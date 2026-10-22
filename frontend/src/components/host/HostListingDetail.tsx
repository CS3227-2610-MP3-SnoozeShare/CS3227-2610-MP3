import type { ListingDetail } from './hostTypes';
import { StatusPill } from '@/components/ui/status-pill';

export function HostListingDetail({ listing }: { listing: ListingDetail }) {
  const metrics = [
    ['MAX GUESTS', listing.maxGuests],
    ['BEDROOMS', listing.bedrooms],
    ['BATHROOMS', listing.bathrooms],
    ['PER NIGHT', `$${listing.rate}`],
    ['CHECK-IN', listing.checkIn],
    ['CHECK-OUT', listing.checkOut],
  ] as const;

  return (
    <div className="space-y-3">
      <div
        aria-label={`${listing.title} cover image`}
        className="h-40 w-full rounded-xl sm:h-44"
        role="img"
        style={{ backgroundImage: 'linear-gradient(110deg, #cf754d 0%, #b59a7e 100%)' }}
      />

      <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,3fr)_minmax(280px,2fr)]">
        <div className="space-y-3">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-2xl font-bold">{listing.title}</h1>
              <span className="ss-pill ss-status-neutral">{listing.type.toUpperCase()}</span>
              <StatusPill status={listing.status} />
            </div>
            <p className="mt-0.5 text-sm text-[var(--text-subtle)]">{listing.address}, {listing.city}, {listing.region} {listing.postalCode}</p>
          </div>

          <dl className="grid grid-cols-2 gap-x-3 gap-y-3 rounded-lg bg-[var(--surface-inset)] p-3 sm:grid-cols-3 md:grid-cols-6">
            {metrics.map(([label, value]) => (
              <div className="flex min-w-0 flex-col" key={label}>
                <dt className="order-2 mt-0.5 text-[10px] font-medium uppercase tracking-wide text-[var(--text-subtle)]">{label}</dt>
                <dd className="order-1 text-base font-semibold leading-tight">{value}</dd>
              </div>
            ))}
          </dl>

          <section aria-labelledby="listing-description-heading">
            <h2 className="text-base font-semibold" id="listing-description-heading">Description</h2>
            <p className="mt-2 text-sm leading-6 text-[var(--text-muted)]">{listing.description}</p>
          </section>

          <section aria-labelledby="listing-amenities-heading">
            <h3 className="text-xs font-bold tracking-wide text-[var(--text-subtle)]" id="listing-amenities-heading">AMENITIES</h3>
            <div className="mt-1.5 flex flex-wrap gap-1.5">
              {listing.amenities.map((amenity) => <span className="rounded-full bg-[var(--surface-inset)] px-2.5 py-1 text-xs" key={amenity}>{amenity}</span>)}
            </div>
          </section>

          <section aria-labelledby="listing-reviews-heading" className="space-y-1">
            <h3 className="text-xs font-bold tracking-wide text-[var(--text-subtle)]" id="listing-reviews-heading">REVIEWS · {listing.rating.toFixed(1)} ★ ({listing.reviewCount})</h3>
            <ul className="space-y-1">
              {listing.reviews.map((review) => (
                <li className="flex flex-wrap items-baseline gap-x-1 text-xs leading-4" key={review.author}>
                  <strong>{review.author}</strong>
                  <span aria-label={`${review.rating} out of 5 stars`}>{'★'.repeat(review.rating)}</span>
                  <span aria-hidden="true">—</span>
                  <span>“{review.text}”</span>
                </li>
              ))}
            </ul>
          </section>
        </div>

        <aside aria-labelledby="listing-performance-heading" className="ss-card self-start p-4 lg:col-start-2 lg:row-start-1" aria-label="Performance">
          <h2 className="text-base font-semibold" id="listing-performance-heading">Performance</h2>
          <dl className="mt-2 space-y-1.5 text-sm">
            {[
              ['Total bookings', listing.bookings],
              ['Occupancy (30d)', `${listing.occupancy30d.toFixed(2)}%`],
              ['Earnings (30d)', `$${listing.earnings30d.toLocaleString('en-US', { minimumFractionDigits: 2 })}`],
            ].map(([label, value]) => (
              <div className="flex justify-between gap-3" key={String(label)}>
                <dt className="text-[var(--text-subtle)]">{label}</dt>
                <dd className="font-semibold">{value}</dd>
              </div>
            ))}
          </dl>
        </aside>
      </div>
    </div>
  );
}
