'use client';

import { useState } from 'react';
import type { Conversation } from './hostTypes';

export function HostMessagesPanel({ conversations }: { conversations: Conversation[] }) {
  const [selectedId, setSelectedId] = useState(conversations[0]?.id);
  const [draft, setDraft] = useState('');
  const [localMessages, setLocalMessages] = useState<Record<string, string[]>>({});
  const selected = conversations.find((conversation) => conversation.id === selectedId) ?? conversations[0];
  const resolved = selected?.status === 'resolved';
  const send = () => { if (!selected || resolved || !draft.trim()) return; setLocalMessages((messages) => ({ ...messages, [selected.id]: [...(messages[selected.id] ?? []), draft.trim()] })); setDraft(''); };
  if (!selected) return <div className="ss-card p-10 text-center text-[var(--text-subtle)]">No messages yet.</div>;
  return <div className="grid min-h-[520px] gap-4 lg:grid-cols-[320px_minmax(0,1fr)]"><aside className="ss-card p-2" aria-label="Conversation list">{conversations.map((conversation) => <button className={`w-full rounded-lg p-3 text-left ${conversation.id === selected.id ? 'bg-[var(--surface-inset)]' : 'hover:bg-[var(--surface-inset)]'}`} key={conversation.id} onClick={() => setSelectedId(conversation.id)} type="button"><strong className="block text-sm">{conversation.title}</strong><span className="mt-1 block text-xs text-[var(--text-subtle)]">{conversation.subtitle}</span><span className="mt-2 block text-xs">{conversation.subject} · {conversation.status.toUpperCase()}</span></button>)}</aside><section className="ss-card flex min-h-[520px] flex-col p-5"><div className="border-b border-[var(--border-muted)] pb-4"><h2 className="font-semibold">{selected.subject}</h2><p className="mt-1 text-xs text-[var(--text-subtle)]">{selected.subtitle}</p></div><div className="flex-1 space-y-3 py-5">{selected.messages.map((message, index) => <div className="rounded-lg bg-[var(--surface-inset)] p-3 text-sm" key={`${message.sender}-${index}`}><strong>{message.sender}</strong><p className="mt-1">{message.body}</p></div>)}{(localMessages[selected.id] ?? []).map((message, index) => <div className="rounded-lg bg-[var(--accent)] p-3 text-sm" key={`local-${index}`}><strong>You</strong><p className="mt-1">{message}</p></div>)}</div><div className="flex gap-2"><input aria-label="Write a message…" className="min-w-0 flex-1 rounded-lg border border-input bg-background px-3 py-2 text-sm" disabled={resolved} placeholder="Write a message…" value={draft} onChange={(event) => setDraft(event.target.value)} /><button className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground disabled:opacity-50" disabled={resolved} onClick={send} type="button">Send</button></div>{resolved && <p className="mt-2 text-xs text-[var(--text-subtle)]">This conversation is resolved and read-only.</p>}</section></div>;
}
