import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { HostFixtureProvider } from './HostFixtureProvider';
import { createHostFixtureAdapter } from './hostFixtures';
import { HostListingCard } from './HostListingCard';
import { HostListingDetail } from './HostListingDetail';
import { HostListingForm } from './HostListingForm';
import HostListingsPage from '@/app/host/listings/page';
import NewListingPage from '@/app/host/listings/new/page';
import HostListingPage from '@/app/host/listings/[listingId]/page';
import EditListingPage from '@/app/host/listings/[listingId]/edit/page';
import HostCalendarPage from '@/app/host/listings/[listingId]/calendar/page';

vi.mock('next/navigation', () => ({ useRouter: () => ({ push: vi.fn() }), useParams: () => ({ listingId: 'lakeside-cabin' }) }));

describe('Host listing components', () => {
  it('aligns the New listing action in the page-title row', () => {
    render(<HostFixtureProvider><HostListingsPage /></HostFixtureProvider>);
    const heading = screen.getByRole('heading', { name: 'My listings' });
    const createLink = screen.getByRole('link', { name: '+ New listing' });
    const titleRow = heading.parentElement!.parentElement;
    expect(createLink.parentElement).toBe(titleRow);
    expect(titleRow).toHaveClass('flex', 'items-center', 'justify-between');
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

  it('makes the listing title a stretched detail link while keeping card controls above it', () => {
    const listing = createHostFixtureAdapter().getListings().data![0];
    render(<HostListingCard listing={listing} />);

    const detailLink = screen.getByRole('link', { name: 'Lakeside Cabin' });
    expect(detailLink).toHaveAttribute('href', '/host/listings/lakeside-cabin');
    expect(detailLink).toHaveClass('after:absolute', 'after:inset-0');
    expect(screen.getByRole('article')).toHaveClass('relative');
    expect(screen.getByRole('group', { name: 'Availability' }).parentElement).toHaveClass('relative', 'z-10');
    expect(screen.getByRole('group', { name: 'Booking calendar' }).parentElement).toHaveClass('relative', 'z-10');
    expect(screen.getByRole('group', { name: 'Edit listing' }).parentElement).toHaveClass('relative', 'z-10');
    expect(screen.getByRole('link', { name: 'Open Booking Calendar' })).toHaveAttribute('href', '/host/listings/lakeside-cabin/calendar');
    expect(screen.getByRole('link', { name: 'Edit' })).toHaveAttribute('href', '/host/listings/lakeside-cabin/edit');
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
    expect(sections[0].parentElement).toHaveClass(
      'grid-cols-2', 'min-[560px]:grid-cols-3', 'lg:grid-cols-5',
      'gap-x-2', 'xl:gap-x-3', 'xl:w-fit', 'xl:grid-cols-[5rem_5rem_7rem_10rem_5rem]',
    );
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
    expect(screen.getByRole('img', { name: 'Lakeside Cabin cover image' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Lakeside Cabin' })).toHaveClass('text-2xl');
    expect(screen.getByText('142 Shoreline Dr, Lakeview, WI 53211')).toHaveClass('text-sm');
    expect(screen.getByText('MAX GUESTS').parentElement).toHaveTextContent('4');
    expect(screen.getByText('BEDROOMS').parentElement).toHaveTextContent('2');
    expect(screen.getByText('BATHROOMS').parentElement).toHaveTextContent('1');
    expect(screen.getByText('PER NIGHT').parentElement).toHaveTextContent('$120');
    expect(screen.getByText('CHECK-IN').parentElement?.querySelector('dd')).toHaveTextContent(/^03:00 PM$/);
    expect(screen.getByText('CHECK-OUT').parentElement?.querySelector('dd')).toHaveTextContent(/^11:00 AM$/);
    expect(screen.getByText('MAX GUESTS').parentElement?.querySelector('dd')).toHaveClass('text-base');
    expect(screen.getByText('MAX GUESTS')).toHaveClass('text-[10px]');
    expect(screen.getByRole('heading', { name: 'Description' })).toHaveClass('text-base');
    expect(screen.getByText(listing.description)).toHaveClass('text-sm');
    expect(screen.getByRole('heading', { name: 'Performance' })).toHaveClass('text-base');
    expect(screen.getByRole('complementary', { name: 'Performance' }).querySelector('dl')).toHaveClass('text-sm');
    expect(screen.getByText('AMENITIES')).toHaveClass('text-xs');
    expect(screen.getByRole('region', { name: /REVIEWS/ }).querySelector('h3')).toHaveClass('text-xs');
    expect(screen.getByText(listing.amenities[0])).toHaveClass('text-xs');
    expect(screen.getByText(/Exactly as pictured, host was very responsive\./).closest('li')).toHaveClass('text-xs');
    expect(screen.getByText('62.00%')).toBeInTheDocument();
    expect(screen.getByText('$1,860.00')).toBeInTheDocument();
    expect(screen.getByText(/Exactly as pictured, host was very responsive\./)).toBeInTheDocument();
    expect(screen.getByText(/Loved the dock\. Would book again\./)).toBeInTheDocument();
  });

  it('aligns listing-detail actions with the breadcrumb and matches their artifact emphasis', () => {
    render(<HostFixtureProvider><HostListingPage /></HostFixtureProvider>);
    const breadcrumb = screen.getByRole('navigation', { name: 'Breadcrumb' });
    const actionRow = breadcrumb.parentElement!;
    const calendarLink = screen.getByRole('link', { name: 'Open Booking Calendar' });
    const editLink = screen.getByRole('link', { name: 'Edit listing' });

    expect(calendarLink.parentElement?.parentElement).toBe(actionRow);
    expect(editLink.parentElement?.parentElement).toBe(actionRow);
    expect(calendarLink).toHaveClass('border');
    expect(editLink).toHaveClass('bg-primary');
  });

  it('keeps detail reviews inline and the performance summary compact', () => {
    const listing = createHostFixtureAdapter().getListing('lakeside-cabin').data!;
    render(<HostListingDetail listing={listing} />);
    const reviews = screen.getByRole('region', { name: /REVIEWS/ });
    const performance = screen.getByRole('complementary', { name: 'Performance' });
    const topDetailsRow = performance.parentElement!;

    expect(within(reviews).getAllByRole('listitem')).toHaveLength(2);
    expect(within(reviews).getByText(/Exactly as pictured, host was very responsive\./)).toBeInTheDocument();
    expect(within(topDetailsRow).getByRole('heading', { name: 'Lakeside Cabin' })).toBeInTheDocument();
    expect(within(topDetailsRow).getByText('MAX GUESTS')).toBeInTheDocument();
    expect(performance).toHaveClass('self-start');
    expect(performance).toHaveClass('ss-card');
    expect(reviews.querySelector('ul')).toHaveClass('space-y-1');
    expect(performance).toHaveClass('lg:col-start-2');
    expect(performance.parentElement).toHaveClass('lg:grid-cols-[minmax(0,3fr)_minmax(280px,2fr)]');
  });

  it('renders form sections and exact required-field validation', async () => {
    render(<HostFixtureProvider adapter={createHostFixtureAdapter('empty')}><HostListingForm /></HostFixtureProvider>);
    expect(screen.getByText('BASIC DETAILS')).toBeInTheDocument();
    expect(screen.getByText('CAPACITY & PRICING')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Save listing' }));
    await waitFor(() => expect(screen.getByText('Title is required')).toBeInTheDocument());
    expect(screen.getByText('Rate / night must be positive with at most 2 decimal places')).toBeInTheDocument();
  });

  it('validates check-in and check-out and reports bedroom and bathroom requirements inline', async () => {
    render(<HostFixtureProvider adapter={createHostFixtureAdapter('empty')}><HostListingForm /></HostFixtureProvider>);
    fireEvent.click(screen.getByRole('button', { name: 'Save listing' }));

    await waitFor(() => expect(screen.getByLabelText('Check-in time')).toHaveAttribute('aria-invalid', 'true'));
    expect(screen.getByLabelText('Check-out time')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByText('Check-in time is required')).toBeInTheDocument();
    expect(screen.getByText('Check-out time is required')).toBeInTheDocument();
    expect(screen.getByText('Bedrooms is required and cannot be negative')).toBeInTheDocument();
    expect(screen.getByText('Bathrooms is required and cannot be negative')).toBeInTheDocument();
  });

  it('requires check-out to be strictly earlier than check-in', async () => {
    render(<HostFixtureProvider><HostListingForm listing={createHostFixtureAdapter().getListing('lakeside-cabin').data!} /></HostFixtureProvider>);
    fireEvent.change(screen.getByLabelText('Check-in time'), { target: { value: '11:00' } });
    fireEvent.change(screen.getByLabelText('Check-out time'), { target: { value: '11:00' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save listing' }));

    expect(await screen.findByText('Check-out must be before check-in')).toBeInTheDocument();
    expect(screen.getByLabelText('Check-out time')).toHaveAttribute('aria-invalid', 'true');
  });

  it('rejects a check-out time later than check-in', async () => {
    render(<HostFixtureProvider><HostListingForm listing={createHostFixtureAdapter().getListing('lakeside-cabin').data!} /></HostFixtureProvider>);
    fireEvent.change(screen.getByLabelText('Check-in time'), { target: { value: '09:00' } });
    fireEvent.change(screen.getByLabelText('Check-out time'), { target: { value: '10:00' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save listing' }));

    expect(await screen.findByText('Check-out must be before check-in')).toBeInTheDocument();
  });

  it('loads saved amenities and presents selected status with normal-weight values', () => {
    const listing = createHostFixtureAdapter().getListing('lakeside-cabin').data!;
    render(<HostFixtureProvider><HostListingForm listing={listing} /></HostFixtureProvider>);

    for (const amenity of ['Wifi', 'Parking', 'Kitchen', 'Washer']) {
      expect(screen.getByRole('checkbox', { name: amenity })).toBeChecked();
    }
    expect(screen.getByRole('combobox', { name: 'Status' })).toHaveTextContent('Active');
    expect(screen.getByRole('combobox', { name: 'Property type' })).toHaveClass('font-normal');
    expect(screen.getByRole('combobox', { name: 'Status' })).toHaveClass('font-normal');
    expect(screen.getByLabelText('Title')).toHaveClass('font-normal');
    expect(screen.getByLabelText('Description')).toHaveClass('font-normal');
  });

  it('formats the listing rate and time values in the artifact edit form', () => {
    const listing = createHostFixtureAdapter().getListing('lakeside-cabin').data!;
    render(<HostFixtureProvider><HostListingForm listing={listing} /></HostFixtureProvider>);

    expect(screen.getByLabelText('Rate / night')).toHaveValue('120.00');
    expect(screen.getByText('$', { selector: 'span[aria-hidden="true"]' })).toBeInTheDocument();
    expect(screen.getByLabelText('Check-in time')).toHaveValue('15:00');
    expect(screen.getByLabelText('Check-out time')).toHaveValue('11:00');
  });

  it('shows entry examples in the new and edit listing text fields', () => {
    const expectedPlaceholders = {
      Title: 'e.g., Lakeside Cabin',
      Description: 'Describe your property and what guests can expect.',
      'Street address': 'e.g., 123 Shoreline Dr',
      City: 'e.g., Lakeview',
      Region: 'e.g., WI',
      'Postal code': 'e.g., 53211',
      'Max guests': 'e.g., 4',
      Bedrooms: 'e.g., 2',
      Bathrooms: 'e.g., 1',
      'Rate / night': 'e.g., 120.00',
    };
    const { rerender } = render(<HostFixtureProvider><NewListingPage /></HostFixtureProvider>);

    for (const [label, placeholder] of Object.entries(expectedPlaceholders)) {
      expect(screen.getByLabelText(label)).toHaveAttribute('placeholder', placeholder);
    }

    rerender(<HostFixtureProvider><EditListingPage /></HostFixtureProvider>);
    for (const [label, placeholder] of Object.entries(expectedPlaceholders)) {
      expect(screen.getByLabelText(label)).toHaveAttribute('placeholder', placeholder);
    }
  });

  it('keeps related cards in independent desktop stacks and preserves narrow-screen order', () => {
    render(<HostFixtureProvider><NewListingPage /></HostFixtureProvider>);
    const form = screen.getByRole('button', { name: 'Save listing' }).closest('form')!;
    const layout = form.firstElementChild!;
    const stacks = Array.from(layout.children);
    const leftHeadings = within(stacks[0] as HTMLElement).getAllByRole('heading', { level: 2 });
    const rightHeadings = within(stacks[1] as HTMLElement).getAllByRole('heading', { level: 2 });

    expect(layout).toHaveClass('grid', 'lg:grid-cols-2', 'w-full', 'gap-y-2');
    expect(stacks[0]).toHaveClass('contents', 'lg:flex', 'lg:flex-col', 'lg:gap-2');
    expect(stacks[1]).toHaveClass('contents', 'lg:flex', 'lg:flex-col', 'lg:gap-2');
    expect(leftHeadings.map((heading) => heading.textContent)).toEqual(['BASIC DETAILS', 'LOCATION']);
    expect(rightHeadings.map((heading) => heading.textContent)).toEqual(['CAPACITY & PRICING', 'AMENITIES']);
    expect(within(form).getByRole('heading', { name: 'BASIC DETAILS' }).closest('section')).toHaveClass('order-1');
    expect(within(form).getByRole('heading', { name: 'CAPACITY & PRICING' }).closest('section')).toHaveClass('order-2');
    expect(within(form).getByRole('heading', { name: 'LOCATION' }).closest('section')).toHaveClass('order-3');
    expect(within(form).getByRole('heading', { name: 'AMENITIES' }).closest('section')).toHaveClass('order-4');
    expect(screen.getByRole('heading', { name: 'BASIC DETAILS' }).closest('section')).toHaveClass('w-full');
  });

  it('groups related fields into the artifact two- and three-column rows', () => {
    render(<HostFixtureProvider><HostListingForm /></HostFixtureProvider>);

    expect(screen.getByRole('combobox', { name: 'Property type' }).closest('div.grid')).toHaveClass('sm:grid-cols-2');
    expect(screen.getByLabelText('City').closest('div.grid')).toHaveClass('sm:grid-cols-3');
    expect(screen.getByLabelText('Max guests').closest('div.grid')).toHaveClass('sm:grid-cols-2');
    expect(screen.getByLabelText('Bathrooms').closest('div.grid')).toHaveClass('sm:grid-cols-2');
  });

  it('shows both select labels and lets each form card keep its own height', () => {
    render(<HostFixtureProvider><NewListingPage /></HostFixtureProvider>);
    const form = screen.getByRole('button', { name: 'Save listing' }).closest('form')!;

    expect(within(form).getByText('Property type')).toBeInTheDocument();
    expect(within(form).getByText('Status')).toBeInTheDocument();
    expect(form.firstElementChild).toHaveClass('items-start', 'gap-y-2');
  });

  it('renders artifact amenity rows and orders the primary action before cancel', () => {
    render(<HostFixtureProvider><HostListingForm /></HostFixtureProvider>);

    for (const amenity of ['Wifi', 'Parking', 'Air Conditioning', 'Kitchen', 'Washer', 'Work Desk']) {
      expect(screen.getByRole('checkbox', { name: amenity })).toBeInTheDocument();
      expect(screen.getByRole('checkbox', { name: amenity }).closest('label')).toHaveClass('border', 'rounded-lg', 'px-3', 'py-2');
    }
    const form = screen.getByRole('button', { name: 'Save listing' }).closest('form')!;
    const actions = form.lastElementChild!;
    expect(actions).toHaveClass('justify-between');
    expect(within(actions).getAllByRole('button').map((button) => button.textContent)).toEqual(['Save listing', 'Cancel']);
    expect(within(actions).getByRole('button', { name: 'Save listing' })).toHaveClass('bg-primary');
    expect(within(actions).getByRole('button', { name: 'Cancel' })).toHaveClass('border');
  });

  it('shows the full listing breadcrumb on New and Edit pages', () => {
    const { rerender } = render(<HostFixtureProvider><NewListingPage /></HostFixtureProvider>);
    expect(screen.getByRole('navigation', { name: 'Breadcrumb' })).toHaveTextContent(/Listings\s*›\s*New/);

    rerender(<HostFixtureProvider><EditListingPage /></HostFixtureProvider>);
    expect(screen.getByRole('navigation', { name: 'Breadcrumb' })).toHaveTextContent(/Listings\s*›\s*Lakeside Cabin\s*›\s*Edit/);
    expect(screen.getByRole('navigation', { name: 'Breadcrumb' })).toHaveClass('gap-2');
  });

  it('uses the same chevron breadcrumb path and styling on listing detail and calendar pages', () => {
    const { rerender } = render(<HostFixtureProvider><HostListingPage /></HostFixtureProvider>);
    let breadcrumb = screen.getByRole('navigation', { name: 'Breadcrumb' });
    expect(breadcrumb).toHaveTextContent(/Listings\s*›\s*Lakeside Cabin/);
    expect(breadcrumb).toHaveClass('gap-2', 'text-xs', 'text-[var(--text-subtle)]');
    expect(breadcrumb.parentElement).toHaveClass('mb-4', 'items-start', 'justify-between');
    expect(breadcrumb.querySelectorAll('[aria-hidden="true"]')).toHaveLength(1);

    rerender(<HostFixtureProvider><HostCalendarPage /></HostFixtureProvider>);
    breadcrumb = screen.getByRole('navigation', { name: 'Breadcrumb' });
    expect(breadcrumb).toHaveTextContent(/Listings\s*›\s*Lakeside Cabin\s*›\s*Booking Calendar/);
    expect(breadcrumb).toHaveClass('mb-4', 'gap-2', 'text-xs', 'text-[var(--text-subtle)]');
    expect(breadcrumb.querySelectorAll('[aria-hidden="true"]')).toHaveLength(2);
  });
});
