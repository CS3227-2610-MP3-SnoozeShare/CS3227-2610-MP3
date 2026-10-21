import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { HostMessagesPanel } from './HostMessagesPanel';
import { HostWalletPanel } from './HostWalletPanel';
import { createHostFixtureAdapter } from './hostFixtures';

describe('Host messages and wallet', () => {
  it('creates direct-message threads from the current pending booking requests', () => {
    const adapter = createHostFixtureAdapter();
    const requests = adapter.getRequests().data!;
    const conversations = adapter.getMessages().data!;
    const directMessages = conversations.filter((conversation) => conversation.id.startsWith('direct-'));

    render(<HostMessagesPanel conversations={conversations} />);
    expect(directMessages).toHaveLength(requests.pending.length);
    for (const request of requests.pending) {
      expect(directMessages).toEqual(expect.arrayContaining([
        expect.objectContaining({
          id: `direct-${request.id.replace('request-', '')}`,
          title: `${request.listing} · ${request.dates}`,
        }),
      ]));
    }
    expect(conversations.some((conversation) => conversation.id.startsWith('ticket-'))).toBe(true);
    expect(screen.queryByRole('button', { name: /New Ticket/i })).not.toBeInTheDocument();
  });

  it('renders the exact conversations and disables resolved composer', () => {
    const conversations = createHostFixtureAdapter().getMessages().data!;
    render(<HostMessagesPanel conversations={conversations} />);
    expect(screen.getByText(/Question about check-in/)).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Write a message…')).not.toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: /Ticket #1042/ }));
    expect(screen.getByPlaceholderText('Write a message…')).toBeDisabled();
  });

  it('shows booking context in the thread header and visually separates each sender', () => {
    render(<HostMessagesPanel conversations={createHostFixtureAdapter().getMessages().data!} />);

    expect(screen.getByRole('heading', { name: 'Lakeside Cabin · Oct 3–6' })).toBeInTheDocument();
    expect(screen.getAllByText('Direct message with Jane G. (Guest) · Booking request')).toHaveLength(2);
    const incoming = screen.getAllByRole('group', { name: 'Message from Jane G.' });
    expect(incoming).toHaveLength(2);
    expect(incoming[0].parentElement).toHaveClass('justify-start');
    expect(screen.getByRole('group', { name: 'Message from You' }).parentElement).toHaveClass('justify-end');
  });

  it('keeps conversation and message overflow inside full-height columns', () => {
    render(<HostMessagesPanel conversations={createHostFixtureAdapter().getMessages().data!} />);

    expect(screen.getByLabelText('Conversation list')).toHaveClass('min-h-0', 'overflow-y-auto');
    expect(screen.getByLabelText('Message history')).toHaveClass('min-h-0', 'overflow-y-auto');
  });

  it('scrolls the message history to the newest message after sending', async () => {
    render(<HostMessagesPanel conversations={createHostFixtureAdapter().getMessages().data!} />);
    const history = screen.getByLabelText('Message history');
    Object.defineProperty(history, 'scrollHeight', { configurable: true, value: 1200 });

    fireEvent.change(screen.getByPlaceholderText('Write a message…'), { target: { value: 'One more detail.' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send' }));

    await waitFor(() => expect(history.scrollTop).toBe(1200));
  });

  it('sends a message when Enter is pressed in the composer', async () => {
    const user = userEvent.setup();
    render(<HostMessagesPanel conversations={createHostFixtureAdapter().getMessages().data!} />);
    const composer = screen.getByPlaceholderText('Write a message…');

    await user.type(composer, 'Sent with Enter{Enter}');

    expect(screen.getByText('Sent with Enter')).toBeInTheDocument();
    expect(composer).toHaveValue('');
  });

  it('renders wallet balance, statement, and mocked local controls', async () => {
    const adapter = createHostFixtureAdapter();
    render(<HostWalletPanel wallet={adapter.getWalletSummary().data!} statement={adapter.getStatement().data!} />);
    expect(screen.getByText('Wallet: $3,920.50')).toBeInTheDocument();
    expect(screen.getAllByText('BOOKING_PAYOUT').length).toBeGreaterThanOrEqual(1);
    fireEvent.click(screen.getByRole('button', { name: 'Top up' }));
    await waitFor(() => expect(screen.getByRole('dialog')).toHaveTextContent('no real payment gateway'));
  });
});
