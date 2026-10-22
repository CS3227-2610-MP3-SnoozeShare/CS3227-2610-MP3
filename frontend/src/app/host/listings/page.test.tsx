import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import HostListingsPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';
import { createHostFixtureAdapter } from '@/components/host/hostFixtures';
import { HostApiProvider } from '@/components/host/HostApiProvider';

const { fetchHostListings, fetchHostWallet, updateHostListingStatus } = vi.hoisted(() => ({ fetchHostListings: vi.fn(), fetchHostWallet: vi.fn(), updateHostListingStatus: vi.fn() }));
vi.mock('@/lib/hostApi', async (importOriginal) => ({ ...await importOriginal<typeof import('@/lib/hostApi')>(), fetchHostListings, fetchHostWallet, updateHostListingStatus, topUpHostWallet: vi.fn(), withdrawFromHostWallet: vi.fn() }));

describe('Host listings page', () => {
  it('renders enough populated listings to scroll below the fixed page title', () => { render(<HostFixtureProvider><HostListingsPage /></HostFixtureProvider>); expect(screen.getByText('My listings')).toBeInTheDocument(); expect(screen.getByText('Lakeside Cabin')).toBeInTheDocument(); expect(screen.getByText('Hilltop House')).toBeInTheDocument(); expect(screen.getAllByRole('article')).toHaveLength(12); expect(screen.getByRole('main')).toHaveClass('flex', 'min-h-0', 'flex-1', 'flex-col'); expect(screen.getByLabelText('Listing results')).toHaveClass('min-h-0', 'flex-1', 'overflow-y-auto'); });
  it('renders the generic listing error with a return link and retry for a listings load failure', () => { const { rerender } = render(<HostFixtureProvider adapter={createHostFixtureAdapter('empty')}><HostListingsPage /></HostFixtureProvider>); expect(screen.getByText("You don't have any listings yet.")).toBeInTheDocument(); rerender(<HostFixtureProvider adapter={createHostFixtureAdapter('error')}><HostListingsPage /></HostFixtureProvider>); expect(screen.getByRole('heading', { name: 'Something went wrong' })).toBeInTheDocument(); expect(screen.getByText("We couldn't load this page.")).toBeInTheDocument(); expect(screen.getByRole('link', { name: 'Back to Listings' })).toHaveAttribute('href', '/host/listings'); expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument(); });
  it('loads listing cards from the authenticated API rather than fixtures', async () => {
    fetchHostListings.mockResolvedValue([{ id: 'api-listing', hostId: 'host-id', status: 'ACTIVE', title: 'Server Cabin', description: '', propertyType: 'HOUSE', streetAddress: '1 Main', city: 'Reno', region: 'NV', postalCode: 89501, maxGuests: 2, bedrooms: 1, bathrooms: 1, baseNightlyRate: 90, checkInTime: '15:00:00', checkOutTime: '11:00:00', amenities: [], createdAt: '2026-10-01T00:00:00Z' }]);
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: [] });
    render(<HostApiProvider><HostFixtureProvider><HostListingsPage /></HostFixtureProvider></HostApiProvider>);
    expect(await screen.findByText('Server Cabin')).toBeInTheDocument();
    expect(screen.queryByText('Lakeside Cabin')).not.toBeInTheDocument();
    expect(fetchHostListings).toHaveBeenCalledWith();
  });
  it('keeps the server status when a status mutation fails', async () => {
    fetchHostListings.mockResolvedValue([{ id: 'api-listing', hostId: 'host-id', status: 'ACTIVE', title: 'Server Cabin', description: '', propertyType: 'HOUSE', streetAddress: '1 Main', city: 'Reno', region: 'NV', postalCode: 89501, maxGuests: 2, bedrooms: 1, bathrooms: 1, baseNightlyRate: 90, checkInTime: '15:00:00', checkOutTime: '11:00:00', amenities: [], createdAt: '' }]);
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: [] });
    updateHostListingStatus.mockRejectedValue(new Error('private backend detail'));
    render(<HostApiProvider><HostFixtureProvider><HostListingsPage /></HostFixtureProvider></HostApiProvider>);
    await screen.findByText('Server Cabin');
    const toggle = screen.getByRole('switch', { name: 'Server Cabin availability' });
    toggle.click();
    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to complete this request. Please try again.');
    expect(toggle).toHaveAttribute('aria-checked', 'true');
    expect(fetchHostListings).toHaveBeenCalledTimes(1);
  });
  it('shows a generic listing error when the collection API fails without exposing details', async () => {
    fetchHostListings.mockRejectedValue(new Error('private backend detail'));
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: [] });
    render(<HostApiProvider><HostFixtureProvider><HostListingsPage /></HostFixtureProvider></HostApiProvider>);

    expect(await screen.findByRole('heading', { name: 'Something went wrong' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to Listings' })).toHaveAttribute('href', '/host/listings');
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
    expect(screen.queryByText('private backend detail')).not.toBeInTheDocument();
  });
  it('renders every listing from one complete collection request', async () => {
    fetchHostListings.mockResolvedValue(Array.from({ length: 101 }, (_, page) => ({ id: `listing-${page}`, hostId: 'h', status: 'ACTIVE' as const, title: `Cabin ${page}`, description: '', propertyType: 'HOUSE', streetAddress: '1 Main', city: 'Reno', region: 'NV', postalCode: 89501, maxGuests: 2, bedrooms: 1, bathrooms: 1, baseNightlyRate: 90, checkInTime: '15:00:00', checkOutTime: '11:00:00', amenities: [], createdAt: '' })));
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: [] });
    render(<HostApiProvider><HostFixtureProvider><HostListingsPage /></HostFixtureProvider></HostApiProvider>);
    await screen.findByText('Cabin 0');
    expect(screen.getByText('Cabin 100')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Next' })).not.toBeInTheDocument();
    expect(fetchHostListings).toHaveBeenCalledTimes(1);
    expect(fetchHostListings).toHaveBeenCalledWith();
  });
});
