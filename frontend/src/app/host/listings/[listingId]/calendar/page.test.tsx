import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
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
    fireEvent.click(screen.getByRole('button', { name: 'Date range: Select dates' }));
    fireEvent.click(screen.getByRole('button', { name: String(Math.max(2, now.getDate() + 1)) }));
    fireEvent.click(screen.getByRole('button', { name: String(Math.max(3, now.getDate() + 3)) }));
    fireEvent.click(screen.getByRole('button', { name: 'Apply' }));
    fireEvent.change(screen.getByLabelText('Reason'), { target: { value: 'Maintenance' } });
    const startDay = String(Math.max(2, now.getDate() + 1)).padStart(2, '0');
    const endDay = String(Math.max(3, now.getDate() + 3)).padStart(2, '0');
    const month = String(now.getMonth() + 1).padStart(2, '0');
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Block these dates' }));
      await waitFor(() => expect(createHostBlock).toHaveBeenCalledWith('lakeside-cabin', { startDate: `${now.getFullYear()}-${month}-${startDay}`, endDate: `${now.getFullYear()}-${month}-${endDay}`, reason: 'Maintenance' }));
      await waitFor(() => expect(fetchHostCalendar).toHaveBeenCalledTimes(2));
    });
    expect(screen.queryByRole('button', { name: 'Confirm' })).not.toBeInTheDocument();
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
  it('keeps the calendar form mounted while a requested month loads, then swaps the grid and blocks', async () => {
    let resolveNovember!: (value: ReturnType<typeof currentCalendar>) => void;
    fetchHostCalendar.mockImplementation((_id: string, year: number, month: number) => {
      if (month === 11) return new Promise((resolve) => { resolveNovember = resolve; });
      return Promise.resolve({ ...currentCalendar(), year, month, blocks: [{ id: 'oct-block', reason: 'October repair', startDate: '2026-10-05', endDate: '2026-10-06' }] });
    });
    fetchHostListing.mockResolvedValue({ listing: { id: 'lakeside-cabin', hostId: 'h', status: 'ACTIVE', title: 'API Cabin', description: '', propertyType: 'HOUSE', streetAddress: '1 Main', city: 'Reno', region: 'NV', postalCode: 89501, maxGuests: 2, bedrooms: 1, bathrooms: 1, baseNightlyRate: 90, checkInTime: '15:00:00', checkOutTime: '11:00:00', amenities: [], createdAt: '' }, metrics: { bookingCount: 0, averageRating: 0, reviews: [], occupancyPercentage: 0, earnings: 0 } });
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: { items: [], page: 0, size: 1, hasMore: false } });
    render(<HostApiProvider><HostFixtureProvider><HostCalendarPage /></HostFixtureProvider></HostApiProvider>);
    await screen.findByRole('heading', { name: 'October 2026' });
    expect(screen.getByRole('button', { name: 'Date range: Select dates' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Date range: Select dates' }));
    fireEvent.click(screen.getByRole('button', { name: '10' }));
    fireEvent.click(screen.getByRole('button', { name: '12' }));
    fireEvent.click(screen.getByRole('button', { name: 'Apply' }));
    fireEvent.change(screen.getByLabelText('Reason'), { target: { value: 'Guest request' } });
    fireEvent.click(screen.getByRole('button', { name: 'Next month' }));

    expect(screen.getByRole('heading', { name: 'November 2026' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Booking calendar and date blocking' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Date range: Oct 10 – Oct 12, 2026' })).toBeInTheDocument();
    expect(screen.getByLabelText('Reason')).toHaveValue('Guest request');
    expect(screen.getByText('October repair')).toBeInTheDocument();
    expect(fetchHostListing).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('status')).toHaveTextContent(/loading calendar/i);
    expect(screen.queryByRole('grid')).not.toBeInTheDocument();

    resolveNovember({ year: 2026, month: 11, days: [{ date: '2026-11-01', booked: true, blocked: false }], blocks: [{ id: 'nov-block', reason: 'New month block', startDate: '2026-11-05', endDate: '2026-11-06' }] });
    expect(await screen.findByText('New month block')).toBeInTheDocument();
    expect(screen.queryByText('October repair')).not.toBeInTheDocument();
    expect(screen.getByRole('gridcell', { name: '1, Booked' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Date range: Oct 10 – Oct 12, 2026' })).toBeInTheDocument();
    expect(screen.getByLabelText('Reason')).toHaveValue('Guest request');
  });
  it('shows the generic listing error when a previous-month request fails', async () => {
    fetchHostCalendar.mockImplementation((_id: string, year: number, month: number) => month === 9
      ? Promise.reject(new Error('private calendar failure'))
      : Promise.resolve({ ...currentCalendar(), year, month }));
    fetchHostListing.mockResolvedValue({ listing: { id: 'lakeside-cabin', hostId: 'h', status: 'ACTIVE', title: 'API Cabin', description: '', propertyType: 'HOUSE', streetAddress: '1 Main', city: 'Reno', region: 'NV', postalCode: 89501, maxGuests: 2, bedrooms: 1, bathrooms: 1, baseNightlyRate: 90, checkInTime: '15:00:00', checkOutTime: '11:00:00', amenities: [], createdAt: '' }, metrics: { bookingCount: 0, averageRating: 0, reviews: [], occupancyPercentage: 0, earnings: 0 } });
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: { items: [], page: 0, size: 1, hasMore: false } });
    render(<HostApiProvider><HostFixtureProvider><HostCalendarPage /></HostFixtureProvider></HostApiProvider>);
    await screen.findByRole('heading', { name: 'October 2026' });
    fireEvent.click(screen.getByRole('button', { name: 'Previous month' }));

    expect(await screen.findByRole('heading', { name: 'Something went wrong' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to Listings' })).toHaveAttribute('href', '/host/listings');
    expect(screen.queryByRole('heading', { name: 'September 2026' })).not.toBeInTheDocument();
    expect(screen.queryByText('private calendar failure')).not.toBeInTheDocument();
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
