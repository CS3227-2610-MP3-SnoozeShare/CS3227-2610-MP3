'use client';

import { useEffect, useRef, useState } from 'react';
import type { Conversation, Message } from './hostTypes';
import { hostSafeErrorMessage } from '@/lib/hostApi';

type Props = { conversations: Conversation[]; messagesByConversation?: Record<string, Message[]>; onSelect?: (conversation: Conversation) => Promise<void>; onSend?: (conversation: Conversation, body: string) => Promise<void>; mutationPending?: boolean; historyErrors?: Record<string, string> };
export function HostMessagesPanel({ conversations, messagesByConversation, onSelect, onSend, mutationPending = false, historyErrors = {} }: Props) {
  const [selectedId, setSelectedId] = useState(conversations[0]?.id);
  const [draft, setDraft] = useState('');
  const [localMessages, setLocalMessages] = useState<Record<string, string[]>>({});
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState('');
  const [sending, setSending] = useState(false);
  const messageHistoryRef = useRef<HTMLDivElement>(null);
  const selected = conversations.find((conversation) => conversation.id === selectedId) ?? conversations[0];
  const selectedMessages = selected ? messagesByConversation?.[selected.id] ?? selected.messages : [];
  const resolved = selected?.status === 'resolved';
  const messageCount = selectedMessages.length + (selected ? localMessages[selected.id]?.length ?? 0 : 0);
  useEffect(() => {
    if (!selected || !onSelect) return;
    setHistoryLoading(true);
    setHistoryError('');
    void onSelect(selected).catch((caught) => setHistoryError(hostSafeErrorMessage(caught))).finally(() => setHistoryLoading(false));
  }, [onSelect, selected]);
  const send = () => {
    if (!selected || resolved || !draft.trim() || mutationPending || sending) return;
    if (!onSend) {
      setLocalMessages((messages) => ({ ...messages, [selected.id]: [...(messages[selected.id] ?? []), draft.trim()] }));
      setDraft('');
      return;
    }
    const body = draft.trim();
    setSending(true);
    setHistoryError('');
    void onSend(selected, body).then(() => setDraft('')).catch((caught) => setHistoryError(hostSafeErrorMessage(caught))).finally(() => setSending(false));
  };

  useEffect(() => {
    if (messageHistoryRef.current) messageHistoryRef.current.scrollTop = messageHistoryRef.current.scrollHeight;
  }, [selectedId, messageCount]);

  if (!selected) return <div className="ss-card p-10 text-center text-[var(--text-subtle)]">No messages yet.</div>;
  return (
    <div className="grid min-h-0 flex-1 grid-rows-[minmax(0,0.8fr)_minmax(0,1.2fr)] gap-0 overflow-hidden rounded-md border border-[var(--border-muted)] bg-card lg:grid-cols-[minmax(280px,27fr)_minmax(0,73fr)] lg:grid-rows-1">
      <aside className="min-h-0 divide-y divide-[var(--border-muted)] overflow-y-auto border-b border-[var(--border-muted)] bg-card lg:border-b-0 lg:border-r" aria-label="Conversation list">
        {conversations.map((conversation) => (
          <button
            className={`w-full rounded-none px-3 py-2 text-left ${conversation.id === selected.id ? 'bg-[var(--surface-inset)]' : 'hover:bg-[var(--surface-inset)]'}`}
            key={conversation.id}
            onClick={() => setSelectedId(conversation.id)}
            type="button"
          >
            <span className="mt-2 flex items-center justify-between gap-2">
              <strong className="block text-sm">{conversation.title}</strong>
              <span className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-bold ${conversation.status === 'resolved' ? 'bg-[var(--success-subtle)] text-[var(--success)]' : 'bg-[var(--warning-subtle)] text-[var(--warning)]'}`}>{conversation.status.toUpperCase()}</span>
            </span>
            <span className="mt-1 mb-2 block text-xs text-[var(--text-subtle)]">{conversation.subtitle}</span>
          </button>
        ))}
      </aside>
      <section className="flex min-h-0 min-w-0 flex-col bg-background">
        <div className="border-b border-[var(--border-muted)] px-4 py-3">
          <h2 className="text-base font-bold">{selected.title}</h2>
          <p className="mt-0.5 text-xs text-[var(--text-subtle)]">{selected.subtitle}</p>
        </div>
        <div aria-label="Message history" className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4" ref={messageHistoryRef}>
          {historyLoading ? <p className="text-center text-sm text-[var(--text-subtle)]">Loading messages…</p> : selectedMessages.length === 0 && !localMessages[selected.id]?.length ? <p className="text-center text-sm text-[var(--text-subtle)]">No messages yet.</p> : [...selectedMessages, ...(localMessages[selected.id] ?? []).map((body) => ({ sender: 'You', body }))].map((message, index) => {
            const outgoing = message.sender === 'You';
            return (
              <div className={`flex ${outgoing ? 'justify-end' : 'justify-start'}`} key={`${message.sender}-${index}`}>
                <div
                  aria-label={`Message from ${message.sender}`}
                  className={`max-w-[80%] rounded-lg px-3 py-2 text-[13px] ${outgoing ? 'bg-[var(--accent)]' : 'bg-[var(--surface-inset)]'}`}
                  role="group"
                >
                  <strong className="text-xs">{message.sender}</strong>
                  <p className="mt-1 text-[13px]">{message.body}</p>
                </div>
              </div>
            );
          })}
        </div>
        {(historyError || historyErrors[selected.id]) && <p className="px-4 text-sm text-destructive" role="alert">{historyError || historyErrors[selected.id]}</p>}
        <div className="flex flex-col gap-3 border-t border-[var(--border-muted)] px-4 pb-4 pt-3">
          <div className="flex gap-2">
            <input aria-label="Write a message…" className="min-w-0 flex-1 rounded-lg border border-input bg-background px-3 py-2 text-sm" disabled={resolved || historyLoading || mutationPending || sending} onChange={(event) => setDraft(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); send(); } }} placeholder="Write a message…" value={draft} />
            <button className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground disabled:opacity-50" disabled={resolved || historyLoading || mutationPending || sending} onClick={send} type="button">{sending ? 'Sending…' : 'Send'}</button>
          </div>
          {resolved && <p className="rounded-md bg-[var(--surface-inset)] px-3 py-2 text-xs text-[var(--text-subtle)]">This conversation is resolved and read-only.</p>}
        </div>
      </section>
    </div>
  );
}
