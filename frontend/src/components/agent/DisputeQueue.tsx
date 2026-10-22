'use client';

import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';
import { isSessionFailure } from '@/lib/api';
import { fetchQueue } from '@/lib/agent-api/disputes';
import type { AssigneeFilter, TicketStatus } from '@/lib/agent-api/types';
import { ErrorBanner, Pill, pageClass, rowClass, tableCardClass, tdClass, thClass, theadRowClass, titleClass, type Tone } from './agentUi';
import { SessionNotice } from './SessionNotice';
import { usePolling } from './usePolling';

const CHIPS: { value: AssigneeFilter; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'UNASSIGNED', label: 'Unassigned' },
  { value: 'MINE', label: 'Mine' },
];

export const STATUS: Record<TicketStatus, { label: string; tone: Tone }> = {
  OPEN: { label: 'Unassigned', tone: 'danger' },
  IN_REVIEW: { label: 'In review', tone: 'warning' },
  RESOLVED_APPROVED: { label: 'Resolved - approved', tone: 'success' },
  RESOLVED_REJECTED: { label: 'Resolved - rejected', tone: 'success' },
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
    <section className={pageClass}>
      <div className="flex items-center gap-2.5">
        <h1 className={titleClass}>Dispute queue</h1>
        {unassigned > 0 && <Pill tone="danger" className="text-[11px] normal-case">{unassigned} unassigned</Pill>}
        <div className="ml-auto flex gap-2" role="group" aria-label="Filter disputes">
          {CHIPS.map((chip) => (
            <button
              key={chip.value}
              type="button"
              disabled={loading && !data}
              aria-pressed={assignee === chip.value}
              onClick={() => setAssignee(chip.value)}
              className={`rounded-full px-[13px] py-[5px] text-xs ${
                assignee === chip.value ? 'bg-accent font-bold text-primary' : 'bg-muted text-[var(--text-muted)]'
              }`}
            >
              {chip.label}
            </button>
          ))}
        </div>
      </div>
      {error !== undefined && (
        <ErrorBanner message={error instanceof Error && error.message ? error.message : 'Could not load disputes'} onRetry={reload} />
      )}
      <div className={tableCardClass}>
        <table className="w-full table-fixed border-collapse text-left">
          <colgroup>
            <col style={{ width: '12%' }} /><col style={{ width: '28%' }} /><col style={{ width: '20%' }} />
            <col style={{ width: '20%' }} /><col style={{ width: '12%' }} /><col style={{ width: '8%' }} />
          </colgroup>
          <thead>
            <tr className={theadRowClass}>
              <th scope="col" className={thClass}>Ticket</th>
              <th scope="col" className={thClass}>Subject</th>
              <th scope="col" className={thClass}>Booking</th>
              <th scope="col" className={thClass}>Guest / Host</th>
              <th scope="col" className={thClass}>Assigned</th>
              <th scope="col" className={thClass}>Status</th>
            </tr>
          </thead>
          <tbody>
            {loading && !data ? (
              <tr><td colSpan={6} className={`${tdClass} text-[var(--text-subtle)]`}>Loading disputes...</td></tr>
            ) : items.length === 0 ? (
              <tr><td colSpan={6} className={`${tdClass} text-[var(--text-subtle)]`}>No disputes match this filter</td></tr>
            ) : (
              items.map((item) => (
                <tr key={item.ticketId} className={`${rowClass} relative cursor-pointer hover:bg-background`}>
                  <td className={`${tdClass} text-[var(--text-subtle)]`}>
                    <Link href={`/agent/disputes/${item.ticketId}`} className="after:absolute after:inset-0">{item.ticketLabel}</Link>
                  </td>
                  <td className={`${tdClass} font-bold`}>{item.title}</td>
                  <td className={`${tdClass} text-[var(--text-muted)]`}>{item.listingTitle}</td>
                  <td className={`${tdClass} text-[var(--text-muted)]`}>{item.guestName} / {item.hostName}</td>
                  <td className={`${tdClass} text-[var(--text-muted)]`}>{item.assignedAgentName ?? '—'}</td>
                  <td className={tdClass}><Pill tone={STATUS[item.status].tone}>{STATUS[item.status].label}</Pill></td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
