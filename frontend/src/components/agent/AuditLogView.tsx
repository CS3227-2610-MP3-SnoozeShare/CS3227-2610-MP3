'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { isSessionFailure } from '@/lib/api';
import { AUDIT_PAGE_SIZE, fetchAudit, fetchAuditActions, type AuditQuery } from '@/lib/agent-api/audit';
import { formatDateTime, formatSgd } from '@/lib/agent-api/format';
import type { AuditEntry } from '@/lib/agent-api/types';
import { SessionNotice } from './SessionNotice';

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

  const field = 'rounded-md border border-[var(--border)] p-2 text-sm';
  return (
    <section className="mx-auto max-w-7xl p-6">
      <h1 className="mb-4 text-xl font-extrabold">Audit log</h1>
      <div className="mb-4 flex flex-wrap items-end gap-3 rounded-md border border-[var(--border)] bg-card p-4">
        <label className="flex flex-col gap-1 text-xs font-semibold">
          Search
          <input className={`${field} w-72`} placeholder="User name, or booking / ticket / user id" maxLength={100} value={draft.text} onChange={(event) => setDraft({ ...draft, text: event.target.value })} />
        </label>
        <label className="flex flex-col gap-1 text-xs font-semibold">
          Action type
          <select className={field} value={draft.action} onChange={(event) => setDraft({ ...draft, action: event.target.value })}>
            <option value="">All action types</option>
            {actions.map((name) => <option key={name} value={name}>{name}</option>)}
          </select>
          {actionsFailed && (
            <span className="text-red-700">
              Could not load action types <button type="button" className="underline" onClick={loadActions}>Retry</button>
            </span>
          )}
        </label>
        <label className="flex flex-col gap-1 text-xs font-semibold">
          From
          <input type="date" className={field} value={draft.from} onChange={(event) => setDraft({ ...draft, from: event.target.value })} />
        </label>
        <label className="flex flex-col gap-1 text-xs font-semibold">
          To
          <input type="date" className={field} value={draft.to} onChange={(event) => setDraft({ ...draft, to: event.target.value })} />
        </label>
        <button type="button" disabled={badRange} onClick={apply} className="rounded-md bg-primary px-4 py-2 text-xs font-bold text-primary-foreground disabled:opacity-50">
          Apply filters
        </button>
        <button type="button" onClick={clear} className="rounded-md border border-[var(--border)] px-4 py-2 text-xs font-semibold">Clear</button>
        {badRange && <p role="alert" className="w-full text-xs text-red-700">From must not be after To</p>}
      </div>
      {error !== undefined && (
        <div role="alert" className="mb-3 flex items-center justify-between rounded-md border border-red-300 bg-red-50 p-3 text-sm text-red-800">
          <span>{error instanceof Error && error.message ? error.message : 'Could not load the audit log'}</span>
          <button type="button" className="rounded-md border border-red-300 px-3 py-1 text-xs font-semibold" onClick={() => void load(applied, 0)}>Retry</button>
        </div>
      )}
      <table className="w-full border-collapse text-left text-sm">
        <thead>
          <tr className="border-b border-[var(--border)] text-xs uppercase text-muted-foreground">
            {['Timestamp', 'Actor', 'Action type', 'Target', 'Status', 'Reason', 'Amount'].map((name) => (
              <th key={name} scope="col" className="p-2">{name}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {loading && rows.length === 0 ? (
            <tr><td colSpan={7} className="p-4 text-muted-foreground">Loading audit log...</td></tr>
          ) : rows.length === 0 ? (
            <tr><td colSpan={7} className="p-4 text-muted-foreground">No audit entries match</td></tr>
          ) : (
            rows.map((entry) => (
              <tr key={entry.id} className="border-b border-[var(--border)] align-top">
                <td className="p-2">{formatDateTime(entry.timestamp)}</td>
                <td className="p-2">{entry.actorName ?? '—'}</td>
                <td className="p-2"><span className="rounded-full border border-[var(--border)] px-2 py-0.5 text-xs">{entry.actionType}</span></td>
                <td className="p-2">{target(entry)}</td>
                <td className="p-2">{status(entry)}</td>
                <td className="p-2">{entry.reason ?? '—'}</td>
                <td className="p-2">{amount(entry)}</td>
              </tr>
            ))
          )}
        </tbody>
      </table>
      {lastPage === AUDIT_PAGE_SIZE && (
        <button type="button" disabled={loading} onClick={() => void load(applied, rows.length)} className="mt-4 rounded-md border border-[var(--border)] px-4 py-2 text-xs font-semibold">
          Load more
        </button>
      )}
    </section>
  );
}
