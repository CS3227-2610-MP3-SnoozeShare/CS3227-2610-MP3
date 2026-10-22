'use client';

import { useState } from 'react';
import { isSessionFailure } from '@/lib/api';
import { fetchThread, postMessage } from '@/lib/agent-api/disputes';
import { formatDateTime, shortName } from '@/lib/agent-api/format';
import type { DisputeDetail, ThreadChannel } from '@/lib/agent-api/types';
import { primaryButton } from './agentUi';
import { SessionNotice } from './SessionNotice';
import { usePolling } from './usePolling';

type Props = {
  detail: DisputeDetail;
  channel: ThreadChannel;
  canSend: boolean;
  resolved: boolean;
};

const REMEDY: Record<string, string> = {
  FULL_REFUND: 'Full refund',
  PARTIAL_REFUND: 'Partial refund',
  HOST_PAYOUT: 'Full payout to host',
  OTHER: 'Other remedy',
};

const bubble = 'max-w-[85%] rounded-lg px-3 py-[9px]';
const stamp = 'mt-[3px] text-[9px] text-[var(--text-subtle)]';

export function ThreadPanel({ detail, channel, canSend, resolved }: Props) {
  const ticketId = detail.ticket.id;
  const { data, error, reload } = usePolling(() => fetchThread(ticketId, channel), 10000);
  const [body, setBody] = useState('');
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<unknown>(undefined);
  const isGuest = channel === 'GUEST';
  const name = isGuest ? detail.guestName : detail.hostName;
  const placeholder = `Reply to ${isGuest ? 'guest' : 'host'}...`;

  if (isSessionFailure(error) || isSessionFailure(sendError)) return <SessionNotice error={error ?? sendError} />;

  async function send() {
    const text = body.trim();
    if (!text || sending) return;
    setSending(true);
    setSendError(undefined);
    try {
      await postMessage(ticketId, channel, text);
      setBody('');
      reload();
    } catch (caught) {
      setSendError(caught);
    } finally {
      setSending(false);
    }
  }

  const raisedHere = detail.ticket.raisedByRole === (isGuest ? 'GUEST' : 'HOST');
  return (
    <section
      aria-label={`${isGuest ? 'Guest' : 'Host'} messages`}
      className="flex h-[460px] flex-1 flex-col overflow-hidden rounded-xl border border-border bg-card"
    >
      <h2 className="m-0 flex items-center gap-2 bg-muted px-4 py-2.5 text-xs font-bold uppercase text-[var(--text-muted)]">
        <span>{isGuest ? 'Guest' : 'Host'} messages · {shortName(name)}</span>
        {raisedHere && <span className="rounded-full bg-primary px-2 py-0.5 text-[9px] font-bold text-primary-foreground">Opened ticket</span>}
      </h2>
      <div className="flex flex-1 flex-col gap-2.5 overflow-y-auto px-4 py-3.5">
        {raisedHere && (
          <>
            <article className={`${bubble} self-start bg-muted`}>
              <div className="text-xs">{detail.ticket.description}</div>
              {detail.ticket.supportingText && <div className="mt-1 text-xs">{detail.ticket.supportingText}</div>}
              <div className={stamp}>{formatDateTime(detail.ticket.createdAt)}</div>
            </article>
            <article className={`${bubble} self-end bg-accent`}>
              <div className="text-xs font-bold text-primary">Ticket {detail.ticketLabel} opened</div>
              <div className="text-xs">Requested remedy: {REMEDY[detail.ticket.requestedRemedy] ?? detail.ticket.requestedRemedy}</div>
              <div className={stamp}>{formatDateTime(detail.ticket.createdAt)}</div>
            </article>
          </>
        )}
        {error !== undefined && <p role="alert" className="text-xs text-destructive">Could not load messages</p>}
        {(data ?? []).map((message) => (
          <article
            key={message.id}
            className={`${bubble} ${message.authorRole === 'AGENT' ? 'self-end bg-accent' : 'self-start bg-muted'}`}
          >
            <div className="text-xs">{message.body}</div>
            <div className={stamp}>{formatDateTime(message.sentAt)}</div>
          </article>
        ))}
      </div>
      <div className="border-t border-[var(--border-muted)] px-3 py-2.5">
        {resolved ? (
          <p className="m-0 text-xs text-[var(--text-subtle)]">Ticket is resolved</p>
        ) : (
          <div className="flex gap-2">
            <input
              aria-label={placeholder}
              placeholder={placeholder}
              value={body}
              maxLength={2000}
              disabled={!canSend}
              onChange={(event) => setBody(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') void send();
              }}
              className="box-border flex-1 rounded-lg border border-border bg-background px-2.5 py-2 text-xs disabled:opacity-50"
            />
            <button
              type="button"
              disabled={!canSend || sending || body.trim() === ''}
              onClick={() => void send()}
              className={`${primaryButton} px-3.5 py-2 text-[11px]`}
            >
              Send
            </button>
          </div>
        )}
        {sendError !== undefined && (
          <p role="alert" className="mb-0 mt-1 text-xs text-destructive">
            {sendError instanceof Error ? sendError.message : 'Could not send'}
          </p>
        )}
      </div>
    </section>
  );
}
