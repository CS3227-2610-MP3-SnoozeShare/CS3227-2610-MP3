'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { HostListingCard } from '@/components/host/HostListingCard';
import { HostPageFrame } from '@/components/host/HostPageFrame';

export default function HostListingsPage() {
  const result = useHostFixtures().getListings();
  const [statuses, setStatuses] = useState<Record<string, 'active' | 'inactive'>>({});
  const listings = (result.data ?? []).map((listing) => ({ ...listing, status: statuses[listing.id] ?? listing.status }));
  const toggle = (id: string, current: 'active' | 'inactive') => setStatuses((currentStatuses) => ({ ...currentStatuses, [id]: current === 'active' ? 'inactive' : 'active' }));
  return <HostPageFrame title="My listings" actions={<Link className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground" href="/host/listings/new">+ New listing</Link>}>{result.state === 'loading' ? <div className="space-y-3" aria-label="Loading listings">{[1, 2, 3, 4].map((id) => <div className="ss-card h-28 animate-pulse bg-[var(--surface-inset)]" key={id} />)}</div> : result.state === 'error' ? <div className="ss-card space-y-3 p-8 text-center"><p>{result.error}</p><button className="rounded-lg border border-border px-3 py-2 text-sm" type="button">Retry</button></div> : !listings.length ? <div className="ss-card p-12 text-center text-[var(--text-subtle)]">You don&apos;t have any listings yet.</div> : <div className="space-y-3">{listings.map((listing) => <HostListingCard key={listing.id} listing={listing} onToggle={() => toggle(listing.id, listing.status)} />)}</div>}</HostPageFrame>;
}
