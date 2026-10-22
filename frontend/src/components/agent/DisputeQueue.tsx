'use client';

import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';
import { isSessionFailure } from '@/lib/api';
import { fetchQueue } from '@/lib/agent-api/disputes';
import type { AssigneeFilter, TicketStatus } from '@/lib/agent-api/types';
import { AgentGrid, Cell, GridMessage, GridRow, type GridColumn } from './AgentGrid';
import { ErrorBanner, Pill, pageClass, titleClass, type Tone } from './agentUi';
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
  RESOLVED_APPROVED: { label: 'Resolved', tone: 'success' },
  RESOLVED_REJECTED: { label: 'Rejected', tone: 'danger' },
};

/** The status drop-down of the desktop queue. */
const STATUS_OPTIONS: { value: TicketStatus | ''; label: string }[] = [
  { value: '', label: 'All statuses' },
  { value: 'OPEN', label: 'Open' },
  { value: 'IN_REVIEW', label: 'In review' },
  { value: 'RESOLVED_APPROVED', label: 'Approved' },
  { value: 'RESOLVED_REJECTED', label: 'Rejected' },
];

const COLUMNS: GridColumn[] = [
  { label: 'Ticket', min: 80, fr: 0.6 },
  { label: 'Subject', min: 170, fr: 1.4 },
  { label: 'Booking', min: 130, fr: 1 },
  { label: 'Guest / Host', min: 160, fr: 1.1 },
  { label: 'Assigned', min: 110, fr: 0.9 },
  { label: 'Status', min: 140, fr: 0.9 },
];

export function DisputeQueue() {
  const [assignee, setAssignee] = useState<AssigneeFilter>('ALL');
  const [status, setStatus] = useState<TicketStatus | ''>('');
  const current = useRef({ assignee, status });
  current.current = { assignee, status };
  const { data, error, loading, reload } = usePolling(
    () => fetchQueue(current.current.assignee, current.current.status || undefined),
    30000,
  );
  const first = useRef(true);

  useEffect(() => {
    if (first.current) {
      first.current = false;
      return;
    }
    reload();
  }, [assignee, status, reload]);

  if (error && isSessionFailure(error)) return <SessionNotice error={error} />;

  const items = data?.items ?? [];
  const unassigned = data?.unassignedCount ?? 0;

  return (
    <section className={pageClass}>
      <div className="flex items-center gap-2.5">
        <h1 className={titleClass}>Dispute queue</h1>
        {unassigned > 0 && <Pill tone="danger" className="text-[11px] normal-case">{unassigned} unassigned</Pill>}
        <div className="ml-auto flex flex-wrap items-center gap-2" role="group" aria-label="Filter disputes">
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
          <select
            aria-label="Status"
            value={status}
            onChange={(event) => setStatus(event.target.value as TicketStatus | '')}
            className="h-7 rounded-lg border border-border bg-card px-2 text-xs text-[var(--text-muted)]"
          >
            {STATUS_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        </div>
      </div>
      {error !== undefined && (
        <ErrorBanner message={error instanceof Error && error.message ? error.message : 'Could not load disputes'} onRetry={reload} />
      )}
      <AgentGrid columns={COLUMNS} label="Dispute queue">
        {loading && !data ? (
          <GridMessage>Loading disputes...</GridMessage>
        ) : items.length === 0 ? (
          <GridMessage>No disputes match these filters.</GridMessage>
        ) : (
          items.map((item) => (
            <GridRow key={item.ticketId} className="relative cursor-pointer hover:bg-background">
              <Cell className="text-[var(--text-subtle)]">
                <Link href={`/agent/disputes/${item.ticketId}`} className="after:absolute after:inset-0">{item.ticketLabel}</Link>
              </Cell>
              <Cell className="font-bold">{item.title}</Cell>
              <Cell className="text-[var(--text-muted)]">{item.listingTitle}</Cell>
              <Cell className="text-[var(--text-muted)]">{item.guestName} / {item.hostName}</Cell>
              <Cell className="text-[var(--text-muted)]">{item.assignedAgentName ?? '—'}</Cell>
              <Cell><Pill tone={STATUS[item.status].tone}>{STATUS[item.status].label}</Pill></Cell>
            </GridRow>
          ))
        )}
      </AgentGrid>
    </section>
  );
}
