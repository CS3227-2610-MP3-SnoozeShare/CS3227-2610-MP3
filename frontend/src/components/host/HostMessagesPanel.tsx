'use client';

import { useEffect, useRef, useState } from 'react';
import type { Conversation } from './hostTypes';

export function HostMessagesPanel({ conversations }: { conversations: Conversation[] }) {
  const [selectedId, setSelectedId] = useState(conversations[0]?.id);
  const [draft, setDraft] = useState('');
  const [localMessages, setLocalMessages] = useState<Record<string, string[]>>({});
  const messageHistoryRef = useRef<HTMLDivElement>(null);
  const selected = conversations.find((conversation) => conversation.id === selectedId) ?? conversations[0];
  const resolved = selected?.status === 'resolved';
  const messageCount = (selected?.messages.length ?? 0) + (selected ? localMessages[selected.id]?.length ?? 0 : 0);
  const send = () => { if (!selected || resolved || !draft.trim()) return; setLocalMessages((messages) => ({ ...messages, [selected.id]: [...(messages[selected.id] ?? []), draft.trim()] })); setDraft(''); };

  useEffect(() => {
    if (messageHistoryRef.current) messageHistoryRef.current.scrollTop = messageHistoryRef.current.scrollHeight;
  }, [selectedId, messageCount]);

  if (!selected) return <div className="ss-card p-10 text-center text-[var(--text-subtle)]">No messages yet.</div>;
  return (
    <div className="grid min-h-0 flex-1 grid-rows-[minmax(0,0.8fr)_minmax(0,1.2fr)] gap-4 lg:grid-cols-[320px_minmax(0,1fr)] lg:grid-rows-1">
      <aside className="ss-card min-h-0 overflow-y-auto p-2" aria-label="Conversation list">
        {conversations.map((conversation) => (
          <button
            className={`w-full rounded-lg p-3 text-left ${conversation.id === selected.id ? 'bg-[var(--surface-inset)]' : 'hover:bg-[var(--surface-inset)]'}`}
            key={conversation.id}
            onClick={() => setSelectedId(conversation.id)}
            type="button"
          >
            <strong className="block text-sm">{conversation.title}</strong>
            <span className="mt-1 block text-xs text-[var(--text-subtle)]">{conversation.subtitle}</span>
            <span className="mt-2 block text-xs">{conversation.subject} · {conversation.status.toUpperCase()}</span>
          </button>
        ))}
      </aside>
      <section className="ss-card flex min-h-0 min-w-0 flex-col p-5">
        <div className="border-b border-[var(--border-muted)] pb-4">
          <h2 className="text-lg font-bold">{selected.title}</h2>
          <p className="mt-1 text-sm text-[var(--text-subtle)]">{selected.subtitle}</p>
        </div>
        <div aria-label="Message history" className="min-h-0 flex-1 space-y-3 overflow-y-auto py-5" ref={messageHistoryRef}>
          {[...selected.messages, ...(localMessages[selected.id] ?? []).map((body) => ({ sender: 'You', body }))].map((message, index) => {
            const outgoing = message.sender === 'You';
            return (
              <div className={`flex ${outgoing ? 'justify-end' : 'justify-start'}`} key={`${message.sender}-${index}`}>
                <div
                  aria-label={`Message from ${message.sender}`}
                  className={`max-w-[80%] rounded-xl px-4 py-3 text-sm ${outgoing ? 'bg-[var(--accent)]' : 'bg-[var(--surface-inset)]'}`}
                  role="group"
                >
                  <strong>{message.sender}</strong>
                  <p className="mt-1">{message.body}</p>
                </div>
              </div>
            );
          })}
        </div>
        <div className="flex gap-2">
          <input aria-label="Write a message…" className="min-w-0 flex-1 rounded-lg border border-input bg-background px-3 py-2 text-sm" disabled={resolved} onChange={(event) => setDraft(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); send(); } }} placeholder="Write a message…" value={draft} />
          <button className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground disabled:opacity-50" disabled={resolved} onClick={send} type="button">Send</button>
        </div>
        {resolved && <p className="mt-2 text-xs text-[var(--text-subtle)]">This conversation is resolved and read-only.</p>}
      </section>
    </div>
  );
}
