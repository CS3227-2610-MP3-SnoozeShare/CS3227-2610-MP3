import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import HostCalendarPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';

vi.mock('next/navigation', () => ({ useParams: () => ({ listingId: 'lakeside-cabin' }) }));

describe('Host calendar page', () => {
  it('renders the calendar without a duplicate page title', () => {
    render(<HostFixtureProvider><HostCalendarPage /></HostFixtureProvider>);
    expect(screen.getByRole('heading', { name: 'September 2026' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Booking Calendar' })).not.toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Breadcrumb' })).toHaveTextContent('Listings');
    expect(screen.getByRole('heading', { name: 'Block dates' })).toBeInTheDocument();
  });
});
