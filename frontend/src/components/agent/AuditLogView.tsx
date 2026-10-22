'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { isSessionFailure } from '@/lib/api';
import { AUDIT_PAGE_SIZE, fetchAudit, fetchAuditActions, type AuditQuery } from '@/lib/agent-api/audit';
import { formatDateTime, formatSgd } from '@/lib/agent-api/format';
import type { AuditEntry } from '@/lib/agent-api/types';
import { ErrorBanner, Pill, outlineButton, pageClass, primaryButton, rowClass, tableCardClass, tdClass, thClass, theadRowClass, titleClass, type Tone } from './agentUi';
import { SessionNotice } from './SessionNotice';

function toneFor(action: string): Tone {
  if (/OVERRIDE|RESOLVED|ASSIGNED/.test(action)) return 'accent';
  if (/REFUND|PAYOUT|TOP_UP|CONFIRMED|REACTIVATED|CREATED/.test(action)) return 'success';
  if (/SUSPEND|DEACTIVATED|WITHDRAW|CASCADE/.test(action)) return 'warning';
  if (/CANCEL|REJECT|DELETED|FORCE/.test(action)) return 'danger';
  return 'neutral';
}

const EMPTY: AuditQuery = { text: '', action: '', from: '', to: '' };

function target(entry: AuditEntry): string {
  return entry.subjectName ?? `${entry.entityType} ${entry.entityId.slice(0, 8)}`;
}

function status(entry: AuditEntry): string {
  if (entry.beforeState && entry.afterState) return `${entry.beforeState} -> ${entry.afterState}`;
  return entry.afterState ?? entry.beforeState ?? '—';
}

function amount(entry: AuditEntry): string {
  if (entry.walletAdjustment === null) return '—';
  return `${entry.walletAdjustment > 0 ? '+' : ''}${formatSgd(entry.walletAdjustment)}`;
}

export function AuditLogView() {
  const [draft, setDraft] = useState<AuditQuery>(EMPTY);
  const [applied, setApplied] = useState<AuditQuery>(EMPTY);
  const [rows, setRows] = useState<AuditEntry[]>([]);
  const [lastPage, setLastPage] = useState(0);
  const [actions, setActions] = useState<string[]>([]);
  const [actionsFailed, setActionsFailed] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(undefined);
  const request = useRef(0);

  const load = useCallback(async (query: AuditQuery, offset: number) => {
    const mine = ++request.current;
    setLoading(true);
    setError(undefined);
    try {
      const page = await fetchAudit(query, offset);
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

  useEffect(() => {
    void load(EMPTY, 0);
    loadActions();
  }, [load, loadActions]);

  if (error !== undefined && isSessionFailure(error)) return <SessionNotice error={error} />;

  const badRange = draft.from !== '' && draft.to !== '' && draft.from > draft.to;

  function apply() {
    if (badRange) return;
    setApplied(draft);
    void load(draft, 0);
  }

  function clear() {
    setDraft(EMPTY);
    setApplied(EMPTY);
    void load(EMPTY, 0);
  }

  const field = 'box-border w-full rounded-lg border border-border bg-background px-2.5 py-2 text-xs';
  const label = 'flex flex-col text-[10px] font-bold uppercase text-[var(--text-muted)]';
  return (
    <section className={pageClass}>
      <h1 className={titleClass}>Audit log</h1>
      <div className="flex flex-wrap items-end gap-3 rounded-xl border border-border bg-card px-4 py-3.5">
        <label className={`${label} w-[280px]`}>
          <span className="mb-1">Search</span>
          <input className={field} placeholder="User name, or booking / ticket / user id" maxLength={100} value={draft.text} onChange={(event) => setDraft({ ...draft, text: event.target.value })} />
        </label>
        <label className={`${label} w-[200px]`}>
          <span className="mb-1">Action type</span>
          <select className={field} value={draft.action} onChange={(event) => setDraft({ ...draft, action: event.target.value })}>
            <option value="">All action types</option>
            {actions.map((name) => <option key={name} value={name}>{name}</option>)}
          </select>
          {actionsFailed && (
            <span className="mt-1 normal-case text-destructive">
              Could not load action types <button type="button" className="underline" onClick={loadActions}>Retry</button>
            </span>
          )}
        </label>
        <label className={`${label} w-[150px]`}>
          <span className="mb-1">From</span>
          <input type="date" className={field} value={draft.from} onChange={(event) => setDraft({ ...draft, from: event.target.value })} />
        </label>
        <label className={`${label} w-[150px]`}>
          <span className="mb-1">To</span>
          <input type="date" className={field} value={draft.to} onChange={(event) => setDraft({ ...draft, to: event.target.value })} />
        </label>
        <button type="button" disabled={badRange} onClick={apply} className={`${primaryButton} px-[18px] py-[9px] text-xs`}>
          Apply filters
        </button>
        <button type="button" onClick={clear} className="rounded-lg border border-[#f07878] bg-transparent px-[18px] py-[9px] text-xs font-semibold text-destructive">
          Clear
        </button>
        {badRange && <p role="alert" className="m-0 w-full text-xs text-destructive">From must not be after To</p>}
      </div>
      {error !== undefined && (
        <ErrorBanner message={error instanceof Error && error.message ? error.message : 'Could not load the audit log'} onRetry={() => void load(applied, 0)} />
      )}
      <div className={tableCardClass}>
        <table className="w-full table-fixed border-collapse text-left">
          <colgroup>
            <col style={{ width: '13%' }} /><col style={{ width: '11%' }} /><col style={{ width: '15%' }} /><col style={{ width: '13%' }} />
            <col style={{ width: '14%' }} /><col style={{ width: '22%' }} /><col style={{ width: '12%' }} />
          </colgroup>
          <thead>
            <tr className={theadRowClass}>
              {['Timestamp', 'Actor', 'Action type', 'Target', 'Status', 'Reason', 'Amount'].map((name) => (
                <th key={name} scope="col" className={thClass}>{name}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading && rows.length === 0 ? (
              <tr><td colSpan={7} className={`${tdClass} text-[var(--text-subtle)]`}>Loading audit log...</td></tr>
            ) : rows.length === 0 ? (
              <tr><td colSpan={7} className={`${tdClass} text-[var(--text-subtle)]`}>No audit entries match</td></tr>
            ) : (
              rows.map((entry) => (
                <tr key={entry.id} className={`${rowClass} align-top text-xs`}>
                  <td className="px-[18px] py-3 text-[var(--text-subtle)]">{formatDateTime(entry.timestamp)}</td>
                  <td className="px-[18px] py-3 font-bold">{entry.actorName ?? '—'}</td>
                  <td className="px-[18px] py-3"><Pill tone={toneFor(entry.actionType)} className="px-[9px] py-0.5">{entry.actionType}</Pill></td>
                  <td className="px-[18px] py-3 text-[var(--text-muted)]">{target(entry)}</td>
                  <td className="px-[18px] py-3 text-[var(--text-muted)]">{status(entry)}</td>
                  <td className="px-[18px] py-3 text-[var(--text-muted)]">{entry.reason ?? '—'}</td>
                  <td className="px-[18px] py-3 font-bold">{amount(entry)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      {lastPage === AUDIT_PAGE_SIZE && (
        <button type="button" disabled={loading} onClick={() => void load(applied, rows.length)} className={`${outlineButton} self-start px-[18px] py-[9px] text-xs`}>
          Load more
        </button>
      )}
    </section>
  );
}
