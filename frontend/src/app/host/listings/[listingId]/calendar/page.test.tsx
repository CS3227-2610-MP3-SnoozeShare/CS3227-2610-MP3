import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import HostCalendarPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';
import { HostApiProvider } from '@/components/host/HostApiProvider';
import { BackendApiError } from '@/lib/api';

vi.mock('next/navigation', () => ({ useParams: () => ({ listingId: 'lakeside-cabin' }) }));
const { fetchHostCalendar, fetchHostListing, fetchHostWallet, createHostBlock, deleteHostBlock } = vi.hoisted(() => ({ fetchHostCalendar: vi.fn(), fetchHostListing: vi.fn(), fetchHostWallet: vi.fn(), createHostBlock: vi.fn(), deleteHostBlock: vi.fn() }));
vi.mock('@/lib/hostApi', async (importOriginal) => ({ ...await importOriginal<typeof import('@/lib/hostApi')>(), fetchHostCalendar, fetchHostListing, fetchHostWallet, createHostBlock, deleteHostBlock, topUpHostWallet: vi.fn(), withdrawFromHostWallet: vi.fn() }));
const currentCalendar = () => { const now = new Date(); return { year: now.getFullYear(), month: now.getMonth() + 1, days: [{ date: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`, booked: false, blocked: false }], blocks: [] }; };

describe('Host calendar page', () => {
  it('renders the calendar without a duplicate page title', () => {
    render(<HostFixtureProvider><HostCalendarPage /></HostFixtureProvider>);
    expect(screen.getByRole('heading', { name: 'September 2026' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Booking Calendar' })).not.toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Breadcrumb' })).toHaveTextContent('Listings');
    expect(screen.getByRole('heading', { name: 'Block dates' })).toBeInTheDocument();
  });
  it('loads the selected API month and listing detail', async () => {
    fetchHostCalendar.mockResolvedValue(currentCalendar());
    fetchHostListing.mockResolvedValue({ listing: { id: 'lakeside-cabin', hostId: 'h', status: 'ACTIVE', title: 'API Cabin', description: '', propertyType: 'HOUSE', streetAddress: '1 Main', city: 'Reno', region: 'NV', postalCode: 89501, maxGuests: 2, bedrooms: 1, bathrooms: 1, baseNightlyRate: 90, checkInTime: '15:00:00', checkOutTime: '11:00:00', amenities: [], createdAt: '' }, metrics: { bookingCount: 0, averageRating: 0, reviews: [], occupancyPercentage: 0, earnings: 0 } });
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: { items: [], page: 0, size: 1, hasMore: false } });
    render(<HostApiProvider><HostFixtureProvider><HostCalendarPage /></HostFixtureProvider></HostApiProvider>);
    await screen.findByText('API Cabin');
    const now = new Date();
    const monthLabel = new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(now);
    expect(screen.getByRole('heading', { name: monthLabel })).toBeInTheDocument();
    expect(fetchHostCalendar).toHaveBeenCalledWith('lakeside-cabin', now.getFullYear(), now.getMonth() + 1);
  });
  it('posts a manual block and reloads the authoritative calendar', async () => {
    fetchHostCalendar.mockResolvedValue(currentCalendar());
    fetchHostListing.mockResolvedValue({ listing: { id: 'lakeside-cabin', hostId: 'h', status: 'ACTIVE', title: 'API Cabin', description: '', propertyType: 'HOUSE', streetAddress: '1 Main', city: 'Reno', region: 'NV', postalCode: 89501, maxGuests: 2, bedrooms: 1, bathrooms: 1, baseNightlyRate: 90, checkInTime: '15:00:00', checkOutTime: '11:00:00', amenities: [], createdAt: '' }, metrics: { bookingCount: 0, averageRating: 0, reviews: [], occupancyPercentage: 0, earnings: 0 } });
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: { items: [], page: 0, size: 1, hasMore: false } });
    createHostBlock.mockResolvedValue({ id: 'b-1' });
    render(<HostApiProvider><HostFixtureProvider><HostCalendarPage /></HostFixtureProvider></HostApiProvider>);
    const now = new Date();
    const monthLabel = new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(now);
    await screen.findByRole('heading', { name: monthLabel });
    fireEvent.change(screen.getByLabelText('Reason'), { target: { value: 'Maintenance' } });
    fireEvent.click(screen.getByRole('button', { name: 'Block these dates' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirm' }));
    const startDay = String(Math.max(2, now.getDate() + 1)).padStart(2, '0');
    const endDay = String(Math.min(Number(startDay) + 2, new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate())).padStart(2, '0');
    const month = String(now.getMonth() + 1).padStart(2, '0');
    await waitFor(() => expect(createHostBlock).toHaveBeenCalledWith('lakeside-cabin', { startDate: `${now.getFullYear()}-${month}-${startDay}`, endDate: `${now.getFullYear()}-${month}-${endDay}`, reason: 'Maintenance' }));
    await waitFor(() => expect(fetchHostCalendar).toHaveBeenCalledTimes(2));
  });
  it('loads a new API month when navigating the calendar', async () => {
    fetchHostCalendar.mockImplementation((_id: string, year: number, month: number) => Promise.resolve({ year, month, days: [{ date: `${year}-${String(month).padStart(2, '0')}-01`, booked: false, blocked: false }], blocks: [] }));
    fetchHostListing.mockResolvedValue({ listing: { id: 'lakeside-cabin', hostId: 'h', status: 'ACTIVE', title: 'API Cabin', description: '', propertyType: 'HOUSE', streetAddress: '1 Main', city: 'Reno', region: 'NV', postalCode: 89501, maxGuests: 2, bedrooms: 1, bathrooms: 1, baseNightlyRate: 90, checkInTime: '15:00:00', checkOutTime: '11:00:00', amenities: [], createdAt: '' }, metrics: { bookingCount: 0, averageRating: 0, reviews: [], occupancyPercentage: 0, earnings: 0 } });
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: { items: [], page: 0, size: 1, hasMore: false } });
    render(<HostApiProvider><HostFixtureProvider><HostCalendarPage /></HostFixtureProvider></HostApiProvider>);
    await screen.findByRole('heading', { name: 'October 2026' });
    fireEvent.click(screen.getByRole('button', { name: 'Next month' }));
    await waitFor(() => expect(fetchHostCalendar).toHaveBeenCalledWith('lakeside-cabin', 2026, 11));
  });
  it('shows a safe unavailable state when the API says the listing does not exist', async () => {
    const notFound = () => new BackendApiError('NOT_FOUND', 'private backend details', 'request-1', 404);
    fetchHostCalendar.mockRejectedValue(notFound());
    fetchHostListing.mockRejectedValue(notFound());
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: { items: [], page: 0, size: 1, hasMore: false } });
    render(<HostApiProvider><HostFixtureProvider><HostCalendarPage /></HostFixtureProvider></HostApiProvider>);

    expect(await screen.findByRole('heading', { name: 'Something went wrong' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to Listings' })).toHaveAttribute('href', '/host/listings');
    expect(screen.queryByText('private backend details')).not.toBeInTheDocument();
  });
});
