'use client';

import Link from 'next/link';
import type { HostListing } from './hostTypes';

export function HostListingCard({ listing, onToggle }: { listing: HostListing; onToggle?: () => void }) {
  return (
    <article className="ss-card relative grid gap-x-6 gap-y-4 p-4 md:grid-cols-[72px_minmax(0,1fr)] md:items-center xl:grid-cols-[72px_minmax(0,1fr)_max-content]">
      <div aria-label={`${listing.title} thumbnail`} className="h-[72px] rounded-md bg-[var(--surface-inset)]" role="img" style={{ backgroundImage: 'linear-gradient(110deg, #cf754d 0%, #b59a7e 100%)' }} />
      <div className="min-w-0">
        <h2 className="truncate text-base font-semibold"><Link className="text-inherit no-underline after:absolute after:inset-0 after:content-[''] focus-visible:rounded-lg focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary" href={`/host/listings/${listing.id}`}>{listing.title}</Link></h2>
        <p className="mt-1 text-sm text-[var(--text-subtle)]">{listing.type} · {listing.city}, {listing.region}</p>
        <p className="mt-2 font-semibold">${listing.rate.toFixed(0)}<span className="font-normal text-[var(--text-subtle)]">/night</span></p>
      </div>
      <div className="relative z-10 grid w-full grid-cols-2 items-stretch gap-x-2 gap-y-4 text-xs text-[var(--text-subtle)] min-[560px]:grid-cols-3 lg:grid-cols-5 xl:col-span-1 xl:grid-cols-[5rem_5rem_7rem_10rem_5rem] xl:gap-x-3 xl:w-fit xl:justify-self-end md:col-span-2">
        <div aria-label="Bookings" className="flex min-w-0 flex-col items-center justify-center text-center" role="group"><strong className="text-sm text-foreground">{listing.bookings}</strong><span>BOOKINGS</span></div>
        <div aria-label="Rating" className="flex min-w-0 flex-col items-center justify-center text-center" role="group"><strong className="text-sm text-foreground">{listing.rating.toFixed(1)} ★</strong><span>RATING</span></div>
        <div aria-label="Availability" className="flex w-28 flex-row items-center justify-center gap-2 text-center" role="group"><span className="text-[11px] font-semibold">{listing.status === 'active' ? 'ACTIVE' : 'INACTIVE'}</span><button aria-label={`${listing.title} availability`} aria-checked={listing.status === 'active'} className={`relative h-5 w-9 rounded-full transition-colors ${listing.status === 'active' ? 'bg-[var(--success)]' : 'bg-[var(--border-muted)]'}`} onClick={onToggle} role="switch" type="button"><span className={`absolute top-0.5 h-4 w-4 rounded-full bg-white ${listing.status === 'active' ? 'left-4' : 'left-0.5'}`} /></button></div>
        <div aria-label="Booking calendar" className="flex min-w-0 items-center justify-center text-center" role="group"><Link className="rounded-lg border border-border px-3 py-2 text-xs" href={`/host/listings/${listing.id}/calendar`}>Open Booking Calendar</Link></div>
        <div aria-label="Edit listing" className="flex min-w-0 items-center justify-center text-center" role="group"><Link className="rounded-lg border border-border px-3 py-2 text-xs" href={`/host/listings/${listing.id}/edit`}>Edit</Link></div>
      </div>
    </article>
  );
}
