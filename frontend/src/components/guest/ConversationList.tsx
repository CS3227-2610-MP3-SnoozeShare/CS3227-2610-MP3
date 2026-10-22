'use client';

import type { ConversationSummary, BookingConversationSummary } from '@/lib/guest-api';

export type ConversationItem =
  | { kind: 'ticket'; data: ConversationSummary }
  | { kind: 'booking'; data: BookingConversationSummary };

export function mergeConversations(
  tickets: ConversationSummary[],
  bookings: BookingConversationSummary[],
): ConversationItem[] {
  const items: ConversationItem[] = [
    ...tickets.map((t) => ({ kind: 'ticket' as const, data: t })),
    ...bookings.map((b) => ({ kind: 'booking' as const, data: b })),
  ];
  items.sort((a, b) => {
    const aTime = a.data.lastMessage?.sentAt ?? '';
    const bTime = b.data.lastMessage?.sentAt ?? '';
    return bTime.localeCompare(aTime);
  });
  return items;
}

function itemId(item: ConversationItem): string {
  return item.kind === 'ticket' ? `ticket:${item.data.ticketId}` : `booking:${item.data.bookingId}`;
}

function itemLabel(item: ConversationItem): string {
  if (item.kind === 'ticket') {
    return item.data.ticketTitle || item.data.ticketLabel;
  }
  return `${item.data.listingTitle} (${item.data.startDate})`;
}

export function ConversationList({
  items,
  selectedId,
  onSelect,
}: {
  items: ConversationItem[];
  selectedId: string | null;
  onSelect: (id: string, item: ConversationItem) => void;
}) {
  if (items.length === 0) {
    return <p data-testid="no-conversations" className="p-4 text-sm text-muted-foreground">No conversations yet.</p>;
  }

  return (
    <ul data-testid="conversation-list" className="divide-y">
      {items.map((item) => {
        const id = itemId(item);
        const active = id === selectedId;
        const unread = item.data.unreadCount > 0;
        return (
          <li key={id}>
            <button type="button" onClick={() => onSelect(id, item)}
              className={`flex w-full items-start gap-2 px-3 py-2.5 text-left text-sm transition-colors ${active ? 'bg-primary/10' : 'hover:bg-muted/50'}`}>
              <span className="mt-0.5 shrink-0 text-xs text-muted-foreground">{item.kind === 'ticket' ? '🎫' : '💬'}</span>
              <div className="min-w-0 flex-1">
                <div className="flex items-center justify-between gap-1">
                  <span className={`truncate ${unread ? 'font-semibold' : ''}`}>{itemLabel(item)}</span>
                  {unread && (
                    <span className="shrink-0 rounded-full bg-primary px-1.5 text-[10px] font-bold text-primary-foreground">
                      {item.data.unreadCount}
                    </span>
                  )}
                </div>
                <p className="truncate text-xs text-muted-foreground">{item.data.counterpartName}</p>
              </div>
            </button>
          </li>
        );
      })}
    </ul>
  );
}
