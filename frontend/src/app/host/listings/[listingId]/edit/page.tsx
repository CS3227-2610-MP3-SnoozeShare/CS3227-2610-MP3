'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useHostFixtures } from '@/components/host/HostFixtureProvider';
import { HostListingForm } from '@/components/host/HostListingForm';
import { HostPageFrame } from '@/components/host/HostPageFrame';

export default function EditListingPage() {
  const id = String(useParams<{ listingId: string }>().listingId ?? '');
  const result = useHostFixtures().getListing(id);
  if (result.state === 'not-found') return <HostPageFrame title="Listing not found"><div className="ss-card p-8 text-center"><p>Listing not found</p><Link className="mt-4 inline-block text-primary underline" href="/host/listings">Back to Listings</Link></div></HostPageFrame>;
  if (result.state !== 'populated' && result.state !== 'empty') return <HostPageFrame title="Edit listing"><div className="ss-card p-8 text-center">{result.error}</div></HostPageFrame>;
  return <HostPageFrame title="Edit listing" breadcrumbs={<><Link href="/host/listings" className="text-primary">Listings</Link><span aria-hidden="true">›</span><Link href={`/host/listings/${id}`} className="text-primary">{result.data.title}</Link><span aria-hidden="true">›</span><span>Edit</span></>}><HostListingForm listing={result.data} /></HostPageFrame>;
}
