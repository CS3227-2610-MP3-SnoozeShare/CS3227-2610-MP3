'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { isSessionFailure } from '@/lib/api';
import { AUDIT_PAGE_SIZE, fetchAudit, fetchAuditActions, type AuditQuery } from '@/lib/agent-api/audit';
import type { AuditEntry } from '@/lib/agent-api/types';
import { AgentGrid, Cell, GridMessage, GridRow, type GridColumn } from './AgentGrid';
import { ErrorBanner, Pill, outlineButton, pageClass, titleClass, type Tone } from './agentUi';
import { DateField } from './DateField';
import { MultiSelectMenu } from './MultiSelectMenu';
import { SessionNotice } from './SessionNotice';

const DASH = '—';
const SEARCH_DELAY_MS = 300;
const EMPTY: AuditQuery = { text: '', actions: [], from: '', to: '' };

const pad = (value: number) => String(value).padStart(2, '0');

/** `23/10/2026 07:30`, the desktop format, in the browser's time zone. */
function timeText(iso: string): string {
  const date = new Date(iso);
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** `IN_REVIEW` becomes `In review`. */
function label(state: string | null): string | null {
  if (!state || state.trim() === '') return null;
  const spaced = state.toLowerCase().replaceAll('_', ' ');
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}

function statusText(entry: AuditEntry): string {
  const before = label(entry.beforeState);
  const after = label(entry.afterState);
  if (before && after) return `${before} → ${after}`;
  return after ?? before ?? DASH;
}

function amountText(entry: AuditEntry): string {
  if (entry.walletAdjustment === null) return DASH;
  const sign = entry.walletAdjustment < 0 ? '-' : '+';
  return `${sign}SGD ${Math.abs(entry.walletAdjustment).toFixed(2)}`;
}

/** The one thing the action was done to: the entity itself, or for a wallet movement the user whose wallet moved. */
function targetText(entry: AuditEntry): string {
  const lastFour = entry.entityId.slice(-4);
  if (entry.entityType === 'WalletTransaction' || entry.entityType === 'User') {
    return entry.subjectName ?? `User #${lastFour}`;
  }
  const noun = entry.entityType === 'TicketCategory' ? 'Category' : entry.entityType;
  return `${noun} #${lastFour}`;
}

function toneFor(action: string): Tone {
  if (['BOOKING_PAYOUT', 'PLATFORM_FEE', 'ESCROW_REFUND', 'TICKET_REMEDY', 'TOP_UP', 'BOOKING_COMPLETED'].includes(action)) return 'success';
  if (['AGENT_OVERRIDE', 'TICKET_RESOLVED', 'TICKET_OPENED', 'TICKET_ASSIGNED', 'TICKET_UNASSIGNED', 'TICKET_NOTE_SAVED'].includes(action)) return 'accent';
  if (['ACCOUNT_SUSPENDED', 'ACCOUNT_REACTIVATED', 'LISTING_STATUS_CASCADE', 'LISTING_STATUS_CHANGED', 'BOOKING_CANCELLED_BY_GUEST', 'BOOKING_CANCELLED_BY_HOST', 'ESCROW_HOLD', 'WITHDRAWAL'].includes(action)) return 'warning';
  if (['BOOKING_REJECTED', 'BOOKING_FORCE_CANCELLED', 'TICKET_CATEGORY_DELETED'].includes(action)) return 'danger';
  return 'neutral';
}

const control = 'box-border h-[34px] w-full rounded-lg border border-border bg-card px-3 text-xs font-normal normal-case text-foreground';
const fieldLabel = 'flex flex-col gap-1 text-[10px] font-bold uppercase text-[var(--text-muted)]';

const COLUMNS: GridColumn[] = [
  { label: 'Timestamp', min: 135, fr: 1.2 },
  { label: 'Actor', min: 100, fr: 1 },
  { label: 'Action type', min: 230, fr: 1.5 },
  { label: 'Target', min: 130, fr: 1.5 },
  { label: 'Status', min: 150, fr: 1.5 },
  { label: 'Reason', min: 180, fr: 2.4 },
  { label: 'Amount', min: 110, fr: 0.9 },
];

export function AuditLogView() {
  const [filters, setFilters] = useState<AuditQuery>(EMPTY);
  const [query, setQuery] = useState<AuditQuery>(EMPTY);
  const [rows, setRows] = useState<AuditEntry[]>([]);
  const [lastPage, setLastPage] = useState(0);
  const [actions, setActions] = useState<string[]>([]);
  const [actionsFailed, setActionsFailed] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(undefined);
  const request = useRef(0);

  const badRange = filters.from !== '' && filters.to !== '' && filters.from > filters.to;

  const load = useCallback(async (next: AuditQuery, offset: number) => {
    const mine = ++request.current;
    setLoading(true);
    setError(undefined);
    try {
      const page = await fetchAudit(next, offset);
      if (mine !== request.current) return;
      setRows((current) => (offset === 0 ? page : [...current, ...page]));
      setLastPage(page.length);
    } catch (caught) {
      if (mine === request.current) setError(caught);
    } finally {
      if (mine === request.current) setLoading(false);
    }
  }, []);

  const loadActions = useCallback(() => {
    setActionsFailed(false);
    fetchAuditActions().then(setActions, (caught: unknown) => {
      if (isSessionFailure(caught)) setError(caught);
      else setActionsFailed(true);
    });
  }, []);

  useEffect(loadActions, [loadActions]);

  // Live filtering: typing waits briefly, every other control applies at once. A bad date range applies nothing.
  useEffect(() => {
    if (badRange) return;
    const timer = setTimeout(() => setQuery(filters), filters.text === query.text ? 0 : SEARCH_DELAY_MS);
    return () => clearTimeout(timer);
  }, [filters, badRange, query.text]);

  useEffect(() => {
    void load(query, 0);
  }, [query, load]);

  if (error !== undefined && isSessionFailure(error)) return <SessionNotice error={error} />;

  return (
    <section className={pageClass}>
      <h1 className={titleClass}>Audit log</h1>
      <div className="flex flex-wrap items-end gap-3 rounded-xl border border-border bg-card px-4 py-3.5">
        <label className={`${fieldLabel} w-[280px] max-w-full`}>
          Search
          <input
            type="search"
            className={control}
            placeholder="User name, or booking / ticket / user id"
            maxLength={100}
            value={filters.text}
            onChange={(event) => setFilters({ ...filters, text: event.target.value })}
          />
        </label>
        <div className={`${fieldLabel} w-[200px] max-w-full`}>
          Action type
          <MultiSelectMenu
            label="Action type"
            allLabel="All action types"
            options={actions}
            selected={filters.actions}
            onChange={(selected) => setFilters({ ...filters, actions: selected })}
          />
          {actionsFailed && (
            <span className="normal-case text-destructive">
              Could not load action types <button type="button" className="underline" onClick={loadActions}>Retry</button>
            </span>
          )}
        </div>
        <div className={`${fieldLabel} w-[160px] max-w-full`}>
          From
          <DateField label="From" value={filters.from} onChange={(from) => setFilters({ ...filters, from })} />
        </div>
        <div className={`${fieldLabel} w-[160px] max-w-full`}>
          To
          <DateField label="To" value={filters.to} onChange={(to) => setFilters({ ...filters, to })} />
        </div>
        <button
          type="button"
          onClick={() => {
            setFilters(EMPTY);
            setQuery(EMPTY);
          }}
          className="h-[34px] rounded-lg border border-[#f07878] bg-transparent px-[18px] text-xs font-semibold text-destructive"
        >
          Clear
        </button>
      </div>
      {badRange && <p role="alert" className="m-0 text-[13px] text-destructive">The From date must not be after the To date.</p>}
      {error !== undefined && (
        <ErrorBanner message={error instanceof Error && error.message ? error.message : 'Could not load the audit log'} onRetry={() => void load(query, 0)} />
      )}
      <AgentGrid columns={COLUMNS} label="Audit log">
        {loading && rows.length === 0 ? (
          <GridMessage>Loading audit log...</GridMessage>
        ) : rows.length === 0 ? (
          <GridMessage>No audit entries match these filters.</GridMessage>
        ) : (
          rows.map((entry) => (
            <GridRow key={entry.id} className="items-start">
              <Cell className="whitespace-nowrap text-xs text-[var(--text-muted)]">{timeText(entry.timestamp)}</Cell>
              <Cell className="text-xs font-bold">{entry.actorName ?? 'Unknown user'}</Cell>
              <Cell className="text-xs"><Pill tone={toneFor(entry.actionType)} className="px-[9px] py-0.5">{entry.actionType}</Pill></Cell>
              <Cell className="text-xs font-bold">{targetText(entry)}</Cell>
              <Cell className="text-xs text-[var(--text-muted)]">{statusText(entry)}</Cell>
              <Cell className="text-xs text-[var(--text-muted)]">{entry.reason ?? DASH}</Cell>
              <Cell className="whitespace-nowrap text-xs font-bold">{amountText(entry)}</Cell>
            </GridRow>
          ))
        )}
      </AgentGrid>
      {lastPage === AUDIT_PAGE_SIZE && (
        <button type="button" disabled={loading} onClick={() => void load(query, rows.length)} className={`${outlineButton} self-start px-[18px] py-[9px] text-xs`}>
          Load more
        </button>
      )}
    </section>
  );
}
