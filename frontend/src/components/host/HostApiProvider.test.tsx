import { act, render, screen, waitFor } from '@testing-library/react';
import { StrictMode, useEffect } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { HostApiProvider, useHostApi } from './HostApiProvider';
import { BackendApiError } from '@/lib/api';

const { fetchHostWallet, topUpHostWallet, withdrawFromHostWallet } = vi.hoisted(() => ({
  fetchHostWallet: vi.fn(), topUpHostWallet: vi.fn(), withdrawFromHostWallet: vi.fn(),
}));
vi.mock('@/lib/hostApi', () => ({ fetchHostWallet, topUpHostWallet, withdrawFromHostWallet }));

function WalletProbe() {
  const api = useHostApi();
  return <div>
    <span>{api.wallet.status === 'ready' ? api.wallet.data.wallet.balance : api.wallet.status}</span>
    <span>{api.wallet.status === 'ready' ? api.wallet.data.statement.length : 0}</span>
    <button onClick={() => void api.mutateWallet('top-up', 25).catch(() => undefined)}>Top up</button>
    <button onClick={() => void api.refreshWallet()}>Reload wallet</button>
    <button onClick={() => void api.retryWalletRecovery().catch(() => undefined)}>Refresh wallet</button>
    <span>{api.walletMutationPending ? 'pending' : 'idle'}</span>
    <span>{api.walletRecoveryRequired ? 'recovery-required' : 'wallet-ready'}</span>
  </div>;
}

function ResourceProbe() {
  const api = useHostApi();
  const resource = api.getResource<string[]>('items');
  return <div>
    <span>{resource?.status ?? 'not-loaded'}</span>
    <span>{resource?.status === 'error' || resource?.status === 'not-found' ? resource.message : ''}</span>
    <button onClick={() => void api.loadResource('items', async () => []).catch(() => undefined)}>Load empty</button>
    <button onClick={() => void api.loadResource('items', async () => { throw new BackendApiError('NOT_FOUND', 'private detail', 'r', 404); }).catch(() => undefined)}>Load missing</button>
  </div>;
}

function ResourceEffectProbe({ loader }: { loader: () => Promise<string[]> }) {
  const api = useHostApi();
  const loadResource = api.loadResource;
  useEffect(() => {
    void loadResource('strict-mode-items', loader).catch(() => undefined);
  }, [loadResource, loader]);
  return null;
}

const walletPage = (balance: number) => ({
  wallet: { id: 'w-1', userId: 'host-1', balance, currency: 'SGD', updatedAt: '2026-10-22T00:00:00Z' },
  statement: Array.from({ length: 101 }, (_, index) => ({
    id: `tx-${index}`, actionType: 'TOP_UP', walletAdjustment: 1, balanceAfter: balance,
    reason: 'seed', bookingId: null, ticketId: null, timestamp: '2026-10-22T00:00:00Z',
  })),
});

