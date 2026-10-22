'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { BackendApiError, isSessionFailure } from '@/lib/api';
import { assignDispute, fetchDispute, saveNotes } from '@/lib/agent-api/disputes';
import { formatSgd } from '@/lib/agent-api/format';
import type { DisputeDetail as Detail, TicketStatus } from '@/lib/agent-api/types';
import { useAuth } from '@/providers/AuthProvider';
import { ResolutionDialog, type ResolutionKind } from './ResolutionDialogs';
import { SessionNotice } from './SessionNotice';
import { ThreadPanel } from './ThreadPanel';
import { usePolling } from './usePolling';

const STATUS_LABEL: Record<TicketStatus, string> = {
  OPEN: 'Unassigned',
  IN_REVIEW: 'In review',
  RESOLVED_APPROVED: 'Resolved - approved',
  RESOLVED_REJECTED: 'Resolved - rejected',
};

function shortDate(date: string): string {
  return new Intl.DateTimeFormat('en-US', { day: 'numeric', month: 'short', timeZone: 'UTC' }).format(new Date(`${date}T00:00:00Z`));
}

function isResolved(status: TicketStatus): boolean {
  return status === 'RESOLVED_APPROVED' || status === 'RESOLVED_REJECTED';
}

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
      <section className="mx-auto max-w-6xl p-6">
        {loading && !error && <p>Loading dispute...</p>}
        {error !== undefined && (
          <div role="alert" className="flex items-center justify-between rounded-md border border-red-300 bg-red-50 p-3 text-sm text-red-800">
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

  const button = 'rounded-md px-3 py-2 text-xs font-bold disabled:opacity-50';
  return (
    <section className="mx-auto max-w-6xl p-6">
      {error !== undefined && (
        <div role="alert" className="mb-3 flex items-center justify-between rounded-md border border-red-300 bg-red-50 p-3 text-sm text-red-800">
          <span>Could not refresh this dispute. What you see may be out of date.</span>
          <button type="button" onClick={reload}>Retry</button>
        </div>
      )}
      <nav aria-label="Breadcrumb" className="mb-3 text-xs">
        <Link href="/agent/disputes" className="underline">Disputes</Link> &gt; {detail.ticketLabel} {detail.ticket.title}
        <span className="ml-3 rounded-full border border-[var(--border)] px-2 py-0.5">{STATUS_LABEL[status]}</span>
      </nav>

      <div className="mb-4 grid grid-cols-2 gap-3 rounded-md border border-[var(--border)] bg-card p-4 text-sm md:grid-cols-3">
        <p><span className="block text-xs text-muted-foreground">Listing</span>{detail.listingTitle}</p>
        <p><span className="block text-xs text-muted-foreground">Stay</span>{shortDate(detail.startDate)} - {shortDate(detail.endDate)}</p>
        <p><span className="block text-xs text-muted-foreground">Guest</span>{detail.guestName}</p>
        <p><span className="block text-xs text-muted-foreground">Host</span>{detail.hostName}</p>
        <p><span className="block text-xs text-muted-foreground">Amount held in escrow</span>{detail.escrowHeld ? formatSgd(detail.escrowAmount) : 'Released'}</p>
        <p><span className="block text-xs text-muted-foreground">Booking status</span>{detail.phaseLabel}</p>
      </div>

      {resolved && (
        <div role="status" className="mb-4 rounded-md border border-[var(--border)] bg-card p-3 text-sm">
          <strong>{STATUS_LABEL[status]}</strong>
          {detail.ticket.resolutionReason && <p>{detail.ticket.resolutionReason}</p>}
        </div>
      )}

      <div className="mb-4 grid gap-4 md:grid-cols-2">
        <ThreadPanel detail={detail} channel="GUEST" canSend={!unassigned && !resolved} resolved={resolved} />
        <ThreadPanel detail={detail} channel="HOST" canSend={!unassigned && !resolved} resolved={resolved} />
      </div>

      <label className="mb-1 block text-xs font-semibold" htmlFor="agent-notes">Internal notes (invisible to guest or host)</label>
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
        className="mb-3 min-h-24 w-full rounded-md border border-[var(--border)] p-2 text-sm disabled:opacity-50"
      />

      {actionError !== undefined && (
        <p role="alert" className="mb-2 text-sm text-red-700">{actionError instanceof Error && actionError.message ? actionError.message : 'Action failed'}</p>
      )}

      {!resolved && (
        <div className="flex flex-wrap items-center gap-2">
          {unassigned && (
            <button type="button" disabled={busy} className={`${button} bg-primary text-primary-foreground`} onClick={() => void act(() => assignDispute(ticketId))}>
              Assign to me
            </button>
          )}
          <button type="button" disabled={!mine || busy || !dirty} className={`${button} border border-[var(--border)]`} onClick={() => void onSaveNotes()}>
            Save notes
          </button>
          <button type="button" disabled={!canResolve} className={`${button} bg-green-700 text-white`} onClick={() => setDialog('ACCEPT')}>
            Accept - remedy guest
          </button>
          <button type="button" disabled={!canResolve} className={`${button} bg-red-700 text-white`} onClick={() => setDialog('REJECT')}>
            Reject dispute
          </button>
          <button type="button" disabled={!canResolve} className={`${button} border border-[var(--border)]`} onClick={() => setDialog('MANUAL')}>
            Manual adjustment...
          </button>
        </div>
      )}
      {unassigned && <p className="mt-2 text-xs text-muted-foreground">Assign this ticket to reply or resolve.</p>}
      {status === 'IN_REVIEW' && !mine && <p className="mt-2 text-xs text-muted-foreground">Assigned to {detail.assignedAgentName}.</p>}
      {mine && !detail.escrowHeld && <p className="mt-2 text-xs text-muted-foreground">Escrow already released.</p>}

      {dialog && (
        <ResolutionDialog kind={dialog} detail={detail} onClose={() => setDialog(undefined)} onResolved={resolvedBy} onFailed={failedResolve} />
      )}
    </section>
  );
}
