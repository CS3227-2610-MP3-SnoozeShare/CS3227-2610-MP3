import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

// Mock next/navigation
const mockGet = vi.fn(() => null);
vi.mock('next/navigation', () => ({
  redirect: vi.fn(),
  useRouter: vi.fn(() => ({ push: vi.fn() })),
  useSearchParams: vi.fn(() => ({ get: mockGet })),
}));

// Mock AuthProvider
vi.mock('@/providers/AuthProvider', () => ({
  useAuth: vi.fn(() => ({ user: { uid: 'u1' }, profile: { role: 'GUEST' } })),
}));

// Mock guest-api
const mockGetConversations = vi.fn();
vi.mock('@/lib/guest-api', () => ({
  getConversations: (...args: unknown[]) => mockGetConversations(...args),
  getTicketThread: vi.fn().mockResolvedValue([]),
  getBookingThread: vi.fn().mockResolvedValue([]),
  postToTicket: vi.fn(),
  postToBooking: vi.fn(),
  markTicketRead: vi.fn().mockResolvedValue(undefined),
  markBookingRead: vi.fn().mockResolvedValue(undefined),
  listCategories: vi.fn().mockResolvedValue([]),
  fileTicket: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  BackendApiError: class extends Error { code: string; constructor(code: string, msg: string) { super(msg); this.code = code; } },
}));

import MessagesPage from './page';

describe('MessagesPage', () => {
  beforeEach(() => { vi.clearAllMocks(); });

  it('renders conversation list', async () => {
    mockGetConversations.mockResolvedValue({
      ticketConversations: [{
        ticketId: 't1', channel: 'TICKET', ticketLabel: 'T-001', ticketTitle: 'Broken AC',
        listingTitle: 'Beach House', counterpartName: 'Admin', lastMessage: null, unreadCount: 0, open: true,
      }],
      bookingConversations: [],
    });
    render(<MessagesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('conversation-list')).toBeInTheDocument();
    });
    expect(screen.getByText('Broken AC')).toBeInTheDocument();
  });

  it('shows empty state when no conversations', async () => {
    mockGetConversations.mockResolvedValue({ ticketConversations: [], bookingConversations: [] });
    render(<MessagesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('no-conversations')).toBeInTheDocument();
    });
  });

  it('shows select prompt when no conversation selected', async () => {
    mockGetConversations.mockResolvedValue({ ticketConversations: [], bookingConversations: [] });
    render(<MessagesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('select-prompt')).toBeInTheDocument();
    });
  });
});
