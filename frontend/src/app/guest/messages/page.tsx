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
    <main aria-label="Messages" className="flex min-h-[calc(100vh-4rem)]">
      {/* Sidebar */}
      <aside className="w-72 shrink-0 border-r bg-background">
        <div className="flex items-center justify-between border-b px-3 py-2">
          <h2 className="text-sm font-semibold">Messages</h2>
          <button type="button" onClick={() => setTicketBookingId('pick')}
            className="rounded bg-primary px-2 py-1 text-xs font-medium text-primary-foreground hover:opacity-90">
            File Ticket
          </button>
        </div>
        {loading && <p className="p-3 text-sm text-muted-foreground">Loading...</p>}
        {error && <p className="p-3 text-sm text-red-600">{error}</p>}
        {!loading && !error && (
          <ConversationList items={items} selectedId={selectedId} onSelect={handleSelect} />
        )}
      </aside>

      {/* Thread */}
      <div className="flex flex-1 flex-col">
        {!selectedItem && (
          <div className="flex flex-1 items-center justify-center text-muted-foreground">
            <p data-testid="select-prompt">Select a conversation to view messages</p>
          </div>
        )}
        {selectedItem && threadLoading && (
          <p className="p-4 text-sm text-muted-foreground">Loading messages...</p>
        )}
        {selectedItem && !threadLoading && (
          <MessageThread
            messages={messages}
            currentUserId={user?.uid ?? ''}
            onSend={handleSend}
            disabled={!isOpen}
          />
        )}
      </div>

      <TicketFilingDialog
        bookingId={ticketBookingId === 'pick' ? null : ticketBookingId}
        onClose={() => setTicketBookingId(null)}
        onFiled={() => { setTicketBookingId(null); loadConversations(); }}
      />
    </main>
  );
}
