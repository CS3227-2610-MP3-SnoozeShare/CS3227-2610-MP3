'use client';

import { HostListingForm } from '@/components/host/HostListingForm';
import { HostBreadcrumbs } from '@/components/host/HostBreadcrumbs';
import { HostPageFrame } from '@/components/host/HostPageFrame';

export default function NewListingPage() { return <HostPageFrame title="New listing" breadcrumbs={<HostBreadcrumbs items={[{ label: 'Listings', href: '/host/listings' }, { label: 'New' }]} />}><HostListingForm /></HostPageFrame>; }
