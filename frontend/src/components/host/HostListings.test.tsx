import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { HostFixtureProvider } from './HostFixtureProvider';
import { createHostFixtureAdapter } from './hostFixtures';
import { HostListingCard } from './HostListingCard';
import { HostListingDetail } from './HostListingDetail';
import { HostListingForm } from './HostListingForm';

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

  it('renders exact Lakeside detail and performance content', () => {
    const listing = createHostFixtureAdapter().getListing('lakeside-cabin').data!;
    render(<HostListingDetail listing={listing} />);
    expect(screen.getByText('142 Shoreline Dr, Lakeview, WI 53211')).toBeInTheDocument();
    expect(screen.getByText('AMENITIES')).toBeInTheDocument();
    expect(screen.getByText('62%')).toBeInTheDocument();
    expect(screen.getByText('$1,860.00')).toBeInTheDocument();
  });

  it('renders form sections and exact required-field validation', async () => {
    render(<HostFixtureProvider adapter={createHostFixtureAdapter('empty')}><HostListingForm /></HostFixtureProvider>);
    expect(screen.getByText('BASIC DETAILS')).toBeInTheDocument();
    expect(screen.getByText('CAPACITY & PRICING')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Save listing' }));
    await waitFor(() => expect(screen.getByText('Title is required')).toBeInTheDocument());
    expect(screen.getByText('Rate / night must be positive with at most 2 decimal places')).toBeInTheDocument();
  });
});
