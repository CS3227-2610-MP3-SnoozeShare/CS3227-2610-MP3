import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HostApiProvider } from './HostApiProvider';
import { HostFixtureProvider } from './HostFixtureProvider';
import { HostListingForm } from './HostListingForm';

const { createHostListing, fetchHostWallet, updateHostListing, updateHostListingStatus } = vi.hoisted(() => ({
  createHostListing: vi.fn(), fetchHostWallet: vi.fn(), updateHostListing: vi.fn(), updateHostListingStatus: vi.fn(),
}));
const { push } = vi.hoisted(() => ({ push: vi.fn() }));
vi.mock('@/lib/hostApi', async (importOriginal) => ({
  ...await importOriginal<typeof import('@/lib/hostApi')>(),
  createHostListing, fetchHostWallet, updateHostListing, updateHostListingStatus,
  topUpHostWallet: vi.fn(), withdrawFromHostWallet: vi.fn(),
}));
vi.mock('next/navigation', () => ({ useRouter: () => ({ push }), useParams: () => ({ listingId: 'listing-1' }) }));

function fillRequiredFields() {
  fireEvent.change(screen.getByLabelText('Title'), { target: { value: 'New Cabin' } });
  fireEvent.change(screen.getByLabelText('Description'), { target: { value: 'A quiet cabin.' } });
  fireEvent.change(screen.getByLabelText('Street address'), { target: { value: '1 Lake Rd' } });
  fireEvent.change(screen.getByLabelText('City'), { target: { value: 'Reno' } });
  fireEvent.change(screen.getByLabelText('Region'), { target: { value: 'NV' } });
  fireEvent.change(screen.getByLabelText('Postal code'), { target: { value: '89501' } });
  fireEvent.change(screen.getByLabelText('Max guests'), { target: { value: '2' } });
  fireEvent.change(screen.getByLabelText('Bedrooms'), { target: { value: '1' } });
  fireEvent.change(screen.getByLabelText('Bathrooms'), { target: { value: '1' } });
  fireEvent.change(screen.getByLabelText('Rate / night'), { target: { value: '125.50' } });
  fireEvent.change(screen.getByLabelText('Check-in time'), { target: { value: '15:00' } });
  fireEvent.change(screen.getByLabelText('Check-out time'), { target: { value: '11:00' } });
}

describe('HostListingForm with the API provider', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 1, currency: 'SGD', updatedAt: '' }, statement: { items: [], page: 0, size: 1, hasMore: false } });
    createHostListing.mockResolvedValue({ id: 'new-id' });
  });

  it('creates through the API with validated values and no client-selected host ID', async () => {
    render(<HostApiProvider><HostFixtureProvider><HostListingForm /></HostFixtureProvider></HostApiProvider>);
    fillRequiredFields();
    fireEvent.click(screen.getByRole('button', { name: 'Save listing' }));

    await waitFor(() => expect(createHostListing).toHaveBeenCalledOnce());
    const payload = createHostListing.mock.calls[0][0];
    expect(payload).toMatchObject({ title: 'New Cabin', propertyType: 'HOUSE', status: 'ACTIVE', postalCode: 89501, baseNightlyRate: 125.5, checkInTime: '15:00' });
    expect(payload).not.toHaveProperty('hostId');
    expect(push).toHaveBeenCalledWith('/host/listings');
  });

  it('shows a safe error and does not navigate when create fails', async () => {
    createHostListing.mockRejectedValue(new Error('database connection string'));
    render(<HostApiProvider><HostFixtureProvider><HostListingForm /></HostFixtureProvider></HostApiProvider>);
    fillRequiredFields();
    fireEvent.click(screen.getByRole('button', { name: 'Save listing' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to complete this request. Please try again.');
    expect(push).not.toHaveBeenCalled();
  });
});
