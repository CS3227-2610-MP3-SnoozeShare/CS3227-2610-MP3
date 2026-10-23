import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

// Mock next/navigation
vi.mock('next/navigation', () => ({ redirect: vi.fn(), useRouter: vi.fn(() => ({ push: vi.fn() })) }));

// Mock guest-api
const mockSearchListings = vi.fn();
const mockAiSearch = vi.fn();
vi.mock('@/lib/guest-api', () => ({
  searchListings: (...args: unknown[]) => mockSearchListings(...args),
  aiSearch: (...args: unknown[]) => mockAiSearch(...args),
  getListingDetail: vi.fn(),
  estimateCost: vi.fn(),
  createBooking: vi.fn(),
}));

// Mock BackendApiError
vi.mock('@/lib/api', () => ({
  BackendApiError: class extends Error { code: string; constructor(code: string, msg: string) { super(msg); this.code = code; } },
}));

import SearchPage from './page';

const SAMPLE_PROPERTY = {
  id: '1', title: 'Beach House', description: 'Nice', propertyType: 'APARTMENT',
  city: 'Singapore', district: 'East', maxGuests: 4, baseNightlyRate: 150,
  hostId: 'h1', status: 'ACTIVE', address: '123', postalCode: 123456,
  bedrooms: 2, bathrooms: 1, checkInTime: '14:00', checkOutTime: '11:00',
  amenities: [], createdAt: '',
};

describe('SearchPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockSearchListings.mockResolvedValue([]);
  });

  it('renders the filter bar', async () => {
    render(<SearchPage />);
    expect(screen.getByTestId('search-filters')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Search' })).toBeInTheDocument();
  });

  it('displays search results', async () => {
    mockSearchListings.mockResolvedValue([{ property: SAMPLE_PROPERTY }]);
    render(<SearchPage />);
    await waitFor(() => {
      expect(screen.getByTestId('results-grid')).toBeInTheDocument();
    });
    expect(screen.getByText('Beach House')).toBeInTheDocument();
  });

  it('handles empty state', async () => {
    render(<SearchPage />);
    await waitFor(() => {
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    });
    expect(screen.getByText(/No listings found/)).toBeInTheDocument();
  });

  // ---- AI search tests (T5) ----

  /** F5: renders AI search bar */
  it('renders AI search bar', async () => {
    render(<SearchPage />);
    expect(screen.getByTestId('ai-search-input')).toBeInTheDocument();
    expect(screen.getByTestId('ai-search-button')).toBeInTheDocument();
  });

  /** E31: AI button disabled when input empty */
  it('AI button disabled when input empty', async () => {
    render(<SearchPage />);
    const button = screen.getByTestId('ai-search-button');
    expect(button).toBeDisabled();
  });

  /** E35: button disabled during loading */
  it('button disabled during loading', async () => {
    mockAiSearch.mockImplementation(() => new Promise(() => {})); // never resolves
    render(<SearchPage />);
    const input = screen.getByTestId('ai-search-input');
    const button = screen.getByTestId('ai-search-button');

    fireEvent.change(input, { target: { value: 'beach in Singapore' } });
    fireEvent.click(button);

    await waitFor(() => {
      expect(button).toBeDisabled();
    });
  });

  /** F6, E32: shows AI explanation after search */
  it('shows AI explanation after search', async () => {
    mockAiSearch.mockResolvedValue({
      results: [{ property: SAMPLE_PROPERTY }],
      explanation: 'Looking for a beach in Singapore.',
      filters: { city: 'Singapore', startDate: null, endDate: null, guests: null, maxNightlyRate: null },
      aiAvailable: true,
    });
    render(<SearchPage />);
    await waitFor(() => expect(screen.queryByText('Loading listings...')).not.toBeInTheDocument());

    const input = screen.getByTestId('ai-search-input');
    const button = screen.getByTestId('ai-search-button');

    fireEvent.change(input, { target: { value: 'beach in Singapore' } });
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByTestId('ai-explanation')).toBeInTheDocument();
    });
    expect(screen.getByText('Looking for a beach in Singapore.')).toBeInTheDocument();
    expect(screen.getByTestId('ai-filters')).toBeInTheDocument();
    expect(screen.getByText(/City: Singapore/)).toBeInTheDocument();
  });

  /** E34: degraded response shows alert */
  it('degraded response shows alert', async () => {
    mockAiSearch.mockResolvedValue({
      results: [],
      explanation: 'AI unavailable.',
      filters: null,
      aiAvailable: false,
      errorCode: 'USER_THROTTLED',
    });
    render(<SearchPage />);
    await waitFor(() => expect(screen.queryByText('Loading listings...')).not.toBeInTheDocument());

    const input = screen.getByTestId('ai-search-input');
    fireEvent.change(input, { target: { value: 'something' } });
    fireEvent.click(screen.getByTestId('ai-search-button'));

    await waitFor(() => {
      expect(screen.getByTestId('ai-alert')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('ai-explanation')).not.toBeInTheDocument();
  });

  /** E33: clear AI search returns to manual mode */
  it('clear AI search returns to manual mode', async () => {
    mockAiSearch.mockResolvedValue({
      results: [{ property: SAMPLE_PROPERTY }],
      explanation: 'AI result.',
      filters: { city: 'Singapore', startDate: null, endDate: null, guests: null, maxNightlyRate: null },
      aiAvailable: true,
    });
    render(<SearchPage />);
    await waitFor(() => expect(screen.queryByText('Loading listings...')).not.toBeInTheDocument());

    const input = screen.getByTestId('ai-search-input');
    fireEvent.change(input, { target: { value: 'test' } });
    fireEvent.click(screen.getByTestId('ai-search-button'));

    await waitFor(() => {
      expect(screen.getByTestId('ai-explanation')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-clear'));

    expect(screen.queryByTestId('ai-explanation')).not.toBeInTheDocument();
  });

  /** F7: manual search still works after AI search */
  it('manual search still works after AI search', async () => {
    mockAiSearch.mockResolvedValue({
      results: [{ property: { ...SAMPLE_PROPERTY, title: 'AI Result' } }],
      explanation: 'AI result.',
      filters: null,
      aiAvailable: true,
    });
    mockSearchListings.mockResolvedValue([{ property: { ...SAMPLE_PROPERTY, title: 'Manual Result' } }]);

    render(<SearchPage />);
    await waitFor(() => expect(screen.queryByText('Loading listings...')).not.toBeInTheDocument());

    // Do AI search
    fireEvent.change(screen.getByTestId('ai-search-input'), { target: { value: 'test' } });
    fireEvent.click(screen.getByTestId('ai-search-button'));
    await waitFor(() => {
      expect(screen.getByTestId('ai-explanation')).toBeInTheDocument();
    });

    // Do manual search
    fireEvent.click(screen.getByRole('button', { name: 'Search' }));
    await waitFor(() => {
      expect(screen.queryByTestId('ai-explanation')).not.toBeInTheDocument();
    });
    expect(screen.getByText('Manual Result')).toBeInTheDocument();
  });
});
