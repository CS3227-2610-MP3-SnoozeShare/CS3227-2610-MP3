'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { HostCalendarPanel } from '@/components/host/HostCalendarPanel';
import { HostPageFrame } from '@/components/host/HostPageFrame';

export default function HostCalendarPage() {
  const id = String(useParams<{ listingId: string }>().listingId ?? '');
  const result = useHostFixtures().getCalendar(id);
  if (result.state === 'loading') return <HostPageFrame title="Booking calendar"><div className="ss-card h-64 animate-pulse bg-[var(--surface-inset)]" /></HostPageFrame>;
  if (result.state === 'error') return <HostPageFrame title="Booking calendar"><div className="ss-card p-8 text-center">{result.error}</div></HostPageFrame>;
  return <HostPageFrame title="Booking calendar" breadcrumbs={<Link className="text-primary" href={`/host/listings/${id}`}>Listing details</Link>}><HostCalendarPanel calendar={result.data!} /></HostPageFrame>;
}
