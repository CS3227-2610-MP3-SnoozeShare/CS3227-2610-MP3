'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { useOptionalHostApi } from '@/components/host/HostApiProvider';
import { HostListingCard } from '@/components/host/HostListingCard';
import { HostPageFrame } from '@/components/host/HostPageFrame';
import { HostListingRouteError } from '@/components/host/HostListingRouteError';
import { hostSafeErrorMessage } from '@/lib/hostApi';
import { mapHostListing, type HostListingDto, type HostPage } from '@/components/host/hostTypes';

export default function HostListingsPage() {
  const fixtures = useHostFixtures();
  const api = useOptionalHostApi();
  const [page, setPage] = useState(0);
  const [statuses, setStatuses] = useState<Record<string, 'active' | 'inactive'>>({});
  const [updating, setUpdating] = useState<string | null>(null);
  const [mutationError, setMutationError] = useState('');
  const resourceKey = `host:listings:${page}`;
  const loadResource = api?.loadResource;
  const fetchListings = api?.api.fetchHostListings;
  useEffect(() => {
    if (loadResource && fetchListings) void loadResource(resourceKey, () => fetchListings(page, 50)).catch(() => undefined);
  }, [fetchListings, loadResource, page, resourceKey]);
  const apiResult = api?.getResource<HostPage<HostListingDto>>(resourceKey);
  const fixtureResult = fixtures.getListings();
  const apiMode = Boolean(api);
  const loading = apiMode ? !apiResult || apiResult.status === 'loading' : fixtureResult.state === 'loading';
  const error = apiMode ? apiResult?.status === 'error' ? apiResult.message : '' : fixtureResult.state === 'error' ? fixtureResult.error : '';
  const listings = apiMode
    ? apiResult?.status === 'ready' || apiResult?.status === 'empty' ? apiResult.data.items.map(mapHostListing) : []
    : (fixtureResult.data ?? []).map((listing) => ({ ...listing, status: statuses[listing.id] ?? listing.status }));
  const hasMore = apiMode && (apiResult?.status === 'ready' || apiResult?.status === 'empty') && apiResult.data.hasMore;
  const toggle = async (id: string, current: 'active' | 'inactive') => {
    if (!api) {
      setStatuses((currentStatuses) => ({ ...currentStatuses, [id]: current === 'active' ? 'inactive' : 'active' }));
      return;
    }
    setUpdating(id);
    setMutationError('');
    try {
      await api.api.updateHostListingStatus(id, current === 'active' ? 'INACTIVE' : 'ACTIVE');
      await api.loadResource(resourceKey, () => api.api.fetchHostListings(page, 50));
    } catch (caught) {
      setMutationError(hostSafeErrorMessage(caught));
    } finally {
      setUpdating(null);
    }
  };
  const retry = () => api
    ? void api.loadResource(resourceKey, () => api.api.fetchHostListings(page, 50)).catch(() => undefined)
    : undefined;
  if (error) return <HostListingRouteError onRetry={retry} />;
  return <HostPageFrame fillHeight title="My listings" actions={<Link className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground" href="/host/listings/new">+ New listing</Link>}><div aria-label="Listing results" className="min-h-0 flex-1 overflow-y-auto">{loading ? <div className="space-y-3">{[1, 2, 3, 4].map((id) => <div className="ss-card h-28 animate-pulse bg-[var(--surface-inset)]" key={id} />)}</div> : !listings.length ? <div className="ss-card p-12 text-center text-[var(--text-subtle)]">You don&apos;t have any listings yet.</div> : <div className="space-y-3">{listings.map((listing) => <HostListingCard key={listing.id} listing={listing} isUpdating={updating === listing.id} onToggle={() => void toggle(listing.id, listing.status)} />)}</div>}{mutationError && <p role="alert" className="mt-3 text-sm text-destructive">{mutationError}</p>}{apiMode && (page > 0 || hasMore) && <div className="flex justify-between py-3"><button disabled={page === 0} onClick={() => setPage((value) => value - 1)} type="button">Previous</button><span>Page {page + 1}</span><button disabled={!hasMore} onClick={() => setPage((value) => value + 1)} type="button">Next</button></div>}</div></HostPageFrame>;
}
