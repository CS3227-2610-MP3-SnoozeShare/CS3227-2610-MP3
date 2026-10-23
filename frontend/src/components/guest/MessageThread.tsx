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
      <div className="flex-1 space-y-2 overflow-y-auto p-4" data-testid="message-list">
        {messages.map((m) => {
          const isMe = m.authorId === currentUserId;
          return (
            <div key={m.id} className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}>
              <div className={`max-w-[75%] rounded-lg px-3 py-2 text-sm ${isMe ? 'bg-primary text-primary-foreground' : 'bg-muted'}`}>
                {!isMe && <p className="mb-0.5 text-[10px] font-medium text-muted-foreground">{authorRoleLabel(m.authorRole)}</p>}
                <p className="whitespace-pre-wrap">{m.body}</p>
                <p className={`mt-0.5 text-[10px] ${isMe ? 'text-primary-foreground/70' : 'text-muted-foreground'}`}>
                  {new Date(m.sentAt).toLocaleString()}
                </p>
              </div>
            </div>
          );
        })}
        <div ref={endRef} />
      </div>
      {!disabled && (
        <form onSubmit={(e) => { e.preventDefault(); handleSend(); }}
          className="flex gap-2 border-t p-3">
          <input type="text" value={body} onChange={(e) => setBody(e.target.value)}
            placeholder="Type a message..." maxLength={5000}
            className="flex-1 rounded border px-3 py-1.5 text-sm" />
          <button type="submit" disabled={sending || !body.trim()}
            className="rounded bg-primary px-4 py-1.5 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50">
            {sending ? 'Sending...' : 'Send'}
          </button>
        </form>
      )}
      {error && <p className="px-3 pb-2 text-xs text-red-600">{error}</p>}
    </div>
  );
}
