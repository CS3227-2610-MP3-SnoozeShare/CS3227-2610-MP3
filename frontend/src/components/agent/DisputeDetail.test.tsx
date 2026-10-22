import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BackendApiError } from '@/lib/api';
import { DisputeDetail } from './DisputeDetail';
import { detail } from './fixtures';

const api = vi.hoisted(() => ({
  fetchDispute: vi.fn(),
  assignDispute: vi.fn(),
  saveNotes: vi.fn(),
  resolveDispute: vi.fn(),
  fetchThread: vi.fn(),
  postMessage: vi.fn(),
}));
vi.mock('@/lib/agent-api/disputes', () => api);
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => ({ signOut: vi.fn(), profile: { id: 'agent-1' } }) }));

const buttons = () => ({
  accept: screen.getByRole('button', { name: 'Accept - remedy guest' }),
  reject: screen.getByRole('button', { name: 'Reject dispute' }),
  manual: screen.getByRole('button', { name: 'Manual adjustment...' }),
});

describe('DisputeDetail (F4, F14)', () => {
  beforeEach(() => {
    Object.values(api).forEach((fn) => fn.mockReset());
    api.fetchThread.mockResolvedValue([]);
  });

  it('unassigned: Assign is offered, actions are disabled', async () => {
    api.fetchDispute.mockResolvedValue(detail({ assignedAgentName: null }, { status: 'OPEN', assignedAgentId: null }));
    render(<DisputeDetail ticketId="t-1" />);

    await screen.findByRole('button', { name: 'Assign to me' });
    expect(screen.getByText('Assign this ticket to reply or resolve.')).toBeInTheDocument();
    expect(buttons().accept).toBeDisabled();
    expect(screen.getByRole('textbox', { name: 'Reply to guest...' })).toBeDisabled();
  });

  it('mine with escrow held: everything is enabled', async () => {
    api.fetchDispute.mockResolvedValue(detail());
    render(<DisputeDetail ticketId="t-1" />);

    await screen.findByText('Amount held in escrow');
    expect(screen.getByText('SGD 120.00')).toBeInTheDocument();
    expect(buttons().accept).toBeEnabled();
    expect(screen.queryByRole('button', { name: 'Assign to me' })).toBeNull();
  });

  it('actions are disabled once the escrow is released (E72)', async () => {
    api.fetchDispute.mockResolvedValue(detail({ escrowHeld: false }));
    render(<DisputeDetail ticketId="t-1" />);

    await screen.findByText('Escrow already released.');
    expect(buttons().reject).toBeDisabled();
    expect(screen.getByText('Released')).toBeInTheDocument();
  });

  it('another agent: can reply, cannot resolve', async () => {
    api.fetchDispute.mockResolvedValue(detail({ assignedAgentName: 'Bo' }, { assignedAgentId: 'agent-2' }));
    render(<DisputeDetail ticketId="t-1" />);

    await screen.findByText('Assigned to Bo.');
    expect(buttons().manual).toBeDisabled();
    expect(screen.getByRole('textbox', { name: 'Reply to guest...' })).toBeEnabled();
  });

  it('resolved: read-only with a banner', async () => {
    api.fetchDispute.mockResolvedValue(detail({}, { status: 'RESOLVED_REJECTED', resolutionReason: 'No evidence' }));
    render(<DisputeDetail ticketId="t-1" />);

    expect(await screen.findByRole('status')).toHaveTextContent('Resolved - rejected');
    expect(screen.getAllByText('Ticket is resolved')).toHaveLength(2);
    expect(screen.queryByRole('button', { name: 'Reject dispute' })).toBeNull();
  });

  it('keeps unsaved notes when a poll brings new data (E61)', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    try {
      api.fetchDispute.mockResolvedValue(detail({ agentNotes: 'server note' }));
      render(<DisputeDetail ticketId="t-1" />);
      const box = (await screen.findByLabelText(/Internal notes/)) as HTMLTextAreaElement;
      await waitFor(() => expect(box.value).toBe('server note'));

      fireEvent.change(box, { target: { value: 'my draft' } });
      api.fetchDispute.mockResolvedValue(detail({ agentNotes: 'changed elsewhere' }));
      await vi.advanceTimersByTimeAsync(10500);

      expect(box.value).toBe('my draft');
    } finally {
      vi.useRealTimers();
    }
  });

  it('shows a not-found banner (404)', async () => {
    api.fetchDispute.mockRejectedValue(new BackendApiError('NOT_FOUND', 'Ticket does not exist', undefined, 404));
    render(<DisputeDetail ticketId="t-1" />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Ticket does not exist');
    expect(screen.getByRole('link', { name: 'Back to Disputes' })).toBeInTheDocument();
  });

  it('assigns the ticket', async () => {
    api.fetchDispute.mockResolvedValue(detail({ assignedAgentName: null }, { status: 'OPEN', assignedAgentId: null }));
    api.assignDispute.mockResolvedValue(detail());
    render(<DisputeDetail ticketId="t-1" />);

    fireEvent.click(await screen.findByRole('button', { name: 'Assign to me' }));
    await waitFor(() => expect(api.assignDispute).toHaveBeenCalledWith('t-1'));
  });

  it('renders ticket, note and message text literally (E36)', async () => {
    const hostile = '<img src=x onerror=alert(1)>';
    api.fetchDispute.mockResolvedValue(detail({ agentNotes: hostile }, { title: hostile }));
    api.fetchThread.mockResolvedValue([{ id: 'm', authorId: 'g', authorRole: 'GUEST', body: hostile, sentAt: '2026-10-01T04:00:00Z' }]);
    const { container } = render(<DisputeDetail ticketId="t-1" />);

    await waitFor(() => expect(screen.getAllByText(hostile).length).toBeGreaterThan(1));
    expect(container.querySelector('img')).toBeNull();
  });
});
