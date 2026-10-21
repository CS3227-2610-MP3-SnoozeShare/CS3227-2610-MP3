'use client';

import { useState } from 'react';
import type { StatementRow, WalletSummary } from './hostTypes';
import { Modal } from '@/components/ui/modal';
import { Table, Tbody, Td, Th, Thead, Tr } from '@/components/ui/table';

export function HostWalletPanel({ wallet, statement }: { wallet: WalletSummary; statement: StatementRow[] }) {
  const [action, setAction] = useState<'Top up' | 'Withdraw' | null>(null);
  return <div className="space-y-5"><div className="ss-card flex flex-wrap items-start justify-between gap-4 p-5"><div><p className="text-xs font-bold tracking-wide text-[var(--text-subtle)]">AVAILABLE BALANCE</p><p className="mt-2 text-3xl font-bold">Wallet: ${wallet.balance.toLocaleString('en-US', { minimumFractionDigits: 2 })}</p><p className="mt-2 text-sm text-[var(--text-subtle)]">Includes $0.00 currently held in escrow for pending bookings</p></div><div className="flex gap-2"><button className="rounded-lg bg-primary px-3 py-2 text-sm font-semibold text-primary-foreground" onClick={() => setAction('Top up')} type="button">Top up</button><button className="rounded-lg border border-border px-3 py-2 text-sm" onClick={() => setAction('Withdraw')} type="button">Withdraw</button></div></div><section><h2 className="mb-3 text-lg font-semibold">Transaction statement</h2>{statement.length ? <Table><Thead><Tr><Th>DATE</Th><Th>TYPE</Th><Th>RELATED TO</Th><Th>AMOUNT</Th><Th>BALANCE AFTER</Th></Tr></Thead><Tbody>{statement.map((row) => <Tr key={`${row.date}-${row.type}`}><Td>{row.date}</Td><Td>{row.type}</Td><Td>{row.relatedTo}</Td><Td className={row.amount < 0 ? 'text-destructive' : 'text-[var(--success)]'}>{row.amount >= 0 ? '+' : ''}${Math.abs(row.amount).toFixed(2)}</Td><Td>${row.balanceAfter.toFixed(2)}</Td></Tr>)}</Tbody></Table> : <div className="ss-card p-8 text-center text-[var(--text-subtle)]">No transactions yet.</div>}</section><Modal open={Boolean(action)} title={`${action ?? ''}`} description={action === 'Top up' ? 'Mocked top-up — no real payment gateway is charged.' : 'Mocked withdrawal — no real payout rail.'} onClose={() => setAction(null)} onConfirm={() => setAction(null)} confirmLabel={action ?? 'Confirm'} /></div>;
}