describe('HostApiProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    fetchHostWallet.mockResolvedValue(walletPage(100));
    topUpHostWallet.mockResolvedValue({ mocked: true, wallet: walletPage(125).wallet, transaction: {} });
  });

  it('loads and shares the complete wallet response with one request', async () => {
    render(<HostApiProvider><WalletProbe /><WalletProbe /></HostApiProvider>);

    expect(screen.getAllByText('loading')).toHaveLength(2);
    await waitFor(() => expect(screen.getAllByText('100').length).toBe(2));
    expect(screen.getAllByText('101')).toHaveLength(2);
    await waitFor(() => expect(fetchHostWallet).toHaveBeenCalledTimes(1));
    expect(fetchHostWallet).toHaveBeenCalledWith();
  });

  it('shares an in-flight complete wallet request across Strict Mode effect replay', async () => {
    let resolveWallet!: (value: ReturnType<typeof walletPage>) => void;
    fetchHostWallet.mockImplementation(() => new Promise((resolve) => { resolveWallet = resolve; }));
    render(<StrictMode><HostApiProvider><WalletProbe /></HostApiProvider></StrictMode>);

    await waitFor(() => expect(fetchHostWallet).toHaveBeenCalledTimes(1));
    await act(async () => { resolveWallet(walletPage(100)); });
    await screen.findByText('100');
  });

  it('shares an in-flight page resource request across Strict Mode effect replay', async () => {
    let resolveResource!: (value: string[]) => void;
    const loader = vi.fn(() => new Promise<string[]>((resolve) => { resolveResource = resolve; }));
    render(<StrictMode><HostApiProvider><ResourceEffectProbe loader={loader} /></HostApiProvider></StrictMode>);

    await waitFor(() => expect(loader).toHaveBeenCalledTimes(1));
    await act(async () => { resolveResource(['listing']); });
  });

  it('does not retry a failed wallet mutation and refreshes the wallet before unlocking', async () => {
    let rejectMutation!: (error: Error) => void;
    topUpHostWallet.mockImplementationOnce(() => new Promise((_, reject) => { rejectMutation = reject; }));
    fetchHostWallet.mockResolvedValueOnce(walletPage(100)).mockResolvedValue(walletPage(100));
    render(<HostApiProvider><WalletProbe /></HostApiProvider>);
    await screen.findByText('100');

    act(() => { screen.getByRole('button', { name: 'Top up' }).click(); });
    expect(screen.getByText('pending')).toBeInTheDocument();
    await act(async () => { rejectMutation(new Error('ambiguous network failure')); });

    await waitFor(() => expect(screen.getByText('idle')).toBeInTheDocument());
    expect(screen.getByText('100')).toBeInTheDocument();
    expect(topUpHostWallet).toHaveBeenCalledTimes(1);
    expect(fetchHostWallet).toHaveBeenCalledTimes(2);
    expect(fetchHostWallet).toHaveBeenNthCalledWith(2);
    expect(withdrawFromHostWallet).not.toHaveBeenCalled();
  });

  it('keeps wallet recovery locked when refresh fails, then unlocks only after an explicit successful refresh', async () => {
    topUpHostWallet.mockRejectedValueOnce(new Error('ambiguous network failure'));
    fetchHostWallet.mockResolvedValueOnce(walletPage(100)).mockRejectedValueOnce(new Error('refresh failed'));
    render(<HostApiProvider><WalletProbe /></HostApiProvider>);
    await screen.findByText('100');

    act(() => { screen.getByRole('button', { name: 'Top up' }).click(); });
    await screen.findByText('recovery-required');
    expect(topUpHostWallet).toHaveBeenCalledTimes(1);

    fetchHostWallet.mockResolvedValue(walletPage(100));
    act(() => { screen.getByRole('button', { name: 'Refresh wallet' }).click(); });
    await screen.findByText('wallet-ready');
    expect(topUpHostWallet).toHaveBeenCalledTimes(1);
  });

  it('refreshes the complete wallet response without page parameters', async () => {
    const statement = vi.fn().mockResolvedValue(walletPage(100));
    fetchHostWallet.mockImplementation(statement);
    render(<HostApiProvider><WalletProbe /></HostApiProvider>);
    await screen.findByText('100');
    screen.getByRole('button', { name: 'Reload wallet' }).click();

    await waitFor(() => expect(fetchHostWallet).toHaveBeenLastCalledWith());
    expect(screen.getByText('100')).toBeInTheDocument();
  });

  it('tracks generic API resources through empty and safe not-found states', async () => {
    render(<HostApiProvider><ResourceProbe /></HostApiProvider>);
    screen.getByRole('button', { name: 'Load empty' }).click();
    await screen.findByText('empty');
    screen.getByRole('button', { name: 'Load missing' }).click();
    await screen.findByText('not-found');
    expect(screen.getByText('This item is no longer available.')).toBeInTheDocument();
    expect(screen.queryByText('private detail')).not.toBeInTheDocument();
  });
});
