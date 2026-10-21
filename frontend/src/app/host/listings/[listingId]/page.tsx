'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { HostListingDetail } from '@/components/host/HostListingDetail';
import { HostBreadcrumbs } from '@/components/host/HostBreadcrumbs';
import { HostPageFrame } from '@/components/host/HostPageFrame';

export default function HostListingPage() {
  const id = String(useParams<{ listingId: string }>().listingId ?? '');
  const result = useHostFixtures().getListing(id);
  if (result.state === 'not-found') return <HostPageFrame title="Listing not found"><div className="ss-card p-8 text-center"><p>Listing not found</p><Link className="mt-4 inline-block text-primary underline" href="/host/listings">Back to Listings</Link></div></HostPageFrame>;
  if (result.state === 'loading') return <HostPageFrame title="Loading…"><div className="ss-card h-56 animate-pulse bg-[var(--surface-inset)]" /></HostPageFrame>;
  if (result.state === 'error') return <HostPageFrame title="Listing error"><div className="ss-card p-8 text-center">{result.error}</div></HostPageFrame>;
  if (result.state !== 'populated' && result.state !== 'empty') return null;
  return <HostPageFrame title="" breadcrumbs={<HostBreadcrumbs items={[{ label: 'Listings', href: '/host/listings' }, { label: result.data.title }]} />}><HostListingDetail listing={result.data} /></HostPageFrame>;
}
