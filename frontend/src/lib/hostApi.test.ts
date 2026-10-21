import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BackendApiError } from './api';
import { mapHostListing, mapHostListingDetail, mapHostWallet } from '@/components/host/hostTypes';

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

  it('requests bounded Host listing pages through authenticatedFetch without a host UUID', async () => {
    authenticatedFetch.mockResolvedValue(new Response(JSON.stringify({
      items: [{ id: 'listing-id', title: 'Cabin' }], page: 2, size: 25, hasMore: true,
    }), { status: 200 }));
    const { fetchHostListings } = await import('./hostApi');

    await expect(fetchHostListings(2, 25)).resolves.toMatchObject({
      items: [{ id: 'listing-id' }], page: 2, size: 25, hasMore: true,
    });
    expect(authenticatedFetch).toHaveBeenCalledWith('/api/host/listings?page=2&size=25');
    expect(JSON.stringify(authenticatedFetch.mock.calls)).not.toContain('hostId');
  });

  it('rejects page sizes above the API limit instead of requesting an unbounded collection', async () => {
    const { fetchHostListings, fetchHostRequests } = await import('./hostApi');

    expect(() => fetchHostListings(0, 101)).toThrow('Page size must be between 1 and 100');
    expect(() => fetchHostRequests('pending', 0, 101)).toThrow('Page size must be between 1 and 100');
    expect(authenticatedFetch).not.toHaveBeenCalled();
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
    expect(hostSafeErrorMessage(new Error('secret or SQL details')))
      .toBe('Unable to complete this request. Please try again.');
  });

  it('maps backend listing and wallet DTOs into the existing presentation types', () => {
    const listing = {
      id: 'l-1', hostId: 'h-1', status: 'ACTIVE' as const, title: 'Cabin', description: 'Lake view',
      propertyType: 'PRIVATE_ROOM', streetAddress: '1 Main St', city: 'Lakeview', region: 'WI', postalCode: 53000,
      maxGuests: 2, bedrooms: 1, bathrooms: 1, baseNightlyRate: 80, checkInTime: '15:00:00', checkOutTime: '11:00:00', amenities: ['WIFI'], createdAt: '2026-10-22T00:00:00Z',
    };
    expect(mapHostListing(listing)).toMatchObject({ type: 'Private Room', rate: 80, status: 'active' });
    expect(mapHostListingDetail({ listing, metrics: { bookingCount: 3, averageRating: 4.5, reviews: [{ guestName: 'Guest', rating: 5, comment: 'Great' }], occupancyPercentage: 60, earnings: 240 } })).toMatchObject({ bookings: 3, rating: 4.5, reviews: [{ author: 'Guest', text: 'Great' }] });
    expect(mapHostWallet({ wallet: { id: 'w-1', userId: 'h-1', balance: 20, currency: 'SGD', updatedAt: '2026-10-22T00:00:00Z' }, statement: { items: [{ id: 'tx-1', actionType: 'TOP_UP', walletAdjustment: 20, balanceAfter: 20, reason: 'Top-up', bookingId: null, ticketId: null, timestamp: '2026-10-22T00:00:00Z' }], page: 0, size: 1, hasMore: false } })).toMatchObject({ wallet: { balance: 20 }, statement: [{ type: 'TOP_UP', amount: 20, relatedTo: '—' }] });
  });
});
