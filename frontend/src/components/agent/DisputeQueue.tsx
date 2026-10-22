'use client';

import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';
import { isSessionFailure } from '@/lib/api';
import { fetchQueue } from '@/lib/agent-api/disputes';
import type { AssigneeFilter, TicketStatus } from '@/lib/agent-api/types';
import { SessionNotice } from './SessionNotice';
import { usePolling } from './usePolling';

const CHIPS: { value: AssigneeFilter; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'UNASSIGNED', label: 'Unassigned' },
  { value: 'MINE', label: 'Mine' },
];

const STATUS_LABEL: Record<TicketStatus, string> = {
  OPEN: 'Unassigned',
  IN_REVIEW: 'In review',
  RESOLVED_APPROVED: 'Resolved - approved',
  RESOLVED_REJECTED: 'Resolved - rejected',
};

export function DisputeQueue() {
  const [assignee, setAssignee] = useState<AssigneeFilter>('ALL');
  const current = useRef(assignee);
  current.current = assignee;
  const { data, error, loading, reload } = usePolling(() => fetchQueue(current.current), 30000);
  const first = useRef(true);

  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    reload();
  }, [assignee, reload]);

  if (error && isSessionFailure(error)) return <SessionNotice error={error} />;

  const items = data?.items ?? [];
  const unassigned = data?.unassignedCount ?? 0;

  return (
    <section className="mx-auto max-w-6xl p-6">
      <div className="mb-4 flex items-center gap-3">
        <h1 className="text-xl font-extrabold">Dispute queue</h1>
        {unassigned > 0 && (
          <span className="rounded-full bg-red-600 px-3 py-1 text-xs font-bold text-white">{unassigned} unassigned</span>
        )}
      </div>
      <div className="mb-4 flex gap-2" role="group" aria-label="Filter disputes">
        {CHIPS.map((chip) => (
          <button
            key={chip.value}
            type="button"
            disabled={loading && !data}
            aria-pressed={assignee === chip.value}
            onClick={() => setAssignee(chip.value)}
            className={`rounded-full border border-[var(--border)] px-4 py-1.5 text-xs font-semibold ${
              assignee === chip.value ? 'bg-primary text-primary-foreground' : 'bg-card'
            }`}
          >
            {chip.label}
          </button>
        ))}
      </div>
      {error !== undefined && (
        <div role="alert" className="mb-3 flex items-center justify-between rounded-md border border-red-300 bg-red-50 p-3 text-sm text-red-800">
          <span>{error instanceof Error && error.message ? error.message : 'Could not load disputes'}</span>
          <button type="button" className="rounded-md border border-red-300 px-3 py-1 text-xs font-semibold" onClick={reload}>
            Retry
          </button>
        </div>
      )}
      <table className="w-full border-collapse text-left text-sm">
        <thead>
          <tr className="border-b border-[var(--border)] text-xs uppercase text-muted-foreground">
            <th scope="col" className="p-2">Ticket</th>
            <th scope="col" className="p-2">Subject</th>
            <th scope="col" className="p-2">Booking</th>
            <th scope="col" className="p-2">Guest / Host</th>
            <th scope="col" className="p-2">Assigned</th>
            <th scope="col" className="p-2">Status</th>
          </tr>
        </thead>
        <tbody>
          {loading && !data ? (
            <tr><td colSpan={6} className="p-4 text-muted-foreground">Loading disputes...</td></tr>
          ) : items.length === 0 ? (
            <tr><td colSpan={6} className="p-4 text-muted-foreground">No disputes match this filter</td></tr>
          ) : (
            items.map((item) => (
              <tr key={item.ticketId} className="border-b border-[var(--border)]">
                <td className="p-2 font-semibold">
                  <Link href={`/agent/disputes/${item.ticketId}`} className="underline">{item.ticketLabel}</Link>
                </td>
                <td className="p-2">{item.title}</td>
                <td className="p-2">{item.listingTitle}</td>
                <td className="p-2">{item.guestName} / {item.hostName}</td>
                <td className="p-2">{item.assignedAgentName ?? '—'}</td>
                <td className="p-2"><span className="rounded-full border border-[var(--border)] px-2 py-0.5 text-xs">{STATUS_LABEL[item.status]}</span></td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </section>
  );
}
