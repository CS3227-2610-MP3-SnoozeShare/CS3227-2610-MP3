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
    expect(screen.getByRole('button', { name: /Lakeside Cabin · Oct 3–6/ })).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Write a message…')).not.toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: /Ticket #1042/ }));
    expect(screen.getByPlaceholderText('Write a message…')).toBeDisabled();
    const notice = screen.getByText('This conversation is resolved and read-only.');
    expect(notice).toHaveClass('rounded-md', 'px-3', 'py-2');
    expect(notice.parentElement).toBe(screen.getByPlaceholderText('Write a message…').parentElement?.parentElement);
  });

  it('shows booking context in the thread header and visually separates each sender', () => {
    render(<HostMessagesPanel conversations={createHostFixtureAdapter().getMessages().data!} />);

    expect(screen.getByRole('heading', { name: 'Lakeside Cabin · Oct 3–6' })).toBeInTheDocument();
    expect(screen.getAllByText('Booking #B-2291 · Direct message with Jane G. (Guest)')).toHaveLength(2);
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

  it('renders complete 1,001-message histories and 101-row statements without load-more controls', () => {
    const conversation = createHostFixtureAdapter().getMessages().data![0];
    const messages = Array.from({ length: 1001 }, (_, index) => ({
      sender: 'Guest', body: `Complete message ${index}`, timestamp: new Date(Date.UTC(2026, 0, 1, 0, 0, index)).toISOString(),
    }));
    const statement = Array.from({ length: 101 }, (_, index) => ({
      date: `Jan ${index + 1}`, type: 'TOP_UP', relatedTo: '—', amount: 1, balanceAfter: index + 1,
    }));
    const adapter = createHostFixtureAdapter();
    const { rerender } = render(<HostMessagesPanel conversations={[conversation]} messagesByConversation={{ [conversation.id]: messages }} />);

    expect(screen.getByText('Complete message 0')).toBeInTheDocument();
    expect(screen.getByText('Complete message 1000')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Load older messages/i })).not.toBeInTheDocument();

    rerender(<HostWalletPanel wallet={adapter.getWalletSummary().data!} statement={statement} />);
    expect(screen.getByRole('table').querySelectorAll('tbody tr')).toHaveLength(101);
    expect(screen.queryByRole('button', { name: /Load older transactions/i })).not.toBeInTheDocument();
  });

  it('uses the artifact’s compact list, chat surface, bubbles, and composer footer', () => {
    render(<HostMessagesPanel conversations={createHostFixtureAdapter().getMessages().data!} />);
    const conversationList = screen.getByLabelText('Conversation list');
    const layout = conversationList.parentElement!;
    const chat = screen.getByLabelText('Message history').parentElement!;
    const composer = screen.getByPlaceholderText('Write a message…').parentElement!;

    expect(layout).toHaveClass('gap-0', 'lg:grid-cols-[minmax(280px,27fr)_minmax(0,73fr)]');
    expect(conversationList).toHaveClass('lg:border-r', 'divide-y');
    expect(conversationList.querySelector('button')).toHaveClass('rounded-none', 'px-3', 'py-2');
    expect(chat).toHaveClass('bg-background');
    expect(screen.getAllByRole('group', { name: 'Message from Jane G.' })[0]).toHaveClass('rounded-lg', 'px-3', 'py-2', 'text-[13px]');
    expect(composer.parentElement).toHaveClass('flex-col', 'border-t', 'px-4', 'pt-3');
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

  it('matches the artifact balance panel and stacked wallet controls', () => {
    const adapter = createHostFixtureAdapter();
    render(<HostWalletPanel wallet={adapter.getWalletSummary().data!} statement={adapter.getStatement().data!} />);
    expect(screen.getAllByText('$3,920.50')).toHaveLength(2);
    expect(screen.getByText('SGD')).toBeInTheDocument();
    expect(screen.getByText('AVAILABLE BALANCE').parentElement).toHaveClass('bg-[var(--surface-inset)]');
    expect(screen.getByRole('button', { name: 'Top up' }).parentElement).toHaveClass('flex-col');
    expect(screen.getByRole('button', { name: 'Top up' }).parentElement?.parentElement).toHaveClass('sm:grid-cols-[minmax(0,5fr)_minmax(160px,1fr)]');
    expect(screen.getByRole('button', { name: 'Top up' }).parentElement?.parentElement).toHaveClass('items-center');
  });

  it('formats wallet transactions in an artifact-style bordered table', () => {
    const adapter = createHostFixtureAdapter();
    render(<HostWalletPanel wallet={adapter.getWalletSummary().data!} statement={adapter.getStatement().data!} />);
    const table = screen.getByRole('table');
    expect(table).toHaveClass('text-xs');
    expect(screen.getByRole('heading', { name: 'Transaction statement' })).toHaveClass('text-base');
    expect(table.querySelector('tbody td')).toHaveClass('py-2.5');
    expect(screen.getByLabelText('Transaction statement table')).toHaveClass('min-h-0', 'flex-1', 'overflow-auto');
    expect(table.querySelector('thead')).toHaveClass('sticky', 'top-0');
    expect(table.parentElement?.parentElement).toHaveClass('overflow-auto', 'rounded-xl', 'border');
    expect(screen.getByText('DATE').parentElement?.parentElement).toHaveClass('bg-[var(--surface-inset)]');
    expect(screen.getByText('ESCROW_HOLD')).toHaveClass('rounded-full');
    expect(screen.getByText('TOP_UP')).toHaveClass('rounded-full');
    expect(screen.getByText('-$210.00')).toBeInTheDocument();
    expect(screen.getByText('+$349.20')).toBeInTheDocument();
    expect(table.querySelectorAll('tbody tr')).toHaveLength(31);
  });

  it('retains mocked local wallet controls', async () => {
    const adapter = createHostFixtureAdapter();
    render(<HostWalletPanel wallet={adapter.getWalletSummary().data!} statement={adapter.getStatement().data!} />);
    fireEvent.click(screen.getByRole('button', { name: 'Top up' }));
    await waitFor(() => expect(screen.getByRole('dialog')).toHaveTextContent('no real payment gateway'));
  });
});
