'use client';

import { useParams } from 'next/navigation';
import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { HostCalendarPanel } from '@/components/host/HostCalendarPanel';
import { HostBreadcrumbs } from '@/components/host/HostBreadcrumbs';
import { HostPageFrame } from '@/components/host/HostPageFrame';

export default function HostCalendarPage() {
  const id = String(useParams<{ listingId: string }>().listingId ?? '');
  const fixtures = useHostFixtures();
  const result = fixtures.getCalendar(id);
  const listing = fixtures.getListing(id);
  if (result.state === 'loading') return <HostPageFrame title="Booking Calendar"><div className="ss-card h-64 animate-pulse bg-[var(--surface-inset)]" /></HostPageFrame>;
  if (result.state === 'error') return <HostPageFrame title="Booking Calendar"><div className="ss-card p-8 text-center">{result.error}</div></HostPageFrame>;
  const listingTitle = listing.state === 'populated' || listing.state === 'empty' ? listing.data.title : 'Listing details';
  return <HostPageFrame title="Booking Calendar" breadcrumbs={<HostBreadcrumbs items={[{ label: 'Listings', href: '/host/listings' }, { label: listingTitle, href: `/host/listings/${id}` }, { label: 'Booking Calendar' }]} />}><HostCalendarPanel calendar={result.data!} /></HostPageFrame>;
}
