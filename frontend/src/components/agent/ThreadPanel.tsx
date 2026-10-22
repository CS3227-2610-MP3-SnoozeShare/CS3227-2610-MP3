'use client';

import { useState } from 'react';
import { isSessionFailure } from '@/lib/api';
import { fetchThread, postMessage } from '@/lib/agent-api/disputes';
import { formatDateTime } from '@/lib/agent-api/format';
import type { DisputeDetail, ThreadChannel } from '@/lib/agent-api/types';
import { SessionNotice } from './SessionNotice';
import { usePolling } from './usePolling';

type Props = {
  detail: DisputeDetail;
  channel: ThreadChannel;
  canSend: boolean;
  resolved: boolean;
};

export function ThreadPanel({ detail, channel, canSend, resolved }: Props) {
  const ticketId = detail.ticket.id;
  const { data, error, reload } = usePolling(() => fetchThread(ticketId, channel), 10000);
  const [body, setBody] = useState('');
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<unknown>(undefined);
  const isGuest = channel === 'GUEST';
  const name = isGuest ? detail.guestName : detail.hostName;
  const placeholder = isGuest ? 'Reply to guest...' : 'Reply to host...';

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

  return (
    <section aria-label={`${isGuest ? 'Guest' : 'Host'} messages`} className="flex flex-col rounded-md border border-[var(--border)] bg-card">
      <h2 className="border-b border-[var(--border)] p-3 text-sm font-bold">
        {isGuest ? 'Guest' : 'Host'} messages - {name}
        {isGuest && <span className="ml-2 rounded-full bg-primary px-2 py-0.5 text-[10px] text-primary-foreground">Opened ticket</span>}
      </h2>
      <div className="flex max-h-96 flex-col gap-2 overflow-y-auto p-3 text-sm">
        {isGuest && (
          <article className="rounded-md border border-[var(--border)] p-3">
            <p className="font-semibold">{detail.ticket.title}</p>
            <p>{detail.ticket.description}</p>
            <p className="text-xs text-muted-foreground">Requested remedy: {detail.ticket.requestedRemedy.replace('_', ' ').toLowerCase()}</p>
            {detail.ticket.supportingText && <p className="text-xs">{detail.ticket.supportingText}</p>}
            <p className="text-xs text-muted-foreground">{formatDateTime(detail.ticket.createdAt)}</p>
          </article>
        )}
        {error !== undefined && <p role="alert" className="text-xs text-red-700">Could not load messages</p>}
        {(data ?? []).map((message) => (
          <article
            key={message.id}
            className={`max-w-[80%] rounded-md p-2 ${message.authorRole === 'AGENT' ? 'self-end bg-primary/15' : 'self-start bg-muted'}`}
          >
            <p>{message.body}</p>
            <p className="text-[10px] text-muted-foreground">{formatDateTime(message.sentAt)}</p>
          </article>
        ))}
      </div>
      <div className="border-t border-[var(--border)] p-3">
        {resolved ? (
          <p className="text-xs text-muted-foreground">Ticket is resolved</p>
        ) : (
          <div className="flex gap-2">
            <textarea
              aria-label={placeholder}
              placeholder={placeholder}
              value={body}
              maxLength={2000}
              disabled={!canSend}
              onChange={(event) => setBody(event.target.value)}
              className="min-h-16 flex-1 rounded-md border border-[var(--border)] p-2 text-sm disabled:opacity-50"
            />
            <button
              type="button"
              disabled={!canSend || sending || body.trim() === ''}
              onClick={() => void send()}
              className="self-end rounded-md bg-primary px-4 py-2 text-xs font-bold text-primary-foreground disabled:opacity-50"
            >
              Send
            </button>
          </div>
        )}
        {sendError !== undefined && <p role="alert" className="mt-1 text-xs text-red-700">{sendError instanceof Error ? sendError.message : 'Could not send'}</p>}
      </div>
    </section>
  );
}
