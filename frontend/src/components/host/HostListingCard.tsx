'use client';

import Link from 'next/link';
import type { HostListing } from './hostTypes';

export function HostListingCard({ listing, onToggle }: { listing: HostListing; onToggle?: () => void }) {
  const thumbnailColors: Record<string, string> = {
    'lakeside-cabin': 'linear-gradient(135deg, #bd8058, #e0c4a0)',
    'downtown-loft': 'linear-gradient(135deg, #c7a585, #eee1cc)',
    'garden-studio': 'linear-gradient(135deg, #88705a, #d8c7a6)',
    'hilltop-house': 'linear-gradient(135deg, #a94d27, #e4b381)',
  };
  return (
    <article className="ss-card grid gap-x-6 gap-y-4 p-4 md:grid-cols-[72px_minmax(0,1fr)] md:items-center xl:grid-cols-[72px_minmax(0,1fr)_minmax(690px,1.65fr)]">
      <div aria-label={`${listing.title} thumbnail`} className="h-[72px] rounded-md bg-[var(--surface-inset)]" role="img" style={{ backgroundImage: thumbnailColors[listing.id] }} />
      <div className="min-w-0">
        <h2 className="truncate text-base font-semibold">{listing.title}</h2>
        <p className="mt-1 text-sm text-[var(--text-subtle)]">{listing.type} · {listing.city}, {listing.region}</p>
        <p className="mt-2 font-semibold">${listing.rate.toFixed(0)}<span className="font-normal text-[var(--text-subtle)]">/night</span></p>
      </div>
      <div className="grid grid-cols-2 items-stretch gap-x-3 gap-y-4 text-xs text-[var(--text-subtle)] sm:grid-cols-5 sm:gap-x-4 xl:gap-x-8 md:col-span-2 xl:col-span-1">
        <div aria-label="Bookings" className="flex min-w-0 flex-col items-center justify-center text-center" role="group"><strong className="text-sm text-foreground">{listing.bookings}</strong><span>BOOKINGS</span></div>
        <div aria-label="Rating" className="flex min-w-0 flex-col items-center justify-center text-center" role="group"><strong className="text-sm text-foreground">{listing.rating.toFixed(1)} ★</strong><span>RATING</span></div>
        <div aria-label="Availability" className="flex w-28 flex-col items-center justify-center gap-1 text-center" role="group"><span className="text-[11px] font-semibold">{listing.status === 'active' ? 'ACTIVE' : 'INACTIVE'}</span><button aria-label={`${listing.title} availability`} aria-checked={listing.status === 'active'} className={`relative h-5 w-9 rounded-full transition-colors ${listing.status === 'active' ? 'bg-[var(--success)]' : 'bg-[var(--border-muted)]'}`} onClick={onToggle} role="switch" type="button"><span className={`absolute top-0.5 h-4 w-4 rounded-full bg-white ${listing.status === 'active' ? 'left-4' : 'left-0.5'}`} /></button></div>
        <div aria-label="Booking calendar" className="flex min-w-0 items-center justify-center text-center" role="group"><Link className="inline-flex min-h-9 items-center justify-center rounded-lg border border-border bg-background px-3 py-2 text-center text-primary hover:bg-[var(--surface-inset)]" href={`/host/listings/${listing.id}/calendar`}>Open Booking Calendar</Link></div>
        <div aria-label="Edit listing" className="flex min-w-0 items-center justify-center text-center" role="group"><Link className="inline-flex min-h-9 items-center justify-center rounded-lg border border-border bg-background px-4 py-2 text-primary hover:bg-[var(--surface-inset)]" href={`/host/listings/${listing.id}/edit`}>Edit</Link></div>
      </div>
    </article>
  );
}
