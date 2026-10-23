'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import type { MessageResponse } from '@/lib/guest-api';
import { BackendApiError } from '@/lib/api';
import { authorRoleLabel } from '@/lib/message-role';

export function MessageThread({
  messages,
  currentUserId,
  onSend,
  disabled,
}: {
  messages: MessageResponse[];
  currentUserId: string;
  onSend: (body: string) => Promise<void>;
  disabled?: boolean;
}) {
  const [body, setBody] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length]);

  const handleSend = useCallback(async () => {
    if (!body.trim() || sending) return;
    setSending(true);
    setError(null);
    try {
      await onSend(body.trim());
      setBody('');
    } catch (e) {
      setError(e instanceof BackendApiError ? e.message : 'Failed to send');
    } finally {
      setSending(false);
    }
  }, [body, sending, onSend]);

  return (
    <div className="flex h-full flex-col">
      <div aria-label="Message history" className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4" data-testid="message-list">
        {messages.length === 0 && <p className="text-center text-sm text-[var(--text-subtle)]">No messages yet.</p>}
        {messages.map((m) => {
          const isMe = m.authorId === currentUserId;
          const authorLabel = isMe ? 'You' : authorRoleLabel(m.authorRole);
          return (
            <div key={m.id} className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}>
              <div
                aria-label={`Message from ${authorLabel}`}
                className={`max-w-[80%] rounded-lg px-3 py-2 text-[13px] ${isMe ? 'bg-[var(--accent)]' : 'bg-[var(--surface-inset)]'}`}
                role="group"
              >
                <strong className="text-xs">{authorLabel}</strong>
                <p className="mt-1 whitespace-pre-wrap text-[13px]">{m.body}</p>
                <p className="mt-0.5 text-[10px] text-[var(--text-subtle)]">
                  {new Date(m.sentAt).toLocaleString()}
                </p>
              </div>
            </div>
          );
        })}
        <div ref={endRef} />
      </div>
      {error && <p className="px-4 text-sm text-destructive" role="alert">{error}</p>}
      <div className="flex flex-col gap-3 border-t border-[var(--border-muted)] px-4 pb-4 pt-3">
        {!disabled && (
          <div className="flex gap-2">
            <input aria-label="Write a message…" type="text" value={body} onChange={(e) => setBody(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleSend(); } }}
              placeholder="Write a message…" maxLength={5000}
              className="min-w-0 flex-1 rounded-lg border border-input bg-background px-3 py-2 text-sm" />
            <button type="button" onClick={handleSend} disabled={sending || !body.trim()}
              className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground disabled:opacity-50">
              {sending ? 'Sending…' : 'Send'}
            </button>
          </div>
        )}
        {disabled && <p className="rounded-md bg-[var(--surface-inset)] px-3 py-2 text-xs text-[var(--text-subtle)]">This conversation is closed and read-only.</p>}
      </div>
    </div>
  );
}
