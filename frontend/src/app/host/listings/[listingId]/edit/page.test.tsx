import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import EditListingPage from './page';
import { HostApiProvider } from '@/components/host/HostApiProvider';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';
import { BackendApiError } from '@/lib/api';

const { useParams } = vi.hoisted(() => ({ useParams: vi.fn(() => ({ listingId: 'missing-id' })) }));
const { fetchHostListing, fetchHostWallet } = vi.hoisted(() => ({ fetchHostListing: vi.fn(), fetchHostWallet: vi.fn() }));
vi.mock('next/navigation', () => ({ useParams }));
vi.mock('@/lib/hostApi', async (importOriginal) => ({
  ...await importOriginal<typeof import('@/lib/hostApi')>(),
  fetchHostListing,
  fetchHostWallet,
}));

describe('Host edit listing route errors', () => {
  it('uses the generic listing error and return link when the listing cannot be loaded', async () => {
    fetchHostListing.mockRejectedValue(new BackendApiError('NOT_FOUND', 'private backend details', 'request-1', 404));
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: { items: [], page: 0, size: 1, hasMore: false } });

    render(<HostApiProvider><HostFixtureProvider><EditListingPage /></HostFixtureProvider></HostApiProvider>);

    expect(await screen.findByRole('heading', { name: 'Something went wrong' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to Listings' })).toHaveAttribute('href', '/host/listings');
    expect(screen.queryByText('private backend details')).not.toBeInTheDocument();
  });
});
