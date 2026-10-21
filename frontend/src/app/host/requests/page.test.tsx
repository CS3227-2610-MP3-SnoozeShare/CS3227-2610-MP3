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

  it('loads bounded pending and history pages from the API', async () => {
    fetchHostRequests.mockImplementation((view: string) => Promise.resolve({ items: view === 'pending' ? [{ booking: { id: 'api-booking', status: 'PENDING', startDate: '2026-11-01', endDate: '2026-11-03', totalAmount: 200 }, guestDisplayName: 'API Guest', listingTitle: 'API Listing', nights: 2, grossAmount: 200, projectedNetAmount: 194, guestAverageRating: null }] : [], page: 0, size: 50, hasMore: false }));
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: { items: [], page: 0, size: 1, hasMore: false } });
    render(<HostApiProvider><HostFixtureProvider><HostRequestsPage /></HostFixtureProvider></HostApiProvider>);
    expect(await screen.findByText('API Guest')).toBeInTheDocument();
    expect(fetchHostRequests).toHaveBeenCalledWith('pending', 0, 50);
    expect(fetchHostRequests).toHaveBeenCalledWith('history', 0, 50);
  });

  it('submits a booking decision and refetches server-owned request pages', async () => {
    fetchHostRequests.mockImplementation((view: string) => Promise.resolve({ items: view === 'pending' ? [{ booking: { id: 'booking-7', status: 'PENDING', startDate: '2026-11-01', endDate: '2026-11-03' }, guestDisplayName: 'API Guest', listingTitle: 'API Listing', nights: 2, grossAmount: 200, projectedNetAmount: 194, guestAverageRating: null }] : [], page: 0, size: 50, hasMore: false }));
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: { items: [], page: 0, size: 1, hasMore: false } });
    decideHostBooking.mockResolvedValue({ id: 'booking-7', status: 'CONFIRMED' });
    render(<HostApiProvider><HostFixtureProvider><HostRequestsPage /></HostFixtureProvider></HostApiProvider>);
    await screen.findByText('API Guest');
    fireEvent.click(screen.getByRole('button', { name: 'Approve' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirm approve' }));
    await waitFor(() => expect(decideHostBooking).toHaveBeenCalledWith('booking-7', true, undefined));
    await waitFor(() => expect(fetchHostRequests).toHaveBeenCalledTimes(4));
  });

  it('pages pending requests only when hasMore is true', async () => {
    fetchHostRequests.mockImplementation((view: string, page: number) => Promise.resolve({ items: view === 'pending' ? [{ booking: { id: `booking-${page}`, status: 'PENDING', startDate: '2026-11-01', endDate: '2026-11-03' }, guestDisplayName: `Guest ${page}`, listingTitle: 'API Listing', nights: 2, grossAmount: 200, projectedNetAmount: 194, guestAverageRating: null }] : [], page, size: 50, hasMore: view === 'pending' && page === 0 }));
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: { items: [], page: 0, size: 1, hasMore: false } });
    render(<HostApiProvider><HostFixtureProvider><HostRequestsPage /></HostFixtureProvider></HostApiProvider>);
    await screen.findByText('Guest 0');
    fireEvent.click(screen.getByRole('button', { name: 'Next pending' }));
    await screen.findByText('Guest 1');
    expect(fetchHostRequests).toHaveBeenCalledWith('pending', 1, 50);
  });
});
