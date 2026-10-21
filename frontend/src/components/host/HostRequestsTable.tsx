'use client';

import { useState } from 'react';
import type { BookingRequest, RequestData } from './hostTypes';
import { Modal } from '@/components/ui/modal';
import { Table, Tbody, Td, Th, Thead, Tr } from '@/components/ui/table';
import { hostSafeErrorMessage } from '@/lib/hostApi';

type Decision = { request: BookingRequest; action: 'Approve' | 'Reject' } | null;

function money(amount: number) {
  return `$${amount.toFixed(2)}`;
}

type Props = { requests: RequestData; onDecision?: (request: BookingRequest, approve: boolean, message?: string) => Promise<void>; pendingHasMore?: boolean; historyHasMore?: boolean; pendingPage?: number; historyPage?: number; onNextPending?: () => void; onPreviousPending?: () => void; onNextHistory?: () => void; onPreviousHistory?: () => void };
export function HostRequestsTable({ requests, onDecision, pendingHasMore, historyHasMore, pendingPage = 0, historyPage = 0, onNextPending, onPreviousPending, onNextHistory, onPreviousHistory }: Props) {
  const [decision, setDecision] = useState<Decision>(null);
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const table = (rows: BookingRequest[], pending = false) => (
    <div aria-label={pending ? 'Pending requests table' : 'Past requests table'} className={`rounded-xl border border-border bg-white ${pending ? 'max-h-[244px] overflow-auto' : 'min-h-0 flex-1 overflow-auto'}`}>
      <Table scrollX={false}>
        <Thead className={`sticky top-0 z-10 bg-[var(--surface-inset)] ${pending ? 'whitespace-nowrap' : ''}`}>
          <Tr><Th>GUEST</Th><Th>LISTING</Th><Th>DATES</Th><Th>NIGHTS</Th>{pending ? <><Th>GROSS</Th><Th>NET EARNING</Th><Th>GUEST RATING</Th><Th>ACTION</Th></> : <><Th>TOTAL</Th><Th>STATUS</Th></>}</Tr>
        </Thead>
        <Tbody>
          {rows.map((request) => (
            <Tr className={pending ? 'whitespace-nowrap' : undefined} key={request.id}>
              <Td className={`font-semibold ${pending ? 'whitespace-nowrap py-2' : ''}`}>{request.guest}</Td><Td className={pending ? 'whitespace-nowrap py-2' : undefined}>{request.listing}</Td><Td className={pending ? 'whitespace-nowrap py-2' : undefined}>{request.dates}</Td><Td className={pending ? 'whitespace-nowrap py-2' : undefined}>{request.nights}</Td>
              <Td className={`${!pending ? 'font-semibold' : ''} ${pending ? 'whitespace-nowrap py-2' : ''}`}>{money(request.gross)}</Td>
              {pending ? <>
                <Td className="whitespace-nowrap py-2 font-semibold text-primary">{request.net == null ? '—' : money(request.net)}</Td>
                <Td className="whitespace-nowrap py-2">{request.rating == null ? 'No ratings yet' : `${request.rating.toFixed(1)} ★`}</Td>
                <Td className="whitespace-nowrap py-2"><div className="flex gap-2">
                  <button className="cursor-pointer rounded-lg bg-[var(--success)] px-3 py-1.5 text-sm font-semibold text-white hover:opacity-90" onClick={() => setDecision({ request, action: 'Approve' })} type="button">Approve</button>
                  <button className="cursor-pointer rounded-lg border border-destructive px-3 py-1.5 text-sm font-semibold text-destructive hover:bg-destructive/5" onClick={() => setDecision({ request, action: 'Reject' })} type="button">Reject</button>
                </div></Td>
              </> : <Td><span className={`ss-pill ${request.status === 'confirmed' ? 'ss-status-success' : 'ss-status-danger'}`}>{request.status.toUpperCase()}</span></Td>}
            </Tr>
          ))}
        </Tbody>
      </Table>
    </div>
  );

  const selected = decision?.request;
  const isApproval = decision?.action === 'Approve';
  const description = isApproval
    ? 'The booking moves to CONFIRMED and the guest is notified. Funds stay held in escrow until check-in.'
    : 'The guest will be notified and the held funds fully refunded (ESCROW_REFUND) — no fee is charged for a host rejection.';

  return <div className="flex min-h-0 flex-1 flex-col gap-5 overflow-hidden">
    <section aria-label="Pending requests" className="shrink-0">
      {requests.pending.length ? table(requests.pending, true) : <div className="ss-card p-6 text-center text-[var(--text-subtle)]">No pending requests</div>}
      {onDecision && (pendingPage > 0 || pendingHasMore) && <div className="mt-2 flex justify-between"><button disabled={pendingPage === 0} onClick={onPreviousPending} type="button">Previous pending</button><span>Page {pendingPage + 1}</span><button disabled={!pendingHasMore} onClick={onNextPending} type="button">Next pending</button></div>}
      <p className="mt-2 text-xs text-[var(--text-subtle)]">Net earning = gross − 3% platform fee. Guest rating is their average review score, or &quot;No ratings yet&quot;.</p>
    </section>
    <section aria-label="Past requests" className="flex min-h-0 flex-1 flex-col">
      <h2 className="mb-2 shrink-0 text-sm font-bold uppercase tracking-wide text-[var(--text-subtle)]">PAST REQUESTS</h2>
      {requests.past.length ? table(requests.past) : <div className="ss-card p-6 text-center text-[var(--text-subtle)]">No past requests</div>}
      {onDecision && (historyPage > 0 || historyHasMore) && <div className="mt-2 flex justify-between"><button disabled={historyPage === 0} onClick={onPreviousHistory} type="button">Previous history</button><span>Page {historyPage + 1}</span><button disabled={!historyHasMore} onClick={onNextHistory} type="button">Next history</button></div>}
    </section>
    {error && <p className="text-sm text-destructive" role="alert">{error}</p>}
    <Modal
      open={Boolean(decision)}
      title={`${decision?.action ?? ''} booking request?`}
      description={description}
      onClose={() => { if (!submitting) setDecision(null); }}
      onConfirm={() => { if (!decision) return; setSubmitting(true); setError(''); void (async () => { try { await onDecision?.(decision.request, decision.action === 'Approve', message || undefined); setDecision(null); setMessage(''); } catch (caught) { setError(hostSafeErrorMessage(caught)); } finally { setSubmitting(false); } })(); }}
      confirmDisabled={submitting}
      confirmLabel={submitting ? 'Saving…' : `Confirm ${decision?.action?.toLowerCase() ?? ''}`.trim()}
      confirmTone={isApproval ? 'success' : 'danger'}
      layout="decision"
      summary={selected && <div aria-label="Booking summary" className={`mt-4 rounded-lg p-4 text-white ${isApproval ? 'bg-[var(--success)]' : 'bg-destructive'}`} role="group">
        <p className="text-sm font-bold">{selected.listing}</p>
        <p className="text-xs">{selected.guest} · {selected.dates} ({selected.nights} nights)</p>
        <p className="mt-1 text-2xl font-bold">{money(selected.gross)} <span className="text-xs font-normal">{isApproval ? 'earning' : 'to be refunded'}</span></p>
      </div>}
    >
      {decision?.action === 'Reject' && <label className="mt-4 block text-xs font-semibold" htmlFor="guest-message">Message to guest (optional)<textarea id="guest-message" className="mt-1 min-h-[52px] w-full rounded-md border border-border bg-[#fdf8f0] px-2 py-2 text-sm font-normal" onChange={(event) => setMessage(event.target.value)} placeholder="e.g. dates unavailable due to maintenance" rows={1} value={message} /></label>}
    </Modal>
  </div>;
}
