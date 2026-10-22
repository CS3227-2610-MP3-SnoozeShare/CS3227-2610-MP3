import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

// Mock next/navigation
vi.mock('next/navigation', () => ({ redirect: vi.fn(), useRouter: vi.fn(() => ({ push: vi.fn() })) }));

// Mock guest-api
const mockSearchListings = vi.fn();
vi.mock('@/lib/guest-api', () => ({
  searchListings: (...args: unknown[]) => mockSearchListings(...args),
  getListingDetail: vi.fn(),
  estimateCost: vi.fn(),
  createBooking: vi.fn(),
}));

// Mock BackendApiError
vi.mock('@/lib/api', () => ({
  BackendApiError: class extends Error { code: string; constructor(code: string, msg: string) { super(msg); this.code = code; } },
}));

import SearchPage from './page';

describe('SearchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the filter bar', async () => {
    mockSearchListings.mockResolvedValue([]);
    render(<SearchPage />);
    expect(screen.getByTestId('search-filters')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Search' })).toBeInTheDocument();
  });

  it('displays search results', async () => {
    mockSearchListings.mockResolvedValue([
      {
        property: {
          id: '1', title: 'Beach House', description: 'Nice', propertyType: 'APARTMENT',
          city: 'Singapore', district: 'East', maxGuests: 4, baseNightlyRate: 150,
          hostId: 'h1', status: 'ACTIVE', address: '123', postalCode: 123456,
          bedrooms: 2, bathrooms: 1, checkInTime: '14:00', checkOutTime: '11:00',
          amenities: [], createdAt: '',
        },
      },
    ]);
    render(<SearchPage />);
    await waitFor(() => {
      expect(screen.getByTestId('results-grid')).toBeInTheDocument();
    });
    expect(screen.getByText('Beach House')).toBeInTheDocument();
  });

  it('handles empty state', async () => {
    mockSearchListings.mockResolvedValue([]);
    render(<SearchPage />);
    await waitFor(() => {
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    });
    expect(screen.getByText(/No listings found/)).toBeInTheDocument();
  });
});
