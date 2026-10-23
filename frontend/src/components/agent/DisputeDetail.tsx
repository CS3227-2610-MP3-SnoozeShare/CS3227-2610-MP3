'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { BackendApiError, isSessionFailure } from '@/lib/api';
import { assignDispute, fetchDispute, saveNotes, unassignDispute } from '@/lib/agent-api/disputes';
import { formatDateTime, formatSgd } from '@/lib/agent-api/format';
import type { Assistant, DisputeDetail as Detail, TicketStatus } from '@/lib/agent-api/types';
import { useAuth } from '@/providers/AuthProvider';
import { ErrorBanner, Pill, dangerOutlineButton, outlineButton, pageClass, primaryButton } from './agentUi';
import { STATUS } from './DisputeQueue';
import { ResolutionDialog, type ResolutionKind } from './ResolutionDialogs';
import { SessionNotice } from './SessionNotice';
import { ResizeGrip } from './ResizeGrip';
import { ThreadPanel } from './ThreadPanel';
import { usePolling } from './usePolling';

function shortDate(date: string): string {
  return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', timeZone: 'UTC' }).format(new Date(`${date}T00:00:00Z`));
}

const NOTES_MIN_HEIGHT = 56;
const NOTES_MAX_HEIGHT = 400;

function isResolved(status: TicketStatus): boolean {
  return status === 'RESOLVED_APPROVED' || status === 'RESOLVED_REJECTED';
}

const caption = 'text-[11px] uppercase text-[var(--text-subtle)]';

function assistantStatus(assistant: Assistant, resolved: boolean): string {
  if (resolved && (assistant.state === 'PENDING' || assistant.state === 'WAITING_GUEST')) return 'The assistant stopped.';
  switch (assistant.state) {
    case 'PENDING': return assistant.stale
      ? 'The assistant did not finish. Continue with this ticket manually.'
      : "The assistant is working on the Guest's latest message.";
    case 'WAITING_GUEST': return `Waiting for the Guest's answer (round ${assistant.rounds} of ${assistant.maxRounds}).`;
    case 'DONE': return `The assistant has finished (${assistant.rounds} question ${assistant.rounds === 1 ? 'round' : 'rounds'}).`;
    case 'STOPPED': return 'The assistant stopped.';
    case 'UNAVAILABLE': return 'The assistant was unavailable for this ticket.';
  }
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
  const [chatHeight, setChatHeight] = useState(460);
  const [notesHeight, setNotesHeight] = useState(60);

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
  const inReview = status === 'IN_REVIEW';
  const canToggleAssignment = unassigned || (inReview && mine);

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
      <nav aria-label="Breadcrumb" className="flex flex-wrap items-center gap-2 text-xs">
        <Link href="/agent/disputes" className="text-[var(--text-subtle)]">Disputes</Link>
        <span className="text-[var(--text-subtle)]">›</span>
        <span className="font-bold">{detail.ticketLabel} {detail.ticket.title}</span>
        <Pill tone={pill.tone} className="ml-1.5">{pill.label}</Pill>
        <button
          type="button"
          disabled={busy || !canToggleAssignment}
          className={`${primaryButton} ml-auto px-4 py-2 text-xs`}
          onClick={() => void act(() => (inReview ? unassignDispute(ticketId) : assignDispute(ticketId)))}
        >
          {inReview && mine ? 'Unassign' : 'Assign to me'}
        </button>
      </nav>

      <div className="flex flex-wrap items-center gap-x-7 gap-y-3 rounded-xl border border-border bg-card px-5 py-4 shadow-[0_4px_14px_rgba(16,6,4,0.08)]">
        <div aria-hidden className="size-14 shrink-0 rounded-lg bg-[linear-gradient(135deg,#e06830,#c0a080)]" />
        <div>
          <div className="text-sm font-bold">{detail.listingTitle}</div>
          <div className="text-xs text-[var(--text-subtle)]">
            Booking #{detail.ticket.bookingId.slice(0, 8).toUpperCase()} · {shortDate(detail.startDate)} – {shortDate(detail.endDate)}
          </div>
        </div>
        <div><div className={caption}>Guest</div><div className="text-[13px] font-bold">{detail.guestName}</div></div>
        <div><div className={caption}>Host</div><div className="text-[13px] font-bold">{detail.hostName}</div></div>
        <div>
          <div className={caption}>Amount held in escrow</div>
          <div className="text-[13px] font-extrabold text-primary">{detail.escrowHeld ? formatSgd(detail.escrowAmount) : 'Settled'}</div>
        </div>
        <div><div className={caption}>Booking status</div><Pill tone="warning">{detail.phaseLabel}</Pill></div>
      </div>

      {resolved && (
        <div role="status" className="rounded-xl border border-border bg-card px-5 py-3 text-[13px]">
          <strong>{status === 'RESOLVED_APPROVED' ? 'Resolved (approved)' : 'Resolved (rejected)'}</strong>
          {detail.ticket.resolutionReason && <p className="m-0 mt-1 text-[var(--text-muted)]">{detail.ticket.resolutionReason}</p>}
        </div>
      )}

      <div className="flex flex-wrap gap-4">
        <ThreadPanel detail={detail} channel="GUEST" canSend={!unassigned && !resolved} resolved={resolved} height={chatHeight} onResize={setChatHeight} />
        <ThreadPanel detail={detail} channel="HOST" canSend={!unassigned && !resolved} resolved={resolved} height={chatHeight} onResize={setChatHeight} />
      </div>

      {detail.assistant && (
        <section aria-label="AI assistant draft" className="rounded-xl border border-border bg-card p-4 text-xs">
          <h2 className="m-0 mb-2 text-sm font-bold">AI assistant</h2>
          {detail.assistant.flagged && (
            <p className="m-0 mb-2 text-[var(--warning)]">Warning: text in this ticket matched a prompt-injection pattern. Treat the draft with care.</p>
          )}
          <p className="m-0 mb-3 text-[var(--text-muted)]">{assistantStatus(detail.assistant, resolved)}</p>
          {detail.assistant.draft === null ? <p className="m-0 text-[var(--text-subtle)]">No draft yet.</p> : (
            <>
              <div className="rounded-lg border border-border p-3">
                <h3 className="m-0 mb-2 text-xs font-bold">AI-assisted draft (unverified, updated {detail.assistant.draftUpdatedAt ? formatDateTime(detail.assistant.draftUpdatedAt) : '—'})</h3>
                <div className="whitespace-pre-wrap break-words">{detail.assistant.draft}</div>
              </div>
              <div className="mt-3 flex justify-end">
                <button
                  type="button"
                  disabled={!mine || busy}
                  title={!mine || busy ? 'Assign the ticket to yourself to edit notes.' : undefined}
                  className={`${outlineButton} rounded-lg px-3 py-2 text-xs disabled:opacity-50`}
                  onClick={() => {
                    const draft = detail.assistant?.draft;
                    if (draft === null || draft === undefined || !mine || busy) return;
                    setNotes((current) => current === '' ? draft : `${current}\n\n${draft}`);
                    setDirty(true);
                  }}
                >
                  Insert into notes
                </button>
              </div>
            </>
          )}
        </section>
      )}

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
          style={{ height: notesHeight }}
          className="agent-scroll box-border block w-full resize-none rounded-lg border border-border bg-background px-3 py-2.5 text-xs disabled:opacity-60"
        />
        <ResizeGrip height={notesHeight} min={NOTES_MIN_HEIGHT} max={NOTES_MAX_HEIGHT} onChange={setNotesHeight} label="Resize notes" />
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
        <div className="flex flex-wrap items-center gap-2.5">
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
