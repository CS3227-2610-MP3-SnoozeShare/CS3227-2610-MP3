import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import HostListingPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';

const { useParams } = vi.hoisted(() => ({ useParams: vi.fn(() => ({ listingId: 'lakeside-cabin' })) }));
vi.mock('next/navigation', () => ({ useParams }));

describe('Host listing detail page', () => {
  it('renders Lakeside Cabin details and its standard breadcrumb', () => {
    render(<HostFixtureProvider><HostListingPage /></HostFixtureProvider>);
    expect(screen.getByRole('heading', { name: 'Lakeside Cabin' })).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Breadcrumb' })).toHaveTextContent(/Listings\s*›\s*Lakeside Cabin/);
    expect(screen.getByText('A cozy cabin steps from the water, with a private dock and wood-burning stove. Perfect for a quiet long weekend away from the city.')).toBeInTheDocument();
  });
  it('renders only the not-found card for unknown IDs', () => { useParams.mockReturnValue({ listingId: 'malformed%2Funknown' }); render(<HostFixtureProvider><HostListingPage /></HostFixtureProvider>); expect(screen.getAllByText('Listing not found').length).toBe(2); expect(screen.queryByText('AMENITIES')).not.toBeInTheDocument(); });
});
