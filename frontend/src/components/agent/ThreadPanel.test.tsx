import { render, screen, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ThreadPanel } from './ThreadPanel';
import { detail } from './fixtures';

const api = vi.hoisted(() => ({ fetchThread: vi.fn(), postMessage: vi.fn() }));
vi.mock('@/lib/agent-api/disputes', () => api);

describe('ThreadPanel assistant presentation', () => {
  beforeEach(() => {
    api.fetchThread.mockReset();
    api.postMessage.mockReset();
  });

  it('E89/F7 labels SYSTEM, shows the exact stored email line literally on the left, and isolates Host messages', async () => {
    const ticketId = 'd0000000-0000-4000-8000-000000000089';
    const body = `To help our support team review your ticket, please answer these in one message:\n1. Please email supporting documents to support@snoozeshare.com with the subject "${ticketId}: Supporting Documents".\n2. When did it start?`;
    api.fetchThread.mockImplementation((_ticket, channel) => Promise.resolve(channel === 'GUEST'
      ? [{ id: 'system', authorId: 'system', authorRole: 'SYSTEM', body, sentAt: '2026-10-23T04:00:00Z' }]
      : [{ id: 'host', authorId: 'host', authorRole: 'HOST', body: 'Host answer', sentAt: '2026-10-23T04:00:00Z' }]));
    const snapshot = detail({}, { id: ticketId });
    render(<><ThreadPanel detail={snapshot} channel="GUEST" canSend resolved={false} height={460} onResize={vi.fn()} />
      <ThreadPanel detail={snapshot} channel="HOST" canSend resolved={false} height={460} onResize={vi.fn()} /></>);
    const guest = screen.getByRole('region', { name: 'Guest messages' });
    const label = await within(guest).findByText('Snoozy · Automated assistant');
    expect(label.closest('article')).toHaveClass('self-start');
    const text = within(guest).getByText(/Please email supporting documents/);
    expect(text.textContent).toBe(body);
    expect(text).toHaveClass('whitespace-pre-wrap');
    expect(guest.textContent).not.toContain('[supporting-documents]');
    expect(guest.querySelector('a')).toBeNull();
    const host = screen.getByRole('region', { name: 'Host messages' });
    expect(await within(host).findByText('Host answer')).toBeInTheDocument();
    expect(within(host).queryByText('Snoozy · Automated assistant')).not.toBeInTheDocument();
    expect(host.textContent).not.toContain('support@snoozeshare.com');
    expect(api.fetchThread).toHaveBeenCalledWith(ticketId, 'GUEST');
    expect(api.fetchThread).toHaveBeenCalledWith(ticketId, 'HOST');
  });

  it('E39 keeps hostile SYSTEM body literal and ordinary Agent messages right aligned', async () => {
    const body = '<img src=x onerror=alert(1)>\n**bold**';
    api.fetchThread.mockResolvedValue([
      { id: 'system', authorId: 'system', authorRole: 'SYSTEM', body, sentAt: '2026-10-23T04:00:00Z' },
      { id: 'agent', authorId: 'agent', authorRole: 'AGENT', body: 'Agent reply', sentAt: '2026-10-23T04:01:00Z' },
    ]);
    const { container } = render(<ThreadPanel detail={detail()} channel="GUEST" canSend resolved={false} height={460} onResize={vi.fn()} />);
    const text = await screen.findByText(/\*\*bold\*\*/);
    expect(text.textContent).toBe(body);
    expect(text).toHaveClass('whitespace-pre-wrap');
    expect(screen.getByText('Support agent').closest('article')).toHaveClass('self-end');
    expect(container.querySelector('img, strong, a')).toBeNull();
  });
});
