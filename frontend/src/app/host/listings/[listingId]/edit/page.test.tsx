import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import EditListingPage from './page';
import { HostApiProvider } from '@/components/host/HostApiProvider';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';
import { BackendApiError } from '@/lib/api';

const { useParams, useRouter } = vi.hoisted(() => ({
  useParams: vi.fn(() => ({ listingId: 'missing-id' })),
  useRouter: vi.fn(() => ({ push: vi.fn() })),
}));
const { fetchHostListing, fetchHostWallet } = vi.hoisted(() => ({ fetchHostListing: vi.fn(), fetchHostWallet: vi.fn() }));
vi.mock('next/navigation', () => ({ useParams, useRouter }));
vi.mock('@/lib/hostApi', async (importOriginal) => ({
  ...await importOriginal<typeof import('@/lib/hostApi')>(),
  fetchHostListing,
  fetchHostWallet,
}));

describe('Host edit listing route', () => {
  it('uses the generic listing error and return link when the listing cannot be loaded', async () => {
    fetchHostListing.mockRejectedValue(new BackendApiError('NOT_FOUND', 'private backend details', 'request-1', 404));
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: { items: [], page: 0, size: 1, hasMore: false } });

    render(<HostApiProvider><HostFixtureProvider><EditListingPage /></HostFixtureProvider></HostApiProvider>);

    expect(await screen.findByRole('heading', { name: 'Something went wrong' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to Listings' })).toHaveAttribute('href', '/host/listings');
    expect(screen.queryByText('private backend details')).not.toBeInTheDocument();
  });

  it('keeps API check-in and check-out values unchanged in the edit inputs', async () => {
    useParams.mockReturnValue({ listingId: 'server-id' });
    fetchHostListing.mockResolvedValue({
      listing: { id: 'server-id', hostId: 'host-id', status: 'ACTIVE', title: 'API Cabin', description: 'A cabin', propertyType: 'HOUSE', streetAddress: '1 Main', city: 'Reno', region: 'NV', postalCode: 89501, maxGuests: 3, bedrooms: 2, bathrooms: 1, baseNightlyRate: 95, checkInTime: '07:00:00', checkOutTime: '03:00:00', amenities: [], createdAt: '' },
      metrics: { bookingCount: 0, averageRating: 0, reviews: [], occupancyPercentage: 0, earnings: 0 },
    });
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: [] });

    render(<HostApiProvider><HostFixtureProvider><EditListingPage /></HostFixtureProvider></HostApiProvider>);

    expect(await screen.findByLabelText('Check-in time')).toHaveValue('07:00');
    expect(screen.getByLabelText('Check-out time')).toHaveValue('03:00');
  });
});
