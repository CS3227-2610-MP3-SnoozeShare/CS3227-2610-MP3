import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { BookingConversationSummary, ConversationSummary } from '@/lib/guest-api';
import { ConversationList, mergeConversations } from './ConversationList';

const ticket: ConversationSummary = {
  ticketId: 'ticket', channel: 'GUEST', ticketLabel: 'T-001', ticketTitle: 'Broken AC',
  listingTitle: 'Beach House', counterpartName: 'Support', unreadCount: 2, open: true,
  lastMessage: { id: 'assistant', authorId: 'system', authorRole: 'SYSTEM', body: '<img src=x onerror=alert(1)> **bold**', sentAt: '2026-10-23T05:00:00Z' },
};
const booking: BookingConversationSummary = {
  bookingId: 'booking', listingTitle: 'Beach House', startDate: '2026-11-01', endDate: '2026-11-03',
  counterpartName: 'Host name', unreadCount: 0, open: true,
  lastMessage: { id: 'host', authorId: 'host', authorRole: 'HOST', body: 'Booking body', sentAt: '2026-10-23T04:00:00Z' },
};

describe('ConversationList', () => {
  afterEach(cleanup);

  it('F7/R11 shows the stored assistant preview literally with unread count and selection', () => {
    const onSelect = vi.fn();
    const item = { kind: 'ticket' as const, data: ticket };
    const { container } = render(<ConversationList items={[item]} selectedId="ticket:ticket" onSelect={onSelect} />);
    expect(screen.getByText(ticket.lastMessage!.body)).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(container.querySelector('img, strong')).toBeNull();
    fireEvent.click(screen.getByRole('button'));
    expect(onSelect).toHaveBeenCalledWith('ticket:ticket', item);
  });

  it('preserves a ticket without messages and its title fallback', () => {
    render(<ConversationList items={[{ kind: 'ticket', data: { ...ticket, ticketTitle: '', lastMessage: null, unreadCount: 0 } }]} selectedId={null} onSelect={vi.fn()} />);
    expect(screen.getByText('T-001')).toBeInTheDocument();
    expect(screen.getByText('Support')).toBeInTheDocument();
    expect(screen.queryByText('2')).not.toBeInTheDocument();
  });

  it('preserves booking title, stay date and counterpart presentation', () => {
    render(<ConversationList items={[{ kind: 'booking', data: booking }]} selectedId={null} onSelect={vi.fn()} />);
    expect(screen.getByText('Beach House (2026-11-01)')).toBeInTheDocument();
    expect(screen.getByText('Host name')).toBeInTheDocument();
    expect(screen.queryByText('Booking body')).not.toBeInTheDocument();
  });

  it('preserves the empty state', () => {
    render(<ConversationList items={[]} selectedId={null} onSelect={vi.fn()} />);
    expect(screen.getByText('No conversations yet.')).toBeInTheDocument();
  });

  it('preserves newest-message ordering across ticket and booking conversations', () => {
    const olderTicket = { ...ticket, ticketId: 'older', lastMessage: null };
    const items = mergeConversations([olderTicket, ticket], [booking]);
    expect(items.map((item) => item.kind === 'ticket' ? item.data.ticketId : item.data.bookingId)).toEqual(['ticket', 'booking', 'older']);
  });
});
