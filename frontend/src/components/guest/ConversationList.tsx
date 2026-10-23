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
    <div data-testid="conversation-list" className="divide-y divide-[var(--border-muted)]">
      {items.map((item) => {
        const id = itemId(item);
        const active = id === selectedId;
        const unread = item.data.unreadCount > 0;
        const isTicket = item.kind === 'ticket';
        const statusLabel = item.data.open ? 'OPEN' : 'RESOLVED';
        const statusClass = item.data.open ? 'bg-[var(--warning-subtle)] text-[var(--warning)]' : 'bg-[var(--success-subtle)] text-[var(--success)]';
        return (
          <button
            key={id}
            type="button"
            onClick={() => onSelect(id, item)}
            className={`w-full rounded-none px-3 py-2 text-left ${active ? 'bg-[var(--surface-inset)]' : 'hover:bg-[var(--surface-inset)]'}`}
          >
            <span className="mt-2 flex items-center justify-between gap-2">
              <span className="flex items-center gap-1 truncate text-sm"><span className="shrink-0">{isTicket ? '🎫' : '💬'}</span><strong>{itemLabel(item)}</strong></span>
              <span className={`shrink-0 rounded-full px-2 py-0.5 text-[10px] font-bold ${statusClass}`}>{statusLabel}</span>
            </span>
            <span className="mt-1 mb-2 flex items-center justify-between gap-1 text-xs text-[var(--text-subtle)]">
              <span className="truncate">{item.data.counterpartName}</span>
              {unread && (
                <span className="shrink-0 rounded-full bg-primary px-1.5 text-[10px] font-bold text-primary-foreground">
                  {item.data.unreadCount}
                </span>
              )}
            </span>
          </button>
        );
      })}
    </div>
  );
}
