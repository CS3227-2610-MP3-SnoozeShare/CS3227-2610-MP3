'use client';

import { useState } from 'react';
import type { StatementRow, WalletSummary } from './hostTypes';
import { Modal } from '@/components/ui/modal';
import { Table, Tbody, Td, Th, Thead, Tr } from '@/components/ui/table';

function formatMoney(amount: number) {
  return amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function transactionTypeClass(type: string) {
  if (type === 'ESCROW_HOLD') return 'bg-[var(--warning-subtle)] text-[var(--warning)]';
  if (type === 'WITHDRAWAL') return 'bg-[var(--surface-inset)] text-[var(--text-subtle)]';
  return 'bg-[var(--success-subtle)] text-[var(--success)]';
}

type Props = { wallet: WalletSummary; statement: StatementRow[]; onMutation?: (operation: 'top-up' | 'withdraw', amount: number) => Promise<unknown>; mutationPending?: boolean; recoveryRequired?: boolean; onRetryRecovery?: () => Promise<void> };
export function HostWalletPanel({ wallet, statement, onMutation, mutationPending = false, recoveryRequired = false, onRetryRecovery }: Props) {
  const [action, setAction] = useState<'Top up' | 'Withdraw' | null>(null);
  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');
  const [recovering, setRecovering] = useState(false);
  const busy = mutationPending || recoveryRequired || recovering;
  const close = () => { if (mutationPending) return; setAction(null); setAmount(''); setError(''); };
  const confirm = async () => {
    const parsedAmount = Number(amount);
    if (!Number.isFinite(parsedAmount) || parsedAmount < 0.01 || parsedAmount > 100000 || Math.round(parsedAmount * 100) !== parsedAmount * 100) {
      setError('Enter an amount from $0.01 to $100,000.00 with up to two decimal places.');
      return;
    }
    if (!onMutation) { setAction(null); setAmount(''); return; }
    setError('');
    try {
      await onMutation(action === 'Top up' ? 'top-up' : 'withdraw', parsedAmount);
      setAction(null);
      setAmount('');
    } catch {
      setError('Unable to complete this request. Check the wallet balance and statement before trying again.');
    }
  };
  return (
    <div className="flex min-h-0 flex-1 flex-col gap-5 overflow-hidden">
      <div className="grid shrink-0 grid-cols-1 items-center gap-4 sm:grid-cols-[minmax(0,5fr)_minmax(160px,1fr)]">
        <div className="rounded-xl bg-[var(--surface-inset)] p-5">
          <p className="text-xs font-bold tracking-wide text-[var(--text-subtle)]">AVAILABLE BALANCE</p>
          <p className="mt-2 flex items-baseline gap-2 text-3xl font-bold">
            <span>${formatMoney(wallet.balance)}</span>
            <span className="text-sm font-medium text-[var(--text-subtle)]">SGD</span>
          </p>
          <p className="mt-2 text-sm text-[var(--text-subtle)]">Includes $0.00 currently held in escrow for pending bookings</p>
        </div>
        <div className="flex flex-col gap-2">
          <button className="w-full rounded-lg bg-[var(--success)] px-3 py-2 text-sm font-semibold text-white hover:brightness-95 disabled:opacity-50" disabled={busy} onClick={() => setAction('Top up')} type="button">Top up</button>
          <button className="w-full rounded-lg border border-[var(--border-muted)] bg-card px-3 py-2 text-sm hover:bg-[var(--surface-inset)] disabled:opacity-50" disabled={busy} onClick={() => setAction('Withdraw')} type="button">Withdraw</button>
        </div>
      </div>
      {recoveryRequired && <div className="flex items-center justify-between gap-3 rounded-lg border border-[var(--warning)] p-3 text-sm" role="alert"><span>Wallet status needs confirmation. Refresh before making another change.</span><button className="rounded-md border px-3 py-1.5 font-semibold disabled:opacity-50" disabled={recovering} onClick={() => { setRecovering(true); void onRetryRecovery?.().catch(() => setError('Wallet refresh failed. Keep wallet actions disabled and retry refresh.')).finally(() => setRecovering(false)); }} type="button">{recovering ? 'Refreshing…' : 'Refresh wallet'}</button></div>}
      <section className="flex min-h-0 flex-1 flex-col">
        <h2 className="mb-3 shrink-0 text-base font-semibold">Transaction statement</h2>
        {statement.length ? (
          <div aria-label="Transaction statement table" className="min-h-0 flex-1 overflow-auto rounded-xl border border-[var(--border-muted)] bg-card">
            <Table className="text-xs" scrollX={false}>
              <Thead className="sticky top-0 z-10 bg-[var(--surface-inset)]">
                <Tr><Th>DATE</Th><Th>TYPE</Th><Th>RELATED TO</Th><Th>AMOUNT</Th><Th>BALANCE AFTER</Th></Tr>
              </Thead>
              <Tbody>
                {statement.map((row) => (
                  <Tr key={`${row.date}-${row.type}`}>
                    <Td className="py-2.5">{row.date}</Td>
                    <Td className="py-2.5"><span className={`inline-flex rounded-full px-2 py-0.5 text-[10px] font-bold tracking-wide ${transactionTypeClass(row.type)}`}>{row.type}</span></Td>
                    <Td className="py-2.5">{row.relatedTo}</Td>
                    <Td className={`py-2.5 ${row.amount < 0 ? 'text-destructive' : row.amount > 0 ? 'text-[var(--success)]' : ''}`}>
                      {row.amount > 0 ? `+$${formatMoney(row.amount)}` : row.amount < 0 ? `-$${formatMoney(Math.abs(row.amount))}` : '$0.00'}
                    </Td>
                    <Td className="py-2.5">${formatMoney(row.balanceAfter)}</Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          </div>
        ) : <div className="ss-card p-8 text-center text-[var(--text-subtle)]">No transactions yet.</div>}
      </section>
      <Modal open={Boolean(action)} title={`${action ?? ''}`} description={action === 'Top up' ? 'Mocked top-up — no real payment gateway is charged.' : 'Mocked withdrawal — no real payout rail.'} onClose={close} onConfirm={() => void confirm()} confirmLabel={action ?? 'Confirm'} confirmDisabled={busy}>
        <label className="mt-4 block text-sm font-medium" htmlFor="wallet-amount">Amount (SGD)</label>
        <input id="wallet-amount" aria-label="Amount" className="mt-1 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm" inputMode="decimal" onChange={(event) => setAmount(event.target.value)} placeholder="0.00" type="number" min="0.01" max="100000" step="0.01" value={amount} />
        {error && <p className="mt-2 text-sm text-destructive" role="alert">{error}</p>}
      </Modal>
    </div>
  );
}
