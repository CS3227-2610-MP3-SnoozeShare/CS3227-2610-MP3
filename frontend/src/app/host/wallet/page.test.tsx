import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import HostWalletPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';
import { HostApiProvider } from '@/components/host/HostApiProvider';

const { fetchHostWallet, topUpHostWallet, withdrawFromHostWallet } = vi.hoisted(() => ({ fetchHostWallet: vi.fn(), topUpHostWallet: vi.fn(), withdrawFromHostWallet: vi.fn() }));
vi.mock('@/lib/hostApi', async (importOriginal) => ({ ...await importOriginal<typeof import('@/lib/hostApi')>(), fetchHostWallet, topUpHostWallet, withdrawFromHostWallet }));

const walletDto = (balance: number) => ({ wallet: { id: 'wallet-1', userId: 'host-1', balance, currency: 'SGD', updatedAt: '2026-10-22T00:00:00Z' }, statement: Array.from({ length: 101 }, (_, index) => ({ id: `tx-${index}`, actionType: 'TOP_UP', walletAdjustment: 10, balanceAfter: balance, reason: 'Top-up', bookingId: null, ticketId: null, timestamp: new Date(Date.UTC(2026, 9, index + 1)).toISOString() })) });

describe('Host wallet page', () => {
  beforeEach(() => { vi.clearAllMocks(); });
  it('renders balance and statement without repeating the Wallet page heading', () => {
    render(<HostFixtureProvider><HostWalletPage /></HostFixtureProvider>);
    expect(screen.getByText('AVAILABLE BALANCE')).toBeInTheDocument();
    expect(screen.getByText('Transaction statement')).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Wallet', level: 1 })).not.toBeInTheDocument();
  });

  it('uses the cached complete wallet response on the Wallet route', async () => {
    fetchHostWallet.mockResolvedValue(walletDto(75));
    render(<HostApiProvider><HostFixtureProvider><HostWalletPage /></HostFixtureProvider></HostApiProvider>);
    expect((await screen.findAllByText('$75.00')).length).toBeGreaterThan(0);
    expect(screen.getAllByText('+$10.00')).toHaveLength(101);
    expect(screen.getByRole('table').querySelectorAll('tbody tr')).toHaveLength(101);
    expect(screen.queryByRole('button', { name: /Load older transactions/i })).not.toBeInTheDocument();
    expect(fetchHostWallet).toHaveBeenCalledTimes(1);
    expect(fetchHostWallet).toHaveBeenCalledWith();
  });

  it('submits one explicit mocked top-up and updates the server wallet summary', async () => {
    fetchHostWallet.mockResolvedValue(walletDto(100));
    topUpHostWallet.mockResolvedValue({ mocked: true, wallet: walletDto(125).wallet, transaction: walletDto(125).statement[0] });
    render(<HostApiProvider><HostFixtureProvider><HostWalletPage /></HostFixtureProvider></HostApiProvider>);
    await screen.findAllByText('$100.00');
    fireEvent.click(screen.getByRole('button', { name: 'Top up' }));
    const dialog = screen.getByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText('Amount'), { target: { value: '25' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Confirm top up' }));

    await waitFor(() => expect(topUpHostWallet).toHaveBeenCalledWith(25));
    expect((await screen.findAllByText('$125.00')).length).toBeGreaterThan(0);
    expect(withdrawFromHostWallet).not.toHaveBeenCalled();
  });
});
