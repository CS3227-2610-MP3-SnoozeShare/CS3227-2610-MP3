'use client';

import Link from 'next/link';
import { useEffect } from 'react';
import { useParams } from 'next/navigation';
import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { useOptionalHostApi } from '@/components/host/HostApiProvider';
import { HostListingForm } from '@/components/host/HostListingForm';
import { HostBreadcrumbs } from '@/components/host/HostBreadcrumbs';
import { HostPageFrame } from '@/components/host/HostPageFrame';
import { mapHostListingDetail, type HostListingDetailDto } from '@/components/host/hostTypes';

export default function EditListingPage() {
  const id = String(useParams<{ listingId: string }>().listingId ?? '');
  const fixture = useHostFixtures().getListing(id);
  const api = useOptionalHostApi();
  const key = `host:listing:${id}`;
  const loadResource = api?.loadResource;
  const fetchListing = api?.api.fetchHostListing;
  useEffect(() => {
    if (loadResource && fetchListing) void loadResource(key, () => fetchListing(id)).catch(() => undefined);
  }, [fetchListing, id, key, loadResource]);
  const resource = api?.getResource<HostListingDetailDto>(key);
  const listing = api
    ? resource?.status === 'ready' || resource?.status === 'empty' ? mapHostListingDetail(resource.data) : null
    : fixture.state === 'populated' || fixture.state === 'empty' ? fixture.data : null;
  const missing = api ? resource?.status === 'not-found' : fixture.state === 'not-found';
  if (missing) return <HostPageFrame title="Listing not found"><div className="ss-card p-8 text-center"><p>Listing not found</p><Link className="mt-4 inline-block text-primary underline" href="/host/listings">Back to Listings</Link></div></HostPageFrame>;
  if (!listing) return <HostPageFrame title="Edit listing"><div className="ss-card p-8 text-center">{resource?.status === 'error' ? resource.message : fixture.error ?? 'Loading listing…'}</div></HostPageFrame>;
  return <HostPageFrame title="Edit listing" breadcrumbs={<HostBreadcrumbs items={[{ label: 'Listings', href: '/host/listings' }, { label: listing.title, href: `/host/listings/${id}` }, { label: 'Edit' }]} />}><HostListingForm listing={listing} /></HostPageFrame>;
}
