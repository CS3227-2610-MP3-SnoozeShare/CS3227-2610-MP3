'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { BackendApiError, isSessionFailure } from '@/lib/api';
import { assignDispute, fetchDispute, saveNotes } from '@/lib/agent-api/disputes';
import { formatSgd, shortName } from '@/lib/agent-api/format';
import type { DisputeDetail as Detail, TicketStatus } from '@/lib/agent-api/types';
import { useAuth } from '@/providers/AuthProvider';
import { ErrorBanner, Pill, dangerOutlineButton, outlineButton, pageClass, primaryButton } from './agentUi';
import { STATUS } from './DisputeQueue';
import { ResolutionDialog, type ResolutionKind } from './ResolutionDialogs';
import { SessionNotice } from './SessionNotice';
import { ThreadPanel } from './ThreadPanel';
import { usePolling } from './usePolling';

function shortDate(date: string): string {
  return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', timeZone: 'UTC' }).format(new Date(`${date}T00:00:00Z`));
}

function isResolved(status: TicketStatus): boolean {
  return status === 'RESOLVED_APPROVED' || status === 'RESOLVED_REJECTED';
}

const caption = 'text-[11px] uppercase text-[var(--text-subtle)]';

export function DisputeDetail({ ticketId }: { ticketId: string }) {
  const { profile } = useAuth();
  const { data, error, loading, reload } = usePolling(() => fetchDispute(ticketId), 10000);
  const [fresh, setFresh] = useState<Detail | undefined>(undefined);
  const [notes, setNotes] = useState('');
  const [dirty, setDirty] = useState(false);
  const [dialog, setDialog] = useState<ResolutionKind | undefined>(undefined);
  const [actionError, setActionError] = useState<unknown>(undefined);
  const [busy, setBusy] = useState(false);

  // An action response or a failed-resolve refetch wins until the reload that follows it lands (it clears `fresh`).
  const detail = fresh ?? data;
  const serverNotes = detail?.agentNotes ?? '';

  useEffect(() => {
    setFresh(undefined);
  }, [data]);

  useEffect(() => {
    if (!dirty) setNotes(serverNotes);
  }, [serverNotes, dirty]);

  const failure = [error, actionError].find((candidate) => candidate !== undefined && isSessionFailure(candidate));
  if (failure) return <SessionNotice error={failure} />;

  if (!detail) {
    const notFound = error instanceof BackendApiError && error.status === 404;
    return (
      <section className={pageClass}>
        {loading && !error && <p className="m-0 text-[13px] text-[var(--text-subtle)]">Loading dispute...</p>}
        {error !== undefined && (
          <div role="alert" className="flex items-center justify-between rounded-lg border border-[#f07878] bg-[var(--danger-subtle)] px-4 py-2.5 text-[13px] text-destructive">
            <span>{notFound ? 'Ticket does not exist' : error instanceof Error && error.message ? error.message : 'Could not load dispute'}</span>
            <span className="flex gap-3">
              <Link href="/agent/disputes" className="underline">Back to Disputes</Link>
              <button type="button" onClick={reload}>Retry</button>
            </span>
          </div>
        )}
      </section>
    );
  }

  const status = detail.ticket.status;
  const resolved = isResolved(status);
  const unassigned = status === 'OPEN';
  const mine = status === 'IN_REVIEW' && detail.ticket.assignedAgentId === profile?.id;
  const canResolve = mine && detail.escrowHeld;
  const hostRaised = detail.ticket.raisedByRole === 'HOST';

  async function act(run: () => Promise<Detail>): Promise<boolean> {
    if (busy) return false;
    setBusy(true);
    setActionError(undefined);
    try {
      setFresh(await run());
      reload();
      return true;
    } catch (caught) {
      setActionError(caught);
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function onSaveNotes() {
    if (await act(() => saveNotes(ticketId, notes))) setDirty(false);
  }

  function resolvedBy(updated: Detail) {
    setFresh(updated);
    setDialog(undefined);
    setDirty(false);
    reload();
  }

  function failedResolve(caught: unknown) {
    if (isSessionFailure(caught)) {
      setActionError(caught);
      return;
    }
    fetchDispute(ticketId).then(
      (latest) => {
        setFresh(latest);
        reload();
        if (isResolved(latest.ticket.status)) setDialog(undefined);
      },
      () => undefined,
    );
  }

  const actionClass = 'rounded-lg px-5 py-2.5 text-[13px] disabled:opacity-50';
  const pill = STATUS[status];
  return (
    <section className={`${pageClass} pt-[18px] pb-7`}>
      {error !== undefined && (
        <ErrorBanner message="Could not refresh this dispute. What you see may be out of date." onRetry={reload} />
      )}
      <nav aria-label="Breadcrumb" className="flex items-center gap-2 text-xs">
        <Link href="/agent/disputes" className="text-[var(--text-subtle)]">Disputes</Link>
        <span className="text-[var(--text-subtle)]">›</span>
        <span className="font-bold">{detail.ticketLabel} {detail.ticket.title}</span>
        <Pill tone={pill.tone} className="ml-1.5">{pill.label}</Pill>
        {unassigned && (
          <button type="button" disabled={busy} className={`${primaryButton} ml-auto px-4 py-2 text-xs`} onClick={() => void act(() => assignDispute(ticketId))}>
            Assign to me
          </button>
        )}
      </nav>

      <div className="flex items-center gap-7 rounded-xl border border-border bg-card px-5 py-4 shadow-[0_4px_14px_rgba(16,6,4,0.08)]">
        <div aria-hidden className="size-14 shrink-0 rounded-lg bg-[linear-gradient(135deg,#e06830,#c0a080)]" />
        <div>
          <div className="text-sm font-bold">{detail.listingTitle}</div>
          <div className="text-xs text-[var(--text-subtle)]">{shortDate(detail.startDate)} – {shortDate(detail.endDate)}</div>
        </div>
        <div><div className={caption}>Guest</div><div className="text-[13px] font-bold">{shortName(detail.guestName)}</div></div>
        <div><div className={caption}>Host</div><div className="text-[13px] font-bold">{shortName(detail.hostName)}</div></div>
        <div>
          <div className={caption}>Amount held in escrow</div>
          <div className="text-[13px] font-extrabold text-primary">{detail.escrowHeld ? formatSgd(detail.escrowAmount) : 'Released'}</div>
        </div>
        <div><div className={caption}>Booking status</div><Pill tone="warning">{detail.phaseLabel}</Pill></div>
      </div>

      {resolved && (
        <div role="status" className="rounded-xl border border-border bg-card px-5 py-3 text-[13px]">
          <strong>{pill.label}</strong>
          {detail.ticket.resolutionReason && <p className="m-0 mt-1 text-[var(--text-muted)]">{detail.ticket.resolutionReason}</p>}
        </div>
      )}

      <div className="flex gap-4">
        <ThreadPanel detail={detail} channel="GUEST" canSend={!unassigned && !resolved} resolved={resolved} />
        <ThreadPanel detail={detail} channel="HOST" canSend={!unassigned && !resolved} resolved={resolved} />
      </div>

      <div>
        <label className="mb-1.5 block text-xs font-bold text-[var(--text-muted)]" htmlFor="agent-notes">Internal notes (invisible to guest or host)</label>
        <textarea
          id="agent-notes"
          placeholder="Add investigation notes..."
          value={notes}
          maxLength={4000}
          disabled={!mine}
          readOnly={resolved}
          onChange={(event) => {
            setNotes(event.target.value);
            setDirty(true);
          }}
          className="box-border h-14 w-full resize-none rounded-lg border border-border bg-background px-3 py-2.5 text-xs disabled:opacity-60"
        />
        {!resolved && (
          <div className="mt-1.5 flex justify-end">
            <button type="button" disabled={!mine || busy || !dirty} className={`${primaryButton} px-3.5 py-2 text-[11px]`} onClick={() => void onSaveNotes()}>
              Save notes
            </button>
          </div>
        )}
      </div>

      {actionError !== undefined && (
        <p role="alert" className="m-0 text-[13px] text-destructive">{actionError instanceof Error && actionError.message ? actionError.message : 'Action failed'}</p>
      )}

      {!resolved && (
        <div className="flex items-center gap-2.5">
          <button type="button" disabled={!canResolve} className={`${actionClass} bg-[var(--success)] font-bold text-[#fdf8f0]`} onClick={() => setDialog('ACCEPT')}>
            Accept — remedy {hostRaised ? 'host' : 'guest'}
          </button>
          <button type="button" disabled={!canResolve} className={`${actionClass} ${dangerOutlineButton}`} onClick={() => setDialog('REJECT')}>
            Reject dispute
          </button>
          <button type="button" disabled={!canResolve} className={`${actionClass} ${outlineButton} font-bold`} onClick={() => setDialog('MANUAL')}>
            Manual adjustment…
          </button>
        </div>
      )}
      {unassigned && <p className="m-0 text-xs text-[var(--text-subtle)]">Assign this ticket to reply or resolve.</p>}
      {status === 'IN_REVIEW' && !mine && <p className="m-0 text-xs text-[var(--text-subtle)]">Assigned to {detail.assignedAgentName}.</p>}
      {mine && !detail.escrowHeld && <p className="m-0 text-xs text-[var(--text-subtle)]">Escrow already released.</p>}

      {dialog && (
        <ResolutionDialog kind={dialog} detail={detail} onClose={() => setDialog(undefined)} onResolved={resolvedBy} onFailed={failedResolve} />
      )}
    </section>
  );
}
