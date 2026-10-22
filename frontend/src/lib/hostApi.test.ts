import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BackendApiError } from './api';
import { formatTime, mapHostListing, mapHostListingDetail, mapHostWallet, mapTicketConversation } from '@/components/host/hostTypes';

const { authenticatedFetch, signOutUser } = vi.hoisted(() => ({ authenticatedFetch: vi.fn(), signOutUser: vi.fn() }));
vi.mock('./api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('./api')>();
  return { ...actual, authenticatedFetch };
});
vi.mock('./firebase', () => ({ auth: { currentUser: null }, signOutUser }));

describe('Host API adapter', () => {
  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
  });

  it('loads complete Host collections without pagination query parameters', async () => {
    const rows = Array.from({ length: 101 }, (_, index) => ({ id: `row-${index}` }));
    authenticatedFetch.mockImplementation(async (path: string) => new Response(JSON.stringify(
      path === '/api/host/wallet'
        ? { wallet: { id: 'wallet-id', userId: 'host-id', balance: 10, currency: 'SGD', updatedAt: '2026-10-22T00:00:00Z' }, statement: rows }
        : rows,
    ), { status: 200 }));
    const { fetchHostListings, fetchHostRequests, fetchBookingConversations, fetchBookingMessages,
      fetchTicketConversations, fetchTicketMessages, fetchHostWallet } = await import('./hostApi');

    await expect(fetchHostListings()).resolves.toHaveLength(101);
    await expect(fetchHostRequests('pending')).resolves.toHaveLength(101);
    await expect(fetchBookingConversations()).resolves.toHaveLength(101);
    await expect(fetchBookingMessages('booking-id')).resolves.toHaveLength(101);
    await expect(fetchTicketConversations()).resolves.toHaveLength(101);
    await expect(fetchTicketMessages('ticket-id')).resolves.toHaveLength(101);
    await expect(fetchHostWallet()).resolves.toMatchObject({ statement: rows });
    expect(authenticatedFetch.mock.calls.map(([path]) => path)).toEqual([
      '/api/host/listings', '/api/host/requests?view=pending', '/api/host/messages/bookings',
      '/api/host/messages/bookings/booking-id', '/api/host/messages/tickets',
      '/api/host/messages/tickets/ticket-id?channel=HOST', '/api/host/wallet',
    ]);
    expect(JSON.stringify(authenticatedFetch.mock.calls)).not.toMatch(/page=|size=|hostId/);
  });

  it('sends wallet mutations without host identity or automatic retry metadata', async () => {
    authenticatedFetch.mockResolvedValue(new Response(JSON.stringify({
      mocked: true,
      wallet: { id: 'wallet-id', balance: 50 },
      transaction: { id: 'transaction-id', amount: 10 },
    }), { status: 200 }));
    const { topUpHostWallet } = await import('./hostApi');

    await expect(topUpHostWallet(10)).resolves.toMatchObject({ mocked: true });
    expect(authenticatedFetch).toHaveBeenCalledWith('/api/host/wallet/top-up', {
      method: 'POST', body: JSON.stringify({ amount: 10 }),
    });
    expect(JSON.stringify(authenticatedFetch.mock.calls)).not.toMatch(/hostId|Idempotency-Key|idempotency/i);
  });

  it('preserves stable backend errors for the provider to render safely', async () => {
    const error = new BackendApiError('STATE_CONFLICT', 'Request could not be completed', 'req-1', 409);
    authenticatedFetch.mockRejectedValue(error);
    const { fetchHostWallet } = await import('./hostApi');

    await expect(fetchHostWallet()).rejects.toBe(error);
  });

  it('routes a rejected Firebase token through the existing sign-out recovery path', async () => {
    authenticatedFetch.mockRejectedValue(new BackendApiError('AUTH_REQUIRED', 'Sign-in is required', undefined, 401));
    const { fetchHostWallet } = await import('./hostApi');

    await expect(fetchHostWallet()).rejects.toMatchObject({ status: 401 });
    expect(signOutUser).toHaveBeenCalledOnce();
  });

  it('maps backend failures to fixed user-safe copy', async () => {
    const { hostSafeErrorMessage } = await import('./hostApi');
    expect(hostSafeErrorMessage(new BackendApiError('STATE_CONFLICT', 'internal details', 'req', 409)))
      .toBe('This item changed. Refresh and try again.');
    expect(hostSafeErrorMessage(new BackendApiError('BOOKING_OVERLAP', 'internal details', 'req', 409)))
      .toBe('These dates overlap an existing booking. Choose another date range.');
    expect(hostSafeErrorMessage(new BackendApiError('HOST_BLOCK_OVERLAP', 'internal details', 'req', 409)))
      .toBe('These dates overlap a date range you have already blocked. Choose another date range.');
    expect(hostSafeErrorMessage(new BackendApiError('DATE_RANGE_UNAVAILABLE', 'internal details', 'req', 409)))
      .toBe('These dates overlap an existing booking or blocked period. Choose another date range.');
    expect(hostSafeErrorMessage(new Error('secret or SQL details')))
      .toBe('Unable to complete this request. Please try again.');
  });

  it('maps backend listing and wallet DTOs into the existing presentation types', () => {
    const listing = {
      id: 'l-1', hostId: 'h-1', status: 'ACTIVE' as const, title: 'Cabin', description: 'Lake view',
      propertyType: 'PRIVATE_ROOM', streetAddress: '1 Main St', city: 'Lakeview', region: 'WI', postalCode: 53000,
      maxGuests: 2, bedrooms: 1, bathrooms: 1, baseNightlyRate: 80, checkInTime: '15:00:00', checkOutTime: '11:00:00', amenities: ['WIFI'], createdAt: '2026-10-22T00:00:00Z',
    };
    expect(mapHostListing({ ...listing, bookingCount: 7, averageRating: 4.5 })).toMatchObject({ type: 'Private Room', rate: 80, status: 'active', bookings: 7, rating: 4.5 });
    expect(mapHostListingDetail({ listing, metrics: { bookingCount: 3, averageRating: 4.5, reviews: [{ guestName: 'Guest', rating: 5, comment: 'Great' }], occupancyPercentage: 60, earnings: 240 } })).toMatchObject({ bookings: 3, rating: 4.5, reviews: [{ author: 'Guest', text: 'Great' }] });
    expect(mapHostWallet({ wallet: { id: 'w-1', userId: 'h-1', balance: 20, currency: 'SGD', updatedAt: '2026-10-22T00:00:00Z' }, statement: [{ id: 'tx-1', actionType: 'TOP_UP', walletAdjustment: 20, balanceAfter: 20, reason: 'Top-up', bookingId: null, ticketId: null, timestamp: '2026-10-22T00:00:00Z' }] })).toMatchObject({ wallet: { balance: 20 }, statement: [{ id: 'tx-1', type: 'TOP_UP', amount: 20, relatedTo: '—' }] });
  });

  it('shows one hash when a ticket conversation label already includes its marker', () => {
    const conversation = mapTicketConversation({
      ticketId: 'ticket-1', channel: 'HOST', ticketLabel: '#b023', ticketTitle: 'Support question',
      listingTitle: 'Cabin', counterpartName: 'Agent', lastMessage: null, unreadCount: 0, open: true,
    });

    expect(conversation.subtitle).toBe('Ticket #b023 · Cabin · Support Agent');
  });

  it('shows one hash for booking and ticket references in wallet rows', () => {
    const mapped = mapHostWallet({
      wallet: { id: 'w-1', userId: 'h-1', balance: 20, currency: 'SGD', updatedAt: '2026-10-22T00:00:00Z' },
      statement: [
        { id: 'tx-booking', actionType: 'BOOKING_PAYOUT', walletAdjustment: 20, balanceAfter: 20, reason: 'Payout', bookingId: '#B-2255', ticketId: null, timestamp: '2026-10-22T00:00:00Z' },
        { id: 'tx-ticket', actionType: 'TICKET_REMEDY', walletAdjustment: 20, balanceAfter: 40, reason: 'Remedy', bookingId: null, ticketId: '##b023', timestamp: '2026-10-22T00:00:00Z' },
      ],
    });

    expect(mapped.statement.map((row) => row.relatedTo)).toEqual(['Booking #B-2255', 'Ticket #b023']);
  });

  it.each(['Asia/Singapore', 'America/Los_Angeles'])('preserves time-only listing values in %s', (timezone) => {
    const originalTimezone = process.env.TZ;
    process.env.TZ = timezone;
    try {
      const listing = {
        id: 'l-1', hostId: 'h-1', status: 'ACTIVE' as const, title: 'Cabin', description: '',
        propertyType: 'HOUSE', streetAddress: '1 Main', city: 'Lakeview', region: 'WI', postalCode: 53000,
        maxGuests: 2, bedrooms: 1, bathrooms: 1, baseNightlyRate: 80,
        checkInTime: '07:00:00', checkOutTime: '03:00:00', amenities: [], createdAt: '',
      };
      const detail = mapHostListingDetail({ listing, metrics: {
        bookingCount: 0, averageRating: 0, reviews: [], occupancyPercentage: 0, earnings: 0,
      } });

      expect(detail.checkIn).toBe('07:00 AM');
      expect(detail.checkOut).toBe('03:00 AM');
    } finally {
      if (originalTimezone === undefined) delete process.env.TZ;
      else process.env.TZ = originalTimezone;
    }
  });

  it.each([
    ['00:05:00', '12:05 AM'],
    ['12:05:00', '12:05 PM'],
    ['15:05:00', '03:05 PM'],
  ])('formats listing time %s as %s', (value, expected) => {
    expect(formatTime(value)).toBe(expected);
  });
});
