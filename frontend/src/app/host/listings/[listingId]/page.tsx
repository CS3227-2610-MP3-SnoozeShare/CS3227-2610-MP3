'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { useOptionalHostApi } from '@/components/host/HostApiProvider';
import { HostListingDetail } from '@/components/host/HostListingDetail';
import { HostBreadcrumbs } from '@/components/host/HostBreadcrumbs';
import { HostPageFrame } from '@/components/host/HostPageFrame';
import { HostListingRouteError } from '@/components/host/HostListingRouteError';
import { mapHostListingDetail, type HostListingDetailDto } from '@/components/host/hostTypes';
import { useEffect } from 'react';

export default function HostListingPage() {
  const id = String(useParams<{ listingId: string }>().listingId ?? '');
  const fixtureResult = useHostFixtures().getListing(id);
  const api = useOptionalHostApi();
  const key = `host:listing:${id}`;
  const loadResource = api?.loadResource;
  const fetchListing = api?.api.fetchHostListing;
  useEffect(() => {
    if (loadResource && fetchListing) void loadResource(key, () => fetchListing(id)).catch(() => undefined);
  }, [fetchListing, id, key, loadResource]);
  const resource = api?.getResource<HostListingDetailDto>(key);
  const data = api
    ? resource?.status === 'ready' || resource?.status === 'empty' ? mapHostListingDetail(resource.data) : null
    : fixtureResult.state === 'populated' || fixtureResult.state === 'empty' ? fixtureResult.data : null;
  const notFound = api ? resource?.status === 'not-found' : fixtureResult.state === 'not-found';
  const loading = api ? !resource || resource.status === 'loading' : fixtureResult.state === 'loading';
  const error = api ? resource?.status === 'error' ? resource.message : '' : fixtureResult.state === 'error' ? fixtureResult.error : '';
  if (notFound || error) return <HostListingRouteError />;
  if (loading) return <HostPageFrame title="Loading…"><div className="ss-card h-56 animate-pulse bg-[var(--surface-inset)]" /></HostPageFrame>;
  if (!data) return null;
  return <HostPageFrame title="" breadcrumbs={<HostBreadcrumbs items={[{ label: 'Listings', href: '/host/listings' }, { label: data.title }]} />} breadcrumbActions={<><Link className="rounded-lg border border-border px-3 py-2 text-xs" href={`/host/listings/${data.id}/calendar`}>Open Booking Calendar</Link><Link className="rounded-lg bg-primary px-3 py-2 text-xs font-semibold text-primary-foreground" href={`/host/listings/${data.id}/edit`}>Edit listing</Link></>}><HostListingDetail listing={data} /></HostPageFrame>;
}
