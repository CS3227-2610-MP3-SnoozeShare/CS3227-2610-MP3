import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import HostMessagesPage from './page';
import { HostFixtureProvider } from '@/components/host/HostFixtureProvider';
import { HostApiProvider } from '@/components/host/HostApiProvider';

const { fetchBookingConversations, fetchTicketConversations, fetchBookingMessages, fetchTicketMessages, postBookingMessage, postTicketMessage, markBookingConversationRead, markTicketConversationRead, fetchHostWallet } = vi.hoisted(() => ({ fetchBookingConversations: vi.fn(), fetchTicketConversations: vi.fn(), fetchBookingMessages: vi.fn(), fetchTicketMessages: vi.fn(), postBookingMessage: vi.fn(), postTicketMessage: vi.fn(), markBookingConversationRead: vi.fn(), markTicketConversationRead: vi.fn(), fetchHostWallet: vi.fn() }));
vi.mock('@/lib/hostApi', async (importOriginal) => ({ ...await importOriginal<typeof import('@/lib/hostApi')>(), fetchBookingConversations, fetchTicketConversations, fetchBookingMessages, fetchTicketMessages, postBookingMessage, postTicketMessage, markBookingConversationRead, markTicketConversationRead, fetchHostWallet, topUpHostWallet: vi.fn(), withdrawFromHostWallet: vi.fn() }));

describe('Host messages page', () => {
  it('renders direct and resolved conversations in a full-height page frame', () => {
    render(<HostFixtureProvider><HostMessagesPage /></HostFixtureProvider>);
    expect(screen.getByText('Messages')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Ticket #1042/ })).toHaveTextContent('Refund not received');
    expect(screen.getByRole('main')).toHaveClass('flex', 'flex-1', 'min-h-0', 'flex-col');
  });

  it('loads booking and HOST ticket channels separately and reads the selected booking thread', async () => {
    fetchBookingConversations.mockResolvedValue([{ bookingId: 'booking-1', listingTitle: 'API Cabin', startDate: '2026-10-25', endDate: '2026-10-27', counterpartName: 'Guest One', lastMessage: null, unreadCount: 1, open: true }]);
    fetchTicketConversations.mockResolvedValue([{ ticketId: 'ticket-2', channel: 'HOST', ticketLabel: '1042', ticketTitle: 'Refund issue', listingTitle: 'API Cabin', counterpartName: 'Support Agent', lastMessage: null, unreadCount: 0, open: false }]);
    fetchBookingMessages.mockResolvedValue([{ id: 'm1', authorId: 'guest', authorRole: 'GUEST', body: 'Hello from API', sentAt: '2026-10-22T10:00:00Z' }, { id: 'm0', authorId: 'guest', authorRole: 'GUEST', body: 'Older API message', sentAt: '2026-10-21T10:00:00Z' }]);
    fetchTicketMessages.mockResolvedValue([]);
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: [] });
    render(<HostApiProvider><HostFixtureProvider><HostMessagesPage /></HostFixtureProvider></HostApiProvider>);

    expect(await screen.findByRole('button', { name: /Refund issue/ })).toBeInTheDocument();
    expect(await screen.findByText('Hello from API')).toBeInTheDocument();
    expect(fetchBookingConversations).toHaveBeenCalledWith();
    expect(fetchTicketConversations).toHaveBeenCalledWith();
    expect(fetchBookingMessages).toHaveBeenCalledWith('booking-1');
    expect(markBookingConversationRead).toHaveBeenCalledWith('booking-1');
    expect(screen.queryByRole('button', { name: /New Ticket/i })).not.toBeInTheDocument();
    expect(await screen.findByText('Older API message')).toBeInTheDocument();
  });

  it('posts messages through the booking-specific route and never creates a local success on failure', async () => {
    fetchBookingConversations.mockResolvedValue([{ bookingId: 'booking-1', listingTitle: 'API Cabin', startDate: '2026-10-25', endDate: '2026-10-27', counterpartName: 'Guest One', lastMessage: null, unreadCount: 0, open: true }]);
    fetchTicketConversations.mockResolvedValue([]);
    fetchBookingMessages.mockResolvedValue([]);
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: [] });
    postBookingMessage.mockRejectedValue(new Error('private backend data'));
    render(<HostApiProvider><HostFixtureProvider><HostMessagesPage /></HostFixtureProvider></HostApiProvider>);
    await screen.findByPlaceholderText('Write a message…');
    fireEvent.change(screen.getByPlaceholderText('Write a message…'), { target: { value: 'Please confirm.' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send' }));

    await waitFor(() => expect(postBookingMessage).toHaveBeenCalledWith('booking-1', 'Please confirm.'));
    expect(screen.queryByText('Please confirm.')).not.toBeInTheDocument();
  });

  it('loads a resolved ticket through the ticket route and keeps its composer read-only', async () => {
    fetchBookingConversations.mockResolvedValue([]);
    fetchTicketConversations.mockResolvedValue([{ ticketId: 'ticket-2', channel: 'HOST', ticketLabel: '1042', ticketTitle: 'Refund issue', listingTitle: 'API Cabin', counterpartName: 'Support Agent', lastMessage: null, unreadCount: 0, open: false }]);
    fetchTicketMessages.mockResolvedValue([]);
    fetchHostWallet.mockResolvedValue({ wallet: { id: 'w', userId: 'h', balance: 10, currency: 'SGD', updatedAt: '' }, statement: [] });
    render(<HostApiProvider><HostFixtureProvider><HostMessagesPage /></HostFixtureProvider></HostApiProvider>);

    expect(await screen.findByText('This conversation is resolved and read-only.')).toBeInTheDocument();
    await waitFor(() => expect(fetchTicketMessages).toHaveBeenCalledWith('ticket-2'));
    expect(markTicketConversationRead).toHaveBeenCalledWith('ticket-2');
    expect(screen.getByPlaceholderText('Write a message…')).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled();
    expect(postTicketMessage).not.toHaveBeenCalled();
  });
});
