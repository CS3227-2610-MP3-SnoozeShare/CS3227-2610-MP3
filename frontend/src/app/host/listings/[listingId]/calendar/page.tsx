'use client';

import { useParams } from 'next/navigation';
import { useEffect, useMemo, useState } from 'react';
import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { useOptionalHostApi } from '@/components/host/HostApiProvider';
import { HostCalendarPanel } from '@/components/host/HostCalendarPanel';
import { HostBreadcrumbs } from '@/components/host/HostBreadcrumbs';
import { HostPageFrame } from '@/components/host/HostPageFrame';
import { HostListingRouteError } from '@/components/host/HostListingRouteError';
import { mapHostCalendar, mapHostListingDetail, type CalendarData, type HostCalendarDto, type HostListingDetailDto } from '@/components/host/hostTypes';

export default function HostCalendarPage() {
  const id = String(useParams<{ listingId: string }>().listingId ?? '');
  const fixtures = useHostFixtures();
  const api = useOptionalHostApi();
  const [month, setMonth] = useState(() => { const now = new Date(); return { year: now.getFullYear(), month: now.getMonth() + 1 }; });
  const [lastSuccessfulCalendar, setLastSuccessfulCalendar] = useState<CalendarData | null>(null);
  const calendarKey = `host:calendar:${id}:${month.year}-${month.month}`;
  const listingKey = `host:listing:${id}`;
  const loadResource = api?.loadResource;
  const fetchCalendar = api?.api.fetchHostCalendar;
  const fetchListing = api?.api.fetchHostListing;
  useEffect(() => {
    if (!loadResource || !fetchCalendar) return;
    void loadResource(calendarKey, () => fetchCalendar(id, month.year, month.month)).catch(() => undefined);
  }, [calendarKey, fetchCalendar, id, loadResource, month.month, month.year]);
  useEffect(() => {
    if (!loadResource || !fetchListing) return;
    void loadResource(listingKey, () => fetchListing(id)).catch(() => undefined);
  }, [fetchListing, id, listingKey, loadResource]);
  const fixtureCalendar = fixtures.getCalendar(id);
  const fixtureListing = fixtures.getListing(id);
  const calendarResource = api?.getResource<HostCalendarDto>(calendarKey);
  const listingResource = api?.getResource<HostListingDetailDto>(listingKey);
  const mappedCalendar = useMemo(() => calendarResource?.status === 'ready' || calendarResource?.status === 'empty' ? mapHostCalendar(calendarResource.data) : null, [calendarResource]);
  useEffect(() => {
    if (mappedCalendar) setLastSuccessfulCalendar(mappedCalendar);
  }, [mappedCalendar]);
  const loading = api
    ? !listingResource || listingResource.status === 'loading' || (!mappedCalendar && !lastSuccessfulCalendar && (!calendarResource || calendarResource.status === 'loading'))
    : fixtureCalendar.state === 'loading';
  if (loading) return <HostPageFrame title=""><div className="ss-card h-64 animate-pulse bg-[var(--surface-inset)]" /></HostPageFrame>;
  const notFound = api && (calendarResource?.status === 'not-found' || listingResource?.status === 'not-found');
  if (notFound) return <HostListingRouteError />;
  const error = api
    ? calendarResource?.status === 'error' ? calendarResource.message : listingResource?.status === 'error' ? listingResource.message : ''
    : fixtureCalendar.state === 'error' ? fixtureCalendar.error : '';
  if (error) return <HostListingRouteError />;
  const calendar = api
    ? mappedCalendar ?? lastSuccessfulCalendar
    : fixtureCalendar.data ?? null;
  const calendarLoading = Boolean(api && (!mappedCalendar && (calendarResource?.status === 'loading' || !calendarResource)));
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
      requestedMonth={api ? month : undefined}
      calendarLoading={calendarLoading}
      onMonthChange={api ? (year, nextMonth) => {
        setLastSuccessfulCalendar(calendar);
        setMonth({ year, month: nextMonth });
      } : undefined}
      onCreateBlock={api ? async (block) => { await api.api.createHostBlock(id, block); await reloadCalendar(); } : undefined}
      onDeleteBlock={api ? async (block) => { if (!block.id) throw new Error('Block is no longer available.'); await api.api.deleteHostBlock(block.id); await reloadCalendar(); } : undefined}
    />}
  </HostPageFrame>;
}
