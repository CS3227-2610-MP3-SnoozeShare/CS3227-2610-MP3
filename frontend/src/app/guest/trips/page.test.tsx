import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('next/navigation', () => ({ redirect: vi.fn(), useRouter: vi.fn(() => ({ push: vi.fn() })) }));

const mockListTrips = vi.fn();
vi.mock('@/lib/guest-api', () => ({
  listTrips: (...args: unknown[]) => mockListTrips(...args),
  cancelPreview: vi.fn(),
  cancelBooking: vi.fn(),
  submitReview: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  BackendApiError: class extends Error { code: string; constructor(code: string, msg: string) { super(msg); this.code = code; } },
}));

import TripsPage from './page';

const trip = (id: string, status: string, startDate: string, endDate: string, extra?: Partial<{ canCancel: boolean; hasReview: boolean }>) => ({
  booking: {
    id, listingId: 'l1', guestId: 'g1', startDate, endDate, status,
    nightlyRateSnapshot: 100, totalAmount: 200, createdAt: '', decidedAt: null, completedAt: null, hostDecisionMessage: null,
  },
  listingTitle: `Listing ${id}`,
  hostDisplayName: 'Host',
  canCancel: extra?.canCancel ?? false,
  canFileDispute: false,
  hasReview: extra?.hasReview ?? false,
});

describe('TripsPage', () => {
  beforeEach(() => { vi.clearAllMocks(); });

  it('shows empty state when no trips', async () => {
    mockListTrips.mockResolvedValue([]);
    render(<TripsPage />);
    await waitFor(() => {
      expect(screen.getByTestId('empty-state')).toBeInTheDocument();
    });
  });

  it('groups trips into sections', async () => {
    const future = '2099-01-01';
    const farFuture = '2099-01-10';
    const pastDate = '2020-01-01';
    const pastEnd = '2020-01-05';
    mockListTrips.mockResolvedValue([
      trip('1', 'CONFIRMED', future, farFuture),
      trip('2', 'COMPLETED', pastDate, pastEnd, { hasReview: true }),
    ]);
    render(<TripsPage />);
    await waitFor(() => {
      expect(screen.getByTestId('trip-sections')).toBeInTheDocument();
    });
    expect(screen.getByTestId('section-upcoming')).toBeInTheDocument();
    expect(screen.getByText('Listing 1')).toBeInTheDocument();
    // Past section is collapsed by default — assert it exists but content is hidden
    expect(screen.getByTestId('section-past')).toBeInTheDocument();
  });

  it('shows cancel button for cancellable trips', async () => {
    mockListTrips.mockResolvedValue([
      trip('1', 'PENDING', '2099-06-01', '2099-06-05', { canCancel: true }),
    ]);
    render(<TripsPage />);
    await waitFor(() => {
      expect(screen.getByText('Cancel')).toBeInTheDocument();
    });
  });
});
