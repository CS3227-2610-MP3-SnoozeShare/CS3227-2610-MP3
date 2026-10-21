'use client';

import Link from 'next/link';
import type { HostListing } from './hostTypes';
import { StatusPill } from '@/components/ui/status-pill';

export function HostListingCard({ listing, onToggle }: { listing: HostListing; onToggle?: () => void }) {
  const thumbnailColors: Record<string, string> = {
    'lakeside-cabin': 'linear-gradient(135deg, #bd8058, #e0c4a0)',
    'downtown-loft': 'linear-gradient(135deg, #c7a585, #eee1cc)',
    'garden-studio': 'linear-gradient(135deg, #88705a, #d8c7a6)',
    'hilltop-house': 'linear-gradient(135deg, #a94d27, #e4b381)',
  };
  return (
    <article className="ss-card grid gap-4 p-4 md:grid-cols-[72px_minmax(0,1fr)_auto] md:items-center">
      <div aria-label={`${listing.title} thumbnail`} className="h-[72px] rounded-md bg-[var(--surface-inset)]" role="img" style={{ backgroundImage: thumbnailColors[listing.id] }} />
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2"><h2 className="truncate text-base font-semibold">{listing.title}</h2><StatusPill status={listing.status} /></div>
        <p className="mt-1 text-sm text-[var(--text-subtle)]">{listing.type} · {listing.city}, {listing.region}</p>
        <p className="mt-2 font-semibold">${listing.rate.toFixed(0)}<span className="font-normal text-[var(--text-subtle)]">/night</span></p>
      </div>
      <div className="flex flex-wrap items-center gap-4 text-right text-xs text-[var(--text-subtle)] md:justify-end">
        <span><strong className="block text-sm text-foreground">{listing.bookings}</strong> BOOKINGS</span>
        <span><strong className="block text-sm text-foreground">{listing.rating.toFixed(1)} ★</strong> RATING</span>
        <span className="text-[11px] font-semibold text-[var(--text-subtle)]">{listing.status === 'active' ? 'ACTIVE' : 'INACTIVE'}</span>
        <button aria-label={`${listing.title} availability`} aria-checked={listing.status === 'active'} className={`relative h-5 w-9 rounded-full transition-colors ${listing.status === 'active' ? 'bg-[var(--success)]' : 'bg-[var(--border-muted)]'}`} onClick={onToggle} role="switch" type="button"><span className={`absolute top-0.5 h-4 w-4 rounded-full bg-white ${listing.status === 'active' ? 'left-4' : 'left-0.5'}`} /></button>
        <Link className="text-primary underline-offset-2 hover:underline" href={`/host/listings/${listing.id}/calendar`}>Open Booking Calendar</Link>
        <Link className="text-primary underline-offset-2 hover:underline" href={`/host/listings/${listing.id}/edit`}>Edit</Link>
      </div>
    </article>
  );
}
