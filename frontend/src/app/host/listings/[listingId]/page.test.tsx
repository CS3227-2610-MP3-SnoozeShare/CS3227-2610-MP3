import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import HostListingPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';
import { HostApiProvider } from '@/components/host/HostApiProvider';

const { useParams } = vi.hoisted(() => ({ useParams: vi.fn(() => ({ listingId: 'lakeside-cabin' })) }));
const { fetchHostListing, fetchHostWallet } = vi.hoisted(() => ({ fetchHostListing: vi.fn(), fetchHostWallet: vi.fn() }));
vi.mock('next/navigation', () => ({ useParams }));
vi.mock('@/lib/hostApi', () => ({ fetchHostListing, fetchHostWallet, topUpHostWallet: vi.fn(), withdrawFromHostWallet: vi.fn() }));

describe('Host listing detail page', () => {
  it('renders Lakeside Cabin details and its standard breadcrumb', () => {
    render(<HostFixtureProvider><HostListingPage /></HostFixtureProvider>);
    expect(screen.getByRole('heading', { name: 'Lakeside Cabin' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Breadcrumb' })).toHaveTextContent(/Listings\s*›\s*Lakeside Cabin/);
    expect(screen.getByText('A cozy cabin steps from the water, with a private dock and wood-burning stove. Perfect for a quiet long weekend away from the city.')).toBeInTheDocument();
  });
  it('renders only the not-found card for unknown IDs', () => { useParams.mockReturnValue({ listingId: 'malformed%2Funknown' }); render(<HostFixtureProvider><HostListingPage /></HostFixtureProvider>); expect(screen.getAllByText('Listing not found').length).toBe(2); expect(screen.queryByText('AMENITIES')).not.toBeInTheDocument(); });
  it('renders listing detail and metrics from the server response', async () => {
    useParams.mockReturnValue({ listingId: 'server-id' });
    fetchHostListing.mockResolvedValue({ listing: { id: 'server-id', hostId: 'host-id', status: 'ACTIVE', title: 'API Cabin', description: 'Server description', propertyType: 'HOUSE', streetAddress: '1 Main', city: 'Reno', region: 'NV', postalCode: 89501, maxGuests: 3, bedrooms: 2, bathrooms: 1, baseNightlyRate: 95, checkInTime: '15:00:00', checkOutTime: '11:00:00', amenities: ['WIFI'], createdAt: '' }, metrics: { bookingCount: 2, averageRating: 4.8, reviews: [], occupancyPercentage: 30, earnings: 300 } });
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: { items: [], page: 0, size: 1, hasMore: false } });
    render(<HostApiProvider><HostFixtureProvider><HostListingPage /></HostFixtureProvider></HostApiProvider>);
    expect(await screen.findByRole('heading', { name: 'API Cabin' })).toBeInTheDocument();
    expect(screen.getByText('Server description')).toBeInTheDocument();
    expect(screen.getByText('30%')).toBeInTheDocument();
    expect(fetchHostListing).toHaveBeenCalledWith('server-id');
  });
});
