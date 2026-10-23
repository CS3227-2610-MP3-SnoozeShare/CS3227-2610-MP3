'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import { isSessionFailure } from '@/lib/api';
import { fetchThread, postMessage } from '@/lib/agent-api/disputes';
import { formatDateTime } from '@/lib/agent-api/format';
import type { DisputeDetail, ThreadChannel } from '@/lib/agent-api/types';
import { authorRoleLabel } from '@/lib/message-role';
import { primaryButton } from './agentUi';
import { ResizeGrip } from './ResizeGrip';
import { SessionNotice } from './SessionNotice';
import { usePolling } from './usePolling';

export const CHAT_MIN_HEIGHT = 240;
export const CHAT_MAX_HEIGHT = 900;

type Props = {
  detail: DisputeDetail;
  channel: ThreadChannel;
  canSend: boolean;
  resolved: boolean;
  /** Both chat boxes share one height; either grip changes it (desktop behaviour). */
  height: number;
  onResize: (height: number) => void;
};

const REMEDY: Record<string, string> = {
  FULL_REFUND: 'Full refund',
  PARTIAL_REFUND: 'Partial refund',
  HOST_PAYOUT: 'Full payout to host',
  OTHER: 'Other remedy',
};

const bubble = 'max-w-[85%] break-words rounded-lg px-3 py-[9px]';
const stamp = 'mt-[3px] text-[9px] text-[var(--text-subtle)]';

function firstName(fullName: string): string {
  return fullName.trim().split(/\s+/)[0] ?? fullName;
}

export function ThreadPanel({ detail, channel, canSend, resolved, height, onResize }: Props) {
  const ticketId = detail.ticket.id;
  const { data, error, reload } = usePolling(() => fetchThread(ticketId, channel), 10000);
  const [body, setBody] = useState('');
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<unknown>(undefined);
  const scroller = useRef<HTMLDivElement>(null);
  const isGuest = channel === 'GUEST';
  const name = isGuest ? detail.guestName : detail.hostName;
  const placeholder = `Reply to ${firstName(name)}…`;

  // The API returns a thread newest first; a chat reads oldest first with the newest at the bottom.
  const messages = useMemo(
    () => [...(data ?? [])].sort((a, b) => Date.parse(a.sentAt) - Date.parse(b.sentAt) || a.id.localeCompare(b.id)),
    [data],
  );

  useEffect(() => {
    const box = scroller.current;
    if (box) box.scrollTop = box.scrollHeight;
  }, [messages.length]);

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
      style={{ height }}
      className="flex min-w-0 flex-col overflow-hidden rounded-xl border border-border bg-card"
    >
      <h2 className="m-0 flex min-h-[38px] items-center gap-2 bg-muted px-4 py-2.5 text-xs font-bold uppercase text-[var(--text-muted)]">
        <span className="min-w-0 truncate">{isGuest ? 'Guest' : 'Host'} messages · {name}</span>
        {raisedHere && <span className="shrink-0 rounded-full bg-primary px-2 py-0.5 text-[9px] font-bold text-primary-foreground">Opened ticket</span>}
      </h2>
      <div ref={scroller} className="agent-scroll flex flex-1 flex-col gap-2.5 overflow-y-auto px-4 py-3.5">
        {raisedHere && (
          <>
            <article className={`${bubble} self-start bg-muted`}>
              <div className="text-xs">{detail.ticket.description}</div>
              {detail.ticket.supportingText && <div className="mt-1 text-xs">{detail.ticket.supportingText}</div>}
              <div className="mt-1 text-[10px] text-[var(--text-muted)]">Requested remedy: {REMEDY[detail.ticket.requestedRemedy] ?? detail.ticket.requestedRemedy}</div>
              <div className={stamp}>{formatDateTime(detail.ticket.createdAt)}</div>
            </article>
          </>
        )}
        {error !== undefined && <p role="alert" className="text-xs text-destructive">Could not load messages</p>}
        {data && messages.length === 0 && !raisedHere && <p className="m-0 text-xs text-[var(--text-subtle)]">No messages yet.</p>}
        {messages.map((message) => (
          <article
            key={message.id}
            className={`${bubble} ${message.authorRole === 'AGENT' || message.authorRole === 'SYSTEM' ? 'self-end bg-accent' : 'self-start bg-muted'}`}
          >
            <div className="mb-1 text-[10px] font-bold text-[var(--text-muted)]">{authorRoleLabel(message.authorRole)}</div>
            <div className="whitespace-pre-wrap text-xs">{message.body}</div>
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
              className="box-border min-w-0 flex-1 rounded-lg border border-border bg-background px-2.5 py-2 text-xs disabled:opacity-50"
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
      <ResizeGrip height={height} min={CHAT_MIN_HEIGHT} max={CHAT_MAX_HEIGHT} onChange={onResize} label={`Resize ${isGuest ? 'guest' : 'host'} messages`} />
    </section>
  );
}
