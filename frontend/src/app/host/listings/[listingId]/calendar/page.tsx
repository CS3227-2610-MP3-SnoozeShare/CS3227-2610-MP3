'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useEffect, useMemo, useState } from 'react';
import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { useOptionalHostApi } from '@/components/host/HostApiProvider';
import { HostCalendarPanel } from '@/components/host/HostCalendarPanel';
import { HostBreadcrumbs } from '@/components/host/HostBreadcrumbs';
import { HostPageFrame } from '@/components/host/HostPageFrame';
import { mapHostCalendar, mapHostListingDetail, type HostCalendarDto, type HostListingDetailDto } from '@/components/host/hostTypes';

export default function HostCalendarPage() {
  const id = String(useParams<{ listingId: string }>().listingId ?? '');
  const fixtures = useHostFixtures();
  const api = useOptionalHostApi();
  const [month, setMonth] = useState(() => { const now = new Date(); return { year: now.getFullYear(), month: now.getMonth() + 1 }; });
  const calendarKey = `host:calendar:${id}:${month.year}-${month.month}`;
  const listingKey = `host:listing:${id}`;
  const loadResource = api?.loadResource;
  const fetchCalendar = api?.api.fetchHostCalendar;
  const fetchListing = api?.api.fetchHostListing;
  useEffect(() => {
    if (!loadResource || !fetchCalendar || !fetchListing) return;
    void loadResource(calendarKey, () => fetchCalendar(id, month.year, month.month)).catch(() => undefined);
    void loadResource(listingKey, () => fetchListing(id)).catch(() => undefined);
  }, [calendarKey, fetchCalendar, fetchListing, id, listingKey, loadResource, month.month, month.year]);
  const fixtureCalendar = fixtures.getCalendar(id);
  const fixtureListing = fixtures.getListing(id);
  const calendarResource = api?.getResource<HostCalendarDto>(calendarKey);
  const listingResource = api?.getResource<HostListingDetailDto>(listingKey);
  const mappedCalendar = useMemo(() => calendarResource?.status === 'ready' || calendarResource?.status === 'empty' ? mapHostCalendar(calendarResource.data) : null, [calendarResource]);
  const loading = api
    ? !calendarResource || !listingResource || calendarResource.status === 'loading' || listingResource.status === 'loading'
    : fixtureCalendar.state === 'loading';
  if (loading) return <HostPageFrame title=""><div className="ss-card h-64 animate-pulse bg-[var(--surface-inset)]" /></HostPageFrame>;
  const notFound = api && (calendarResource?.status === 'not-found' || listingResource?.status === 'not-found');
  if (notFound) return <HostPageFrame title=""><div className="ss-card p-8 text-center"><p>This item is no longer available.</p><Link className="mt-4 inline-block text-primary underline" href="/host/listings">Back to Listings</Link></div></HostPageFrame>;
  const error = api
    ? calendarResource?.status === 'error' ? calendarResource.message : listingResource?.status === 'error' ? listingResource.message : ''
    : fixtureCalendar.state === 'error' ? fixtureCalendar.error : '';
  if (error) return <HostPageFrame title=""><div className="ss-card p-8 text-center">{error}</div></HostPageFrame>;
  const calendar = api
    ? mappedCalendar
    : fixtureCalendar.data ?? null;
  const listing = api
    ? listingResource?.status === 'ready' || listingResource?.status === 'empty' ? mapHostListingDetail(listingResource.data) : null
    : fixtureListing.state === 'populated' || fixtureListing.state === 'empty' ? fixtureListing.data : null;
  const listingTitle = listing?.title ?? 'Listing details';
  const reloadCalendar = () => api && loadResource && fetchCalendar
    ? loadResource(calendarKey, () => fetchCalendar(id, month.year, month.month))
    : Promise.resolve();
  return <HostPageFrame title="" breadcrumbs={<HostBreadcrumbs items={[{ label: 'Listings', href: '/host/listings' }, { label: listingTitle, href: `/host/listings/${id}` }, { label: 'Booking Calendar' }]} />}>
    {calendar && <HostCalendarPanel
      calendar={calendar}
      onMonthChange={api ? (year, nextMonth) => setMonth({ year, month: nextMonth }) : undefined}
      onCreateBlock={api ? async (block) => { await api.api.createHostBlock(id, block); await reloadCalendar(); } : undefined}
      onDeleteBlock={api ? async (block) => { if (!block.id) throw new Error('Block is no longer available.'); await api.api.deleteHostBlock(block.id); await reloadCalendar(); } : undefined}
    />}
  </HostPageFrame>;
}
