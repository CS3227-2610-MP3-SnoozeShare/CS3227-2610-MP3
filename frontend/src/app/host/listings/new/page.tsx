'use client';

import { HostListingForm } from '@/components/host/HostListingForm';
import { HostPageFrame } from '@/components/host/HostPageFrame';
import Link from 'next/link';

export default function NewListingPage() { return <HostPageFrame title="New listing" breadcrumbs={<><Link href="/host/listings" className="text-primary">Listings</Link><span aria-hidden="true">›</span><span>New</span></>}><HostListingForm /></HostPageFrame>; }
