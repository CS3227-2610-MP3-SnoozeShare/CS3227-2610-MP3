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

export function HostWalletPanel({ wallet, statement }: { wallet: WalletSummary; statement: StatementRow[] }) {
  const [action, setAction] = useState<'Top up' | 'Withdraw' | null>(null);
  return (
    <div className="space-y-5">
      <div className="grid grid-cols-1 items-center gap-4 sm:grid-cols-[minmax(0,5fr)_minmax(160px,1fr)]">
        <div className="rounded-xl bg-[var(--surface-inset)] p-5">
          <p className="text-xs font-bold tracking-wide text-[var(--text-subtle)]">AVAILABLE BALANCE</p>
          <p className="mt-2 flex items-baseline gap-2 text-3xl font-bold">
            <span>${formatMoney(wallet.balance)}</span>
            <span className="text-sm font-medium text-[var(--text-subtle)]">SGD</span>
          </p>
          <p className="mt-2 text-sm text-[var(--text-subtle)]">Includes $0.00 currently held in escrow for pending bookings</p>
        </div>
        <div className="flex flex-col gap-2">
          <button className="w-full rounded-lg bg-[var(--success)] px-3 py-2 text-sm font-semibold text-white hover:brightness-95" onClick={() => setAction('Top up')} type="button">Top up</button>
          <button className="w-full rounded-lg border border-[var(--border-muted)] bg-card px-3 py-2 text-sm hover:bg-[var(--surface-inset)]" onClick={() => setAction('Withdraw')} type="button">Withdraw</button>
        </div>
      </div>
      <section>
        <h2 className="mb-3 text-base font-semibold">Transaction statement</h2>
        {statement.length ? (
          <div className="overflow-hidden rounded-xl border border-[var(--border-muted)] bg-card">
            <Table className="text-xs">
              <Thead className="bg-[var(--surface-inset)]">
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
      <Modal open={Boolean(action)} title={`${action ?? ''}`} description={action === 'Top up' ? 'Mocked top-up — no real payment gateway is charged.' : 'Mocked withdrawal — no real payout rail.'} onClose={() => setAction(null)} onConfirm={() => setAction(null)} confirmLabel={action ?? 'Confirm'} />
    </div>
  );
}
