'use client';

import { useCallback, useEffect, useState } from 'react';
import { getWallet, getTransactions, topUp, withdraw, type WalletResponse, type TransactionResponse } from '@/lib/guest-api';
import { BackendApiError } from '@/lib/api';
import { Modal } from '@/components/ui/modal';
import { Table, Tbody, Td, Th, Thead, Tr } from '@/components/ui/table';

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

function formatMoney(amount: number) {
  return amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function transactionTypeClass(type: string) {
  if (type === 'BOOKING_HOLD' || type === 'ESCROW_HOLD') return 'bg-[var(--warning-subtle)] text-[var(--warning)]';
  if (type === 'WITHDRAWAL') return 'bg-[var(--surface-inset)] text-[var(--text-subtle)]';
  return 'bg-[var(--success-subtle)] text-[var(--success)]';
}

export default function WalletPage() {
  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [action, setAction] = useState<'Top up' | 'Withdraw' | null>(null);
  const [amount, setAmount] = useState('');
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

  const closeModal = () => { if (actionLoading) return; setAction(null); setAmount(''); setActionError(null); };

  const confirm = async () => {
    if (!wallet) return;
    const parsedAmount = Number(amount);
    if (!Number.isFinite(parsedAmount) || parsedAmount < 0.01 || Math.round(parsedAmount * 100) !== parsedAmount * 100) {
      setActionError('Enter an amount of at least $0.01 with up to two decimal places.');
      return;
    }
    if (parsedAmount > 100000) {
      setActionError('The maximum amount per wallet transaction is $100,000.00.');
      return;
    }
    if (action === 'Withdraw' && parsedAmount > wallet.balance) {
      setActionError(`You can withdraw up to your available balance of $${formatMoney(wallet.balance)}.`);
      return;
    }
    setActionLoading(true);
    setActionError(null);
    try {
      const w = action === 'Top up' ? await topUp(parsedAmount) : await withdraw(parsedAmount);
      setWallet(w);
      const txs = await getTransactions();
      setTransactions(txs);
      setAction(null);
      setAmount('');
    } catch (e) {
      setActionError(e instanceof BackendApiError ? e.message : `${action === 'Top up' ? 'Top-up' : 'Withdrawal'} failed`);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <main aria-label="Wallet" className="w-full max-w-none px-6 py-6 flex min-h-0 flex-1 flex-col overflow-hidden">

      {loading && <div className="ss-card h-64 animate-pulse bg-[var(--surface-inset)]" />}
      {error && <div className="ss-card p-8 text-center text-destructive">{error}</div>}

      {!loading && !error && wallet && (
        <div className="flex min-h-0 flex-1 flex-col gap-5">
          {/* Balance + actions row */}
          <div className="grid shrink-0 grid-cols-1 items-center gap-4 sm:grid-cols-[minmax(0,5fr)_minmax(160px,1fr)]">
            <div data-testid="balance-card" className="rounded-xl bg-[var(--surface-inset)] p-5">
              <p className="text-xs font-bold tracking-wide text-[var(--text-subtle)]">AVAILABLE BALANCE</p>
              <p className="mt-2 flex items-baseline gap-2 text-3xl font-bold">
                <span>${formatMoney(wallet.balance)}</span>
                <span className="text-sm font-medium text-[var(--text-subtle)]">{wallet.currency}</span>
              </p>
            </div>
            <div className="flex flex-col gap-2">
              <button className="w-full rounded-lg bg-[var(--success)] px-3 py-2 text-sm font-semibold text-white hover:brightness-95 disabled:opacity-50" disabled={actionLoading} onClick={() => setAction('Top up')} type="button">Top up</button>
              <button className="w-full rounded-lg border border-[var(--border-muted)] bg-card px-3 py-2 text-sm hover:bg-[var(--surface-inset)] disabled:opacity-50" disabled={actionLoading} onClick={() => setAction('Withdraw')} type="button">Withdraw</button>
            </div>
          </div>

          {/* Transaction statement */}
          <section className="flex min-h-0 flex-1 flex-col">
            <h2 className="mb-3 shrink-0 text-base font-semibold">Transaction statement</h2>
            {transactions.length === 0 && (
              <div data-testid="no-transactions" className="ss-card p-8 text-center text-[var(--text-subtle)]">No transactions yet.</div>
            )}
            {transactions.length > 0 && (
              <div data-testid="transaction-list" aria-label="Transaction statement table" className="min-h-0 flex-1 overflow-auto rounded-xl border border-[var(--border-muted)] bg-card">
                <Table className="text-xs" scrollX={false}>
                  <Thead className="sticky top-0 z-10 bg-[var(--surface-inset)]">
                    <Tr><Th>DATE</Th><Th>TYPE</Th><Th>AMOUNT</Th><Th>BALANCE AFTER</Th></Tr>
                  </Thead>
                  <Tbody>
                    {transactions.map((tx) => (
                      <Tr key={tx.id}>
                        <Td className="py-2.5">{new Date(tx.timestamp).toLocaleDateString()}</Td>
                        <Td className="py-2.5"><span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-bold tracking-wide ${transactionTypeClass(tx.actionType)}`}>{actionLabels[tx.actionType] ?? tx.actionType}</span></Td>
                        <Td className={`py-2.5 ${tx.walletAdjustment < 0 ? 'text-destructive' : tx.walletAdjustment > 0 ? 'text-[var(--success)]' : ''}`}>
                          {tx.walletAdjustment > 0 ? `+$${formatMoney(tx.walletAdjustment)}` : tx.walletAdjustment < 0 ? `-$${formatMoney(Math.abs(tx.walletAdjustment))}` : '$0.00'}
                        </Td>
                        <Td className="py-2.5">${formatMoney(tx.balanceAfter)}</Td>
                      </Tr>
                    ))}
                  </Tbody>
                </Table>
              </div>
            )}
          </section>
        </div>
      )}

      {/* Top-up / Withdraw modal */}
      <Modal
        open={Boolean(action)}
        title={action === 'Top up' ? 'Top up wallet' : 'Withdraw funds'}
        description={action === 'Top up'
          ? `Current balance $${formatMoney(wallet?.balance ?? 0)} ${wallet?.currency ?? 'SGD'}`
          : `Available to withdraw $${formatMoney(wallet?.balance ?? 0)} ${wallet?.currency ?? 'SGD'}`}
        onClose={closeModal}
        onConfirm={() => void confirm()}
        confirmLabel={action === 'Top up' ? 'Confirm top up' : 'Confirm withdraw'}
        confirmTone={action === 'Top up' ? 'success' : 'primary'}
        confirmDisabled={actionLoading}
      >
        <label className="mt-4 block text-sm font-medium" htmlFor="wallet-amount">Amount</label>
        <div className="mt-1 flex items-center rounded-lg border border-input bg-background px-3">
          <span aria-hidden="true" className="mr-2 text-lg font-semibold">$</span>
          <input id="wallet-amount" aria-label="Amount" className="h-11 w-full bg-transparent text-lg font-semibold outline-none" inputMode="decimal" onChange={(event) => setAmount(event.target.value)} placeholder="0.00" type="number" min="0.01" max={action === 'Withdraw' ? Math.min(wallet?.balance ?? 0, 100000) : 100000} step="0.01" value={amount} />
        </div>
        {action === 'Top up' ? (
          <div aria-label="Top-up amount presets" className="mt-2 flex flex-wrap gap-2">
            {[50, 100, 500, 1000].map((preset) => (
              <button className="rounded-full border border-[var(--border-muted)] px-3 py-1 text-sm hover:bg-[var(--surface-inset)]" key={preset} onClick={() => { setAmount(String(preset)); setActionError(null); }} type="button">${preset.toLocaleString('en-US')}</button>
            ))}
          </div>
        ) : (
          <button className="mt-2 text-sm font-semibold text-[var(--primary)] underline underline-offset-2" onClick={() => { setAmount((wallet?.balance ?? 0).toFixed(2)); setActionError(null); }} type="button">Withdraw full available balance</button>
        )}
        <p className="mt-4 rounded-md bg-[var(--surface-inset)] px-3 py-2 text-xs leading-tight text-foreground">
          {action === 'Top up'
            ? 'Mocked top-up — no real payment gateway is charged. This records a TOP_UP wallet transaction and credits your balance immediately.'
            : 'Mocked withdrawal — no real payout rail. You can only withdraw up to your available balance.'}
        </p>
        {actionError && <p className="mt-2 text-sm text-destructive" role="alert">{actionError}</p>}
      </Modal>
    </main>
  );
}
