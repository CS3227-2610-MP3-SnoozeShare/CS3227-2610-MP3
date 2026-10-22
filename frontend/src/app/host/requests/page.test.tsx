import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import HostRequestsPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';
import { HostApiProvider } from '@/components/host/HostApiProvider';

const { fetchHostRequests, fetchHostWallet, decideHostBooking } = vi.hoisted(() => ({ fetchHostRequests: vi.fn(), fetchHostWallet: vi.fn(), decideHostBooking: vi.fn() }));
vi.mock('@/lib/hostApi', async (importOriginal) => ({ ...await importOriginal<typeof import('@/lib/hostApi')>(), fetchHostRequests, fetchHostWallet, decideHostBooking, topUpHostWallet: vi.fn(), withdrawFromHostWallet: vi.fn() }));

describe('Host requests page', () => {
  it('groups the pending count directly beside the page title and labels history like the artifact', () => {
    render(<HostFixtureProvider><HostRequestsPage /></HostFixtureProvider>);
    const title = screen.getByRole('heading', { name: 'Booking requests' });
    const titleGroup = title.parentElement!;

    expect(titleGroup).toHaveClass('inline-flex', 'items-center', 'gap-2');
    expect(titleGroup).toContainElement(screen.getByText('8 pending'));
    expect(screen.queryByRole('heading', { name: 'Pending requests' })).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'PAST REQUESTS' })).toBeInTheDocument();
  });

  it('loads complete pending and history collections from the API', async () => {
    fetchHostRequests.mockImplementation((view: string) => Promise.resolve(view === 'pending' ? [{ booking: { id: 'api-booking', status: 'PENDING', startDate: '2026-11-01', endDate: '2026-11-03', totalAmount: 200 }, guestDisplayName: 'API Guest', listingTitle: 'API Listing', nights: 2, grossAmount: 200, projectedNetAmount: 194, guestAverageRating: null }] : []));
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: [] });
    render(<HostApiProvider><HostFixtureProvider><HostRequestsPage /></HostFixtureProvider></HostApiProvider>);
    expect(await screen.findByText('API Guest')).toBeInTheDocument();
    expect(fetchHostRequests).toHaveBeenCalledWith('pending');
    expect(fetchHostRequests).toHaveBeenCalledWith('history');
  });

  it('submits a booking decision and refetches server-owned request pages', async () => {
    fetchHostRequests.mockImplementation((view: string) => Promise.resolve(view === 'pending' ? [{ booking: { id: 'booking-7', status: 'PENDING', startDate: '2026-11-01', endDate: '2026-11-03' }, guestDisplayName: 'API Guest', listingTitle: 'API Listing', nights: 2, grossAmount: 200, projectedNetAmount: 194, guestAverageRating: null }] : []));
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: [] });
    decideHostBooking.mockResolvedValue({ id: 'booking-7', status: 'CONFIRMED' });
    render(<HostApiProvider><HostFixtureProvider><HostRequestsPage /></HostFixtureProvider></HostApiProvider>);
    await screen.findByText('API Guest');
    fireEvent.click(screen.getByRole('button', { name: 'Approve' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirm approve' }));
    await waitFor(() => expect(decideHostBooking).toHaveBeenCalledWith('booking-7', true, undefined));
    await waitFor(() => expect(fetchHostRequests).toHaveBeenCalledTimes(4));
  });

  it('renders all pending requests from one complete collection request', async () => {
    fetchHostRequests.mockImplementation((view: string) => Promise.resolve(view === 'pending' ? Array.from({ length: 101 }, (_, index) => ({ booking: { id: `booking-${index}`, status: 'PENDING', startDate: '2026-11-01', endDate: '2026-11-03' }, guestDisplayName: `Guest ${index}`, listingTitle: 'API Listing', nights: 2, grossAmount: 200, projectedNetAmount: 194, guestAverageRating: null })) : []));
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: [] });
    render(<HostApiProvider><HostFixtureProvider><HostRequestsPage /></HostFixtureProvider></HostApiProvider>);
    await screen.findByText('Guest 0');
    expect(screen.getByText('Guest 100')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Next pending' })).not.toBeInTheDocument();
    expect(fetchHostRequests).toHaveBeenCalledTimes(2);
  });
});
