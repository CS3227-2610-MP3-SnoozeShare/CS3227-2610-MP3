'use client';

import { useCallback, useEffect, useState } from 'react';
import { getWallet, getTransactions, topUp, withdraw, type WalletResponse, type TransactionResponse } from '@/lib/guest-api';
import { BackendApiError } from '@/lib/api';

const actionLabels: Record<string, string> = {
  TOP_UP: 'Top-up',
  WITHDRAWAL: 'Withdrawal',
  BOOKING_HOLD: 'Booking hold',
  BOOKING_RELEASE: 'Booking release',
  BOOKING_CHARGE: 'Booking charge',
  CANCELLATION_REFUND: 'Cancellation refund',
  HOST_PAYOUT: 'Host payout',
  DISPUTE_REFUND: 'Dispute refund',
  DISPUTE_CHARGE: 'Dispute charge',
};

export default function WalletPage() {
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [topUpAmount, setTopUpAmount] = useState('');
  const [withdrawAmount, setWithdrawAmount] = useState('');
  const [actionLoading, setActionLoading] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [w, txs] = await Promise.all([getWallet(), getTransactions()]);
      setWallet(w);
      setTransactions(txs);
    } catch (e) {
      setError(e instanceof BackendApiError ? e.message : 'Failed to load wallet');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleTopUp = useCallback(async () => {
    const amount = parseFloat(topUpAmount);
    if (!amount || amount <= 0) return;
    setActionLoading(true);
    setActionError(null);
    try {
      const w = await topUp(amount);
      setWallet(w);
      setTopUpAmount('');
      const txs = await getTransactions();
      setTransactions(txs);
    } catch (e) {
      setActionError(e instanceof BackendApiError ? e.message : 'Top-up failed');
    } finally {
      setActionLoading(false);
    }
  }, [topUpAmount]);

  const handleWithdraw = useCallback(async () => {
    const amount = parseFloat(withdrawAmount);
    if (!amount || amount <= 0) return;
    setActionLoading(true);
    setActionError(null);
    try {
      const w = await withdraw(amount);
      setWallet(w);
      setWithdrawAmount('');
      const txs = await getTransactions();
      setTransactions(txs);
    } catch (e) {
      setActionError(e instanceof BackendApiError ? e.message : 'Withdrawal failed');
    } finally {
      setActionLoading(false);
    }
  }, [withdrawAmount]);

  return (
    <main aria-label="Wallet" className="min-h-[calc(100vh-4rem)] p-6">
      <h2 className="mb-6 text-2xl font-bold">Wallet</h2>

      {loading && <p className="text-muted-foreground">Loading wallet...</p>}
      {error && <p className="text-red-600">{error}</p>}

      {!loading && !error && wallet && (
        <div className="space-y-6">
          {/* Balance card */}
          <div data-testid="balance-card" className="ss-card p-6">
            <p className="text-sm text-muted-foreground">Available Balance</p>
            <p className="mt-1 text-3xl font-bold">${wallet.balance.toFixed(2)}</p>
            <p className="mt-1 text-xs text-muted-foreground">{wallet.currency}</p>
          </div>

          {/* Top-up / Withdraw */}
          <div className="flex flex-wrap gap-4">
            <form onSubmit={(e) => { e.preventDefault(); handleTopUp(); }}
              className="ss-card flex items-end gap-2 p-4">
              <div>
                <label className="text-xs font-medium text-muted-foreground">Top-up amount</label>
                <input type="number" min="0.01" step="0.01" value={topUpAmount}
                  onChange={(e) => setTopUpAmount(e.target.value)}
                  className="mt-1 w-32 rounded border px-2 py-1.5 text-sm" />
              </div>
              <button type="submit" disabled={actionLoading || !topUpAmount}
                className="rounded bg-primary px-4 py-1.5 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50">
                Top Up
              </button>
            </form>
            <form onSubmit={(e) => { e.preventDefault(); handleWithdraw(); }}
              className="ss-card flex items-end gap-2 p-4">
              <div>
                <label className="text-xs font-medium text-muted-foreground">Withdraw amount</label>
                <input type="number" min="0.01" step="0.01" value={withdrawAmount}
                  onChange={(e) => setWithdrawAmount(e.target.value)}
                  className="mt-1 w-32 rounded border px-2 py-1.5 text-sm" />
              </div>
              <button type="submit" disabled={actionLoading || !withdrawAmount}
                className="rounded bg-primary px-4 py-1.5 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50">
                Withdraw
              </button>
            </form>
          </div>
          {actionError && <p className="text-sm text-red-600">{actionError}</p>}

          {/* Transaction history */}
          <div>
            <h3 className="mb-3 text-lg font-semibold">Transaction History</h3>
            {transactions.length === 0 && (
              <p data-testid="no-transactions" className="text-sm text-muted-foreground">No transactions yet.</p>
            )}
            {transactions.length > 0 && (
              <div data-testid="transaction-list" className="ss-card overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b text-left text-xs text-muted-foreground">
                      <th className="px-3 py-2">Date</th>
                      <th className="px-3 py-2">Type</th>
                      <th className="px-3 py-2 text-right">Amount</th>
                      <th className="px-3 py-2 text-right">Balance</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {transactions.map((tx) => (
                      <tr key={tx.id}>
                        <td className="px-3 py-2 text-muted-foreground">{new Date(tx.timestamp).toLocaleDateString()}</td>
                        <td className="px-3 py-2">{actionLabels[tx.actionType] ?? tx.actionType}</td>
                        <td className={`px-3 py-2 text-right font-medium ${tx.walletAdjustment >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                          {tx.walletAdjustment >= 0 ? '+' : ''}{tx.walletAdjustment.toFixed(2)}
                        </td>
                        <td className="px-3 py-2 text-right">${tx.balanceAfter.toFixed(2)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}
    </main>
  );
}
