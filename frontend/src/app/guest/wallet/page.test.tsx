import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('next/navigation', () => ({ redirect: vi.fn(), useRouter: vi.fn(() => ({ push: vi.fn() })) }));

const mockGetWallet = vi.fn();
const mockGetTransactions = vi.fn();
vi.mock('@/lib/guest-api', () => ({
  getWallet: (...args: unknown[]) => mockGetWallet(...args),
  getTransactions: (...args: unknown[]) => mockGetTransactions(...args),
  topUp: vi.fn(),
  withdraw: vi.fn(),
}));

vi.mock('@/lib/api', () => ({
  BackendApiError: class extends Error { code: string; constructor(code: string, msg: string) { super(msg); this.code = code; } },
}));

import WalletPage from './page';

describe('WalletPage', () => {
  beforeEach(() => { vi.clearAllMocks(); });

  it('renders balance card', async () => {
    mockGetWallet.mockResolvedValue({ id: 'w1', userId: 'u1', balance: 250.50, currency: 'SGD', updatedAt: '' });
    mockGetTransactions.mockResolvedValue([]);
    render(<WalletPage />);
    await waitFor(() => {
      expect(screen.getByTestId('balance-card')).toBeInTheDocument();
    });
    expect(screen.getByText('$250.50')).toBeInTheDocument();
  });

  it('uses the Host wallet page heading and statement hierarchy', async () => {
    mockGetWallet.mockResolvedValue({ id: 'w1', userId: 'u1', balance: 250.50, currency: 'SGD', updatedAt: '' });
    mockGetTransactions.mockResolvedValue([]);
    render(<WalletPage />);
    await waitFor(() => expect(screen.getByTestId('balance-card')).toBeInTheDocument());
    expect(screen.queryByRole('heading', { name: 'Wallet', level: 1 })).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Transaction statement', level: 2 })).toBeInTheDocument();
  });

  it('renders transaction list', async () => {
    mockGetWallet.mockResolvedValue({ id: 'w1', userId: 'u1', balance: 100, currency: 'SGD', updatedAt: '' });
    mockGetTransactions.mockResolvedValue([{
      id: 'tx1', actionType: 'TOP_UP', subjectUserId: 'u1',
      walletAdjustment: 50, balanceAfter: 100, reason: null,
      bookingId: null, ticketId: null, timestamp: '2026-01-01T00:00:00Z',
    }]);
    render(<WalletPage />);
    await waitFor(() => {
      expect(screen.getByTestId('transaction-list')).toBeInTheDocument();
    });
    expect(screen.getByText('Top-up')).toBeInTheDocument();
    expect(screen.getByText('+$50.00')).toBeInTheDocument();
  });

  it('colors canonical escrow holds with the warning type color', async () => {
    mockGetWallet.mockResolvedValue({ id: 'w1', userId: 'u1', balance: 100, currency: 'SGD', updatedAt: '' });
    mockGetTransactions.mockResolvedValue([{
      id: 'tx-escrow', actionType: 'ESCROW_HOLD', subjectUserId: 'u1',
      walletAdjustment: -50, balanceAfter: 50, reason: null,
      bookingId: 'booking-1', ticketId: null, timestamp: '2026-01-01T00:00:00Z',
    }]);
    render(<WalletPage />);
    await waitFor(() => expect(screen.getByText('ESCROW_HOLD')).toBeInTheDocument());
    expect(screen.getByText('ESCROW_HOLD')).toHaveClass('bg-[var(--warning-subtle)]', 'text-[var(--warning)]');
  });

  it('shows empty transactions state', async () => {
    mockGetWallet.mockResolvedValue({ id: 'w1', userId: 'u1', balance: 0, currency: 'SGD', updatedAt: '' });
    mockGetTransactions.mockResolvedValue([]);
    render(<WalletPage />);
    await waitFor(() => {
      expect(screen.getByTestId('no-transactions')).toBeInTheDocument();
    });
  });
});
