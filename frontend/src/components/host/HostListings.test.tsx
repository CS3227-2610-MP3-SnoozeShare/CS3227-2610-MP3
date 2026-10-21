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
  it('aligns the New listing action in the page-title row', () => {
    render(<HostFixtureProvider><HostListingsPage /></HostFixtureProvider>);
    const heading = screen.getByRole('heading', { name: 'My listings' });
    const createLink = screen.getByRole('link', { name: '+ New listing' });
    expect(createLink.parentElement).toBe(heading.parentElement);
    expect(heading.parentElement).toHaveClass('flex', 'items-center', 'justify-between');
  });

  it('renders listing card metrics, fallback thumbnail, statuses, and links', () => {
    const listing = createHostFixtureAdapter().getListings().data![0];
    render(<HostListingCard listing={listing} />);
    expect(screen.getByRole('img', { name: 'Lakeside Cabin thumbnail' })).toBeInTheDocument();
    expect(screen.getByText('BOOKINGS')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open Booking Calendar' })).toHaveAttribute('href', '/host/listings/lakeside-cabin/calendar');
    expect(within(screen.getByRole('article')).getAllByText('ACTIVE')).toHaveLength(1);
    expect(screen.getByRole('link', { name: 'Open Booking Calendar' })).toHaveClass('border');
    expect(screen.getByRole('link', { name: 'Edit' })).toHaveClass('border');
    expect(screen.getByRole('article').querySelector('.ss-pill')).toBeNull();
  });

  it('centers the five card sections and keeps availability width stable when toggled', () => {
    render(<HostFixtureProvider><HostListingsPage /></HostFixtureProvider>);
    const card = screen.getAllByRole('article')[0];
    const sections = within(card).getAllByRole('group');
    expect(sections.map((section) => section.getAttribute('aria-label'))).toEqual([
      'Bookings', 'Rating', 'Availability', 'Booking calendar', 'Edit listing',
    ]);
    for (const section of sections) expect(section).toHaveClass('items-center', 'justify-center', 'text-center');
    const availability = within(card).getByRole('group', { name: 'Availability' });
    expect(availability).toHaveClass('w-28');
    expect(availability).toHaveClass('flex-row');
    expect(availability.children[0]).toHaveTextContent('ACTIVE');
    expect(availability.children[1]).toBe(within(availability).getByRole('switch'));
    expect(sections[0].parentElement).toHaveClass('gap-x-2', 'xl:gap-x-3', 'xl:w-fit', 'xl:grid-cols-[5rem_5rem_7rem_10rem_5rem]');
    const widthClass = availability.className;
    fireEvent.click(within(availability).getByRole('switch'));
    expect(within(card).getByRole('group', { name: 'Availability' }).className).toBe(widthClass);
    expect(within(card).getByRole('group', { name: 'Bookings' })).toHaveClass('text-center');
    expect(within(card).getByRole('group', { name: 'Rating' })).toHaveClass('text-center');
  });

  it('exposes listing availability as a labelled switch', () => {
    const listing = createHostFixtureAdapter().getListings().data![0];
    const onToggle = vi.fn();
    render(<HostListingCard listing={listing} onToggle={onToggle} />);

    expect(screen.getByRole('switch', { name: 'Lakeside Cabin availability' })).toHaveAttribute('aria-checked', 'true');
    fireEvent.click(screen.getByRole('switch', { name: 'Lakeside Cabin availability' }));
    expect(onToggle).toHaveBeenCalledOnce();
  });

  it('uses a defined neutral track color for inactive availability', () => {
    const listing = { ...createHostFixtureAdapter().getListings().data![0], status: 'inactive' as const };
    render(<HostListingCard listing={listing} />);
    expect(screen.getByRole('switch', { name: 'Lakeside Cabin availability' })).toHaveClass('bg-[var(--border-muted)]');
  });

  it('changes listing availability locally when the switch is toggled', () => {
    render(<HostFixtureProvider><HostListingsPage /></HostFixtureProvider>);
    const toggle = screen.getByRole('switch', { name: 'Lakeside Cabin availability' });

    expect(toggle).toHaveAttribute('aria-checked', 'true');
    fireEvent.click(toggle);
    expect(screen.getByRole('switch', { name: 'Lakeside Cabin availability' })).toHaveAttribute('aria-checked', 'false');
    expect(within(toggle.closest('article')!).getAllByText('INACTIVE')).toHaveLength(1);
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
