'use client';

import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import {
  getConversations,
  getTicketThread,
  getBookingThread,
  postToTicket,
  postToBooking,
  markTicketRead,
  markBookingRead,
  type MessageResponse,
} from '@/lib/guest-api';
import { BackendApiError } from '@/lib/api';
import { useAuth } from '@/providers/AuthProvider';
import { ConversationList, mergeConversations, type ConversationItem } from '@/components/guest/ConversationList';
import { MessageThread } from '@/components/guest/MessageThread';
import { TicketFilingDialog } from '@/components/guest/TicketFilingDialog';

export default function MessagesPage() {
  const searchParams = useSearchParams();
  const { user } = useAuth();

  const [items, setItems] = useState<ConversationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [selectedItem, setSelectedItem] = useState<ConversationItem | null>(null);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [threadLoading, setThreadLoading] = useState(false);

  const [ticketBookingId, setTicketBookingId] = useState<string | null>(null);

  const loadConversations = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getConversations();
      const merged = mergeConversations(data.ticketConversations, data.bookingConversations);
      setItems(merged);

      // Handle ?booking={id} deep link
      const bookingParam = searchParams.get('booking');
      if (bookingParam) {
        const match = merged.find(
          (i) => i.kind === 'booking' && i.data.bookingId === bookingParam,
        );
        if (match) {
          const id = `booking:${bookingParam}`;
          setSelectedId(id);
          setSelectedItem(match);
        }
      }
    } catch (e) {
      setError(e instanceof BackendApiError ? e.message : 'Failed to load conversations');
    } finally {
      setLoading(false);
    }
  }, [searchParams]);

  useEffect(() => { loadConversations(); }, [loadConversations]);

  // Load thread when selection changes
  useEffect(() => {
    if (!selectedItem) {
      setMessages([]);
      return;
    }
    setThreadLoading(true);
    const load = selectedItem.kind === 'ticket'
      ? getTicketThread(selectedItem.data.ticketId)
      : getBookingThread(selectedItem.data.bookingId);

    const markRead = selectedItem.kind === 'ticket'
      ? markTicketRead(selectedItem.data.ticketId)
      : markBookingRead(selectedItem.data.bookingId);

    Promise.all([load, markRead])
      .then(([msgs]) => setMessages(msgs))
      .catch(() => {})
      .finally(() => setThreadLoading(false));
  }, [selectedItem]);

  const handleSelect = useCallback((id: string, item: ConversationItem) => {
    setSelectedId(id);
    setSelectedItem(item);
  }, []);

  const handleSend = useCallback(async (body: string) => {
    if (!selectedItem) return;
    let msg: MessageResponse;
    if (selectedItem.kind === 'ticket') {
      msg = await postToTicket(selectedItem.data.ticketId, body);
    } else {
      msg = await postToBooking(selectedItem.data.bookingId, body);
    }
    setMessages((prev) => [...prev, msg]);
  }, [selectedItem]);

  const isOpen = selectedItem?.data.open ?? true;

  return (
    <main aria-label="Messages" className="flex min-h-[calc(100vh-4rem)] flex-col p-6">
      <div className="mb-5 flex shrink-0 items-center justify-between">
        <h2 className="text-2xl font-bold">Messages</h2>
        <button type="button" onClick={() => setTicketBookingId('pick')}
          className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90">
          File Ticket
        </button>
      </div>

      {loading && <div className="ss-card h-64 animate-pulse bg-[var(--surface-inset)]" />}
      {error && <div className="ss-card p-8 text-center text-destructive">{error}</div>}

      {!loading && !error && (
        <div className="grid min-h-0 flex-1 grid-rows-[minmax(0,0.8fr)_minmax(0,1.2fr)] gap-0 overflow-hidden rounded-md border border-[var(--border-muted)] bg-card lg:grid-cols-[minmax(280px,27fr)_minmax(0,73fr)] lg:grid-rows-1">
          {/* Sidebar */}
          <aside className="min-h-0 overflow-y-auto border-b border-[var(--border-muted)] bg-card lg:border-b-0 lg:border-r" aria-label="Conversation list">
            <ConversationList items={items} selectedId={selectedId} onSelect={handleSelect} />
          </aside>

          {/* Thread */}
          <section className="flex min-h-0 min-w-0 flex-col bg-background">
            {!selectedItem && (
              <div className="flex flex-1 items-center justify-center text-[var(--text-subtle)]">
                <p data-testid="select-prompt">Select a conversation to view messages</p>
              </div>
            )}
            {selectedItem && threadLoading && (
              <p className="p-4 text-sm text-[var(--text-subtle)]">Loading messages…</p>
            )}
            {selectedItem && !threadLoading && (
              <MessageThread
                messages={messages}
                currentUserId={user?.uid ?? ''}
                onSend={handleSend}
                disabled={!isOpen}
              />
            )}
          </section>
        </div>
      )}

      <TicketFilingDialog
        bookingId={ticketBookingId === 'pick' ? null : ticketBookingId}
        onClose={() => setTicketBookingId(null)}
        onFiled={() => { setTicketBookingId(null); loadConversations(); }}
      />
    </main>
  );
}
