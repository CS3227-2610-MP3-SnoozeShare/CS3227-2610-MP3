'use client';

import Link from 'next/link';
import type { HostListing } from './hostTypes';
import { StatusPill } from '@/components/ui/status-pill';

export function HostListingCard({ listing, onToggle }: { listing: HostListing; onToggle?: () => void }) {
  return (
    <article className="ss-card grid gap-4 p-4 md:grid-cols-[72px_minmax(0,1fr)_auto] md:items-center">
      <div aria-label={`${listing.title} thumbnail`} className="flex h-[72px] items-center justify-center rounded-md bg-[var(--surface-inset)] text-2xl text-[var(--primary)]" role="img">⌂</div>
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2"><h2 className="truncate text-base font-semibold">{listing.title}</h2><StatusPill status={listing.status} /></div>
        <p className="mt-1 text-sm text-[var(--text-subtle)]">{listing.type} · {listing.city}, {listing.region}</p>
        <p className="mt-2 font-semibold">${listing.rate.toFixed(0)}<span className="font-normal text-[var(--text-subtle)]">/night</span></p>
      </div>
      <div className="flex flex-wrap items-center gap-4 text-right text-xs text-[var(--text-subtle)] md:justify-end">
        <span><strong className="block text-sm text-foreground">{listing.bookings}</strong> BOOKINGS</span>
        <span><strong className="block text-sm text-foreground">{listing.rating.toFixed(1)} ★</strong> RATING</span>
        <button aria-label={`${listing.status === 'active' ? 'Deactivate' : 'Activate'} ${listing.title}`} className="rounded-lg border border-border px-2 py-1 text-xs font-semibold text-foreground" onClick={onToggle} type="button">{listing.status === 'active' ? 'ACTIVE' : 'INACTIVE'}</button>
        <Link className="text-primary underline-offset-2 hover:underline" href={`/host/listings/${listing.id}/calendar`}>Open Booking Calendar</Link>
        <Link className="text-primary underline-offset-2 hover:underline" href={`/host/listings/${listing.id}/edit`}>Edit</Link>
      </div>
    </article>
  );
}
