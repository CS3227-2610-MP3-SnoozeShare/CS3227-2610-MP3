import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { HostFixtureProvider } from './HostFixtureProvider';
import { createHostFixtureAdapter } from './hostFixtures';
import { HostListingCard } from './HostListingCard';
import { HostListingDetail } from './HostListingDetail';
import { HostListingForm } from './HostListingForm';
import HostListingsPage from '@/app/host/listings/page';

vi.mock('next/navigation', () => ({ useRouter: () => ({ push: vi.fn() }) }));

describe('Host listing components', () => {
  it('renders listing card metrics, fallback thumbnail, statuses, and links', () => {
    const listing = createHostFixtureAdapter().getListings().data![0];
    render(<HostListingCard listing={listing} />);
    expect(screen.getByRole('img', { name: 'Lakeside Cabin thumbnail' })).toBeInTheDocument();
    expect(screen.getByText('BOOKINGS')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open Booking Calendar' })).toHaveAttribute('href', '/host/listings/lakeside-cabin/calendar');
    expect(screen.getAllByText('ACTIVE').length).toBeGreaterThanOrEqual(1);
  });

  it('exposes listing availability as a labelled switch', () => {
    const listing = createHostFixtureAdapter().getListings().data![0];
    const onToggle = vi.fn();
    render(<HostListingCard listing={listing} onToggle={onToggle} />);

    expect(screen.getByRole('switch', { name: 'Lakeside Cabin availability' })).toHaveAttribute('aria-checked', 'true');
    fireEvent.click(screen.getByRole('switch', { name: 'Lakeside Cabin availability' }));
    expect(onToggle).toHaveBeenCalledOnce();
  });

  it('changes listing availability locally when the switch is toggled', () => {
    render(<HostFixtureProvider><HostListingsPage /></HostFixtureProvider>);
    const toggle = screen.getByRole('switch', { name: 'Lakeside Cabin availability' });

    expect(toggle).toHaveAttribute('aria-checked', 'true');
    fireEvent.click(toggle);
    expect(screen.getByRole('switch', { name: 'Lakeside Cabin availability' })).toHaveAttribute('aria-checked', 'false');
    expect(within(toggle.closest('article')!).getAllByText('INACTIVE')).toHaveLength(2);
  });

  it('renders exact Lakeside detail and performance content', () => {
    const listing = createHostFixtureAdapter().getListing('lakeside-cabin').data!;
    render(<HostListingDetail listing={listing} />);
    expect(screen.getByText('142 Shoreline Dr, Lakeview, WI 53211')).toBeInTheDocument();
    expect(screen.getByText('AMENITIES')).toBeInTheDocument();
    expect(screen.getByText('62%')).toBeInTheDocument();
    expect(screen.getByText('$1,860.00')).toBeInTheDocument();
    expect(screen.getByText('Exactly as pictured, host was very responsive.')).toBeInTheDocument();
    expect(screen.getByText('Loved the dock. Would book again.')).toBeInTheDocument();
  });

  it('renders form sections and exact required-field validation', async () => {
    render(<HostFixtureProvider adapter={createHostFixtureAdapter('empty')}><HostListingForm /></HostFixtureProvider>);
    expect(screen.getByText('BASIC DETAILS')).toBeInTheDocument();
    expect(screen.getByText('CAPACITY & PRICING')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Save listing' }));
    await waitFor(() => expect(screen.getByText('Title is required')).toBeInTheDocument());
    expect(screen.getByText('Rate / night must be positive with at most 2 decimal places')).toBeInTheDocument();
  });

  it('loads saved amenities and presents selected status with normal-weight values', () => {
    const listing = createHostFixtureAdapter().getListing('lakeside-cabin').data!;
    render(<HostFixtureProvider><HostListingForm listing={listing} /></HostFixtureProvider>);

    for (const amenity of ['Wifi', 'Parking', 'Kitchen', 'Washer']) {
      expect(screen.getByRole('checkbox', { name: amenity })).toBeChecked();
    }
    expect(screen.getByRole('combobox', { name: 'Status' })).toHaveTextContent('Active');
    expect(screen.getByLabelText('Title')).toHaveClass('font-normal');
    expect(screen.getByLabelText('Description')).toHaveClass('font-normal');
  });
});
