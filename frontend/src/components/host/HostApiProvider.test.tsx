import { act, render, screen, waitFor } from '@testing-library/react';
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
    <button onClick={() => void api.mutateWallet('top-up', 25).catch(() => undefined)}>Top up</button>
    <button onClick={() => void api.refreshWalletStatement()}>Load statement</button>
    <span>{api.walletMutationPending ? 'pending' : 'idle'}</span>
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

const walletPage = (balance: number) => ({
  wallet: { id: 'w-1', userId: 'host-1', balance, currency: 'SGD', updatedAt: '2026-10-22T00:00:00Z' },
  statement: { items: [], page: 0, size: 1, hasMore: false },
});

describe('HostApiProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    fetchHostWallet.mockResolvedValue(walletPage(100));
    topUpHostWallet.mockResolvedValue({ mocked: true, wallet: walletPage(125).wallet, transaction: {} });
  });

  it('loads and shares one cached wallet summary without loading the statement page', async () => {
    render(<HostApiProvider><WalletProbe /><WalletProbe /></HostApiProvider>);

    expect(screen.getAllByText('loading')).toHaveLength(2);
    await waitFor(() => expect(screen.getAllByText('100').length).toBe(2));
    expect(fetchHostWallet).toHaveBeenCalledTimes(1);
    expect(fetchHostWallet).toHaveBeenCalledWith(0, 1);
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
    expect(fetchHostWallet).toHaveBeenCalledTimes(3);
    expect(fetchHostWallet).toHaveBeenNthCalledWith(2, 0, 1);
    expect(fetchHostWallet).toHaveBeenNthCalledWith(3, 0, 50);
    expect(withdrawFromHostWallet).not.toHaveBeenCalled();
  });

  it('loads a bounded statement page on demand while retaining the cached wallet', async () => {
    const statement = vi.fn().mockResolvedValue(walletPage(100));
    fetchHostWallet.mockImplementation(statement);
    render(<HostApiProvider><WalletProbe /></HostApiProvider>);
    await screen.findByText('100');
    screen.getByRole('button', { name: 'Load statement' }).click();

    await waitFor(() => expect(fetchHostWallet).toHaveBeenLastCalledWith(0, 50));
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
