import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BackendApiError } from '@/lib/api';
import { formatDateTime } from '@/lib/agent-api/format';
import type { Assistant } from '@/lib/agent-api/types';
import { DisputeDetail } from './DisputeDetail';
import { detail } from './fixtures';

const api = vi.hoisted(() => ({
  fetchDispute: vi.fn(),
  assignDispute: vi.fn(),
  unassignDispute: vi.fn(),
  saveNotes: vi.fn(),
  resolveDispute: vi.fn(),
  fetchThread: vi.fn(),
  postMessage: vi.fn(),
}));
vi.mock('@/lib/agent-api/disputes', () => api);
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => ({ signOut: vi.fn(), profile: { id: 'agent-1' } }) }));

const buttons = () => ({
  accept: screen.getByRole('button', { name: 'Accept — remedy guest' }),
  reject: screen.getByRole('button', { name: 'Reject dispute' }),
  manual: screen.getByRole('button', { name: 'Manual adjustment…' }),
});

function assistant(overrides: Partial<Assistant> = {}): Assistant {
  return { state: 'WAITING_GUEST', rounds: 2, maxRounds: 3, flagged: false, stale: false,
    draft: 'AI-assisted draft\nSummary: Reported issue.', draftUpdatedAt: '2026-10-23T04:00:00Z', ...overrides };
}

describe('DisputeDetail (F4, F14)', () => {
  beforeEach(() => {
    Object.values(api).forEach((fn) => fn.mockReset());
    api.fetchThread.mockResolvedValue([]);
  });

  it('places the assistant between the Guest and Host chats in a 40/20/40 row that resizes together', async () => {
    api.fetchDispute.mockResolvedValue(detail({ assistant: assistant() }));
    render(<DisputeDetail ticketId="t-1" />);
    const ai = await screen.findByRole('region', { name: 'AI assistant draft' });
    const guest = screen.getByRole('region', { name: 'Guest messages' });
    const host = screen.getByRole('region', { name: 'Host messages' });
    expect(guest.compareDocumentPosition(ai) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(ai.compareDocumentPosition(host) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(ai.parentElement).toHaveClass('lg:grid-cols-[2fr_1fr_2fr]');
    const grip = within(ai).getByRole('separator', { name: 'Resize AI assistant' });
    fireEvent.keyDown(grip, { key: 'ArrowDown' });
    await waitFor(() => expect(guest.style.height).toBe('476px'));
    expect([ai.style.height, host.style.height]).toEqual(['476px', '476px']);
  });

  it('keeps two equal columns without an assistant and gives notes the chat background', async () => {
    api.fetchDispute.mockResolvedValue(detail({ assistant: null }));
    render(<DisputeDetail ticketId="t-1" />);
    const notes = await screen.findByLabelText(/Internal notes/);
    expect(screen.getByRole('region', { name: 'Guest messages' }).parentElement).toHaveClass('lg:grid-cols-2');
    expect(notes).toHaveClass('bg-card');
    expect(notes).not.toHaveClass('bg-background');
  });

  it('renders a standard draft as headed table rows without losing text', async () => {
    const draft = [
      "AI-assisted draft. Everything below comes from the Guest's own statements and is unverified.",
      'Summary: Leak reported.',
      'Timeline (as reported):',
      '- 16 Oct: leak',
      'Guest claims:',
      '- (none listed)',
    ].join('\n');
    api.fetchDispute.mockResolvedValue(detail({ assistant: assistant({ draft }) }));
    render(<DisputeDetail ticketId="t-1" />);
    const panel = await screen.findByRole('region', { name: 'AI assistant draft' });
    expect(within(panel).getByRole('heading', { name: 'Summary:' })).toHaveClass('font-bold');
    expect(within(panel).getByText('Leak reported.')).toBeInTheDocument();
    expect(within(panel).getByRole('heading', { name: 'Timeline (as reported):' })).toBeInTheDocument();
    expect(within(panel).getByText('- 16 Oct: leak')).toBeInTheDocument();
    expect(within(panel).getByText('- (none listed)')).toBeInTheDocument();
  });

  it('F6 hides the assistant panel for a null projection', async () => {
    api.fetchDispute.mockResolvedValue(detail({ assistant: null }));
    render(<DisputeDetail ticketId="t-1" />);
    await screen.findByLabelText(/Internal notes/);
    expect(screen.queryByRole('region', { name: 'AI assistant draft' })).not.toBeInTheDocument();
  });

  it.each([
    ['PENDING', false, 2, "The assistant is working on the Guest's latest message."],
    ['PENDING', true, 2, 'The assistant did not finish. Continue with this ticket manually.'],
    ['WAITING_GUEST', false, 2, "Waiting for the Guest's answer (round 2 of 3)."],
    ['DONE', false, 0, 'The assistant has finished (0 question rounds).'],
    ['DONE', false, 1, 'The assistant has finished (1 question round).'],
    ['DONE', false, 2, 'The assistant has finished (2 question rounds).'],
    ['STOPPED', false, 2, 'The assistant stopped.'],
    ['UNAVAILABLE', false, 2, 'The assistant was unavailable for this ticket.'],
  ] as const)('F6 shows %s stale=%s rounds=%s with the stored draft', async (state, stale, rounds, copy) => {
    const snapshot = assistant({ state, stale, rounds });
    api.fetchDispute.mockResolvedValue(detail({ assistant: snapshot }));
    render(<DisputeDetail ticketId="t-1" />);
    const panel = await screen.findByRole('region', { name: 'AI assistant draft' });
    expect(within(panel).getByText(copy)).toBeInTheDocument();
    expect(within(panel).getByText(`AI-assisted draft (unverified, updated ${formatDateTime(snapshot.draftUpdatedAt!)})`)).toBeInTheDocument();
    expect(within(panel).getByRole('button', { name: 'Insert into notes' })).toBeEnabled();
    const notes = screen.getByLabelText(/Internal notes/);
    expect(panel.compareDocumentPosition(notes) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('F6 shows the injection warning above the status and no insert for an absent draft', async () => {
    api.fetchDispute.mockResolvedValue(detail({ assistant: assistant({ flagged: true, draft: null, draftUpdatedAt: null }) }));
    render(<DisputeDetail ticketId="t-1" />);
    const panel = await screen.findByRole('region', { name: 'AI assistant draft' });
    const warning = within(panel).getByText('Warning: text in this ticket matched a prompt-injection pattern. Treat the draft with care.');
    const status = within(panel).getByText("Waiting for the Guest's answer (round 2 of 3).");
    expect(warning.compareDocumentPosition(status) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(within(panel).getByText('No draft yet.')).toBeInTheDocument();
    expect(within(panel).queryByRole('button', { name: 'Insert into notes' })).not.toBeInTheDocument();
  });

  it.each(['PENDING', 'WAITING_GUEST'] as const)('E59 renders resolved %s as stopped with insertion disabled', async (state) => {
    api.fetchDispute.mockResolvedValue(detail({ assistant: assistant({ state, stale: true }) }, { status: 'RESOLVED_APPROVED' }));
    render(<DisputeDetail ticketId="t-1" />);
    const panel = await screen.findByRole('region', { name: 'AI assistant draft' });
    expect(within(panel).getByText('The assistant stopped.')).toBeInTheDocument();
    expect(within(panel).getByRole('button', { name: 'Insert into notes' })).toBeDisabled();
    expect(within(panel).getByRole('button', { name: 'Insert into notes' })).toHaveAttribute('title', 'Assign the ticket to yourself to edit notes.');
  });

  it('E39 renders hostile HTML and Markdown in the draft as pre-wrapped text', async () => {
    const draft = '<img src=x onerror=alert(1)>\n**bold**';
    api.fetchDispute.mockResolvedValue(detail({ assistant: assistant({ draft }) }));
    render(<DisputeDetail ticketId="t-1" />);
    const panel = await screen.findByRole('region', { name: 'AI assistant draft' });
    expect(within(panel).getByText(/\*\*bold\*\*/)).toHaveClass('whitespace-pre-wrap');
    expect(within(panel).getByText(/\*\*bold\*\*/).textContent).toBe(draft);
    expect(panel.querySelector('img, strong, a')).toBeNull();
  });

  it('E89 shows the stored fixed-email UUID instruction in the Agent Guest thread without an anchor', async () => {
    const ticketId = 'd0000000-0000-4000-8000-000000000089';
    const body = `To help our support team review your ticket, please answer these in one message:\n1. Please email supporting documents to support@snoozeshare.com with the subject "${ticketId}: Supporting Documents".`;
    api.fetchDispute.mockResolvedValue(detail({ assistant: assistant() }, { id: ticketId }));
    api.fetchThread.mockImplementation((_ticket, channel) => Promise.resolve(channel === 'GUEST'
      ? [{ id: 'system', authorId: 'system', authorRole: 'SYSTEM', body, sentAt: '2026-10-23T04:00:00Z' }] : []));
    render(<DisputeDetail ticketId={ticketId} />);
    const panel = await screen.findByRole('region', { name: 'Guest messages' });
    const text = await within(panel).findByText(/Please email supporting documents/);
    expect(text.textContent).toBe(body);
    expect(text.closest('article')).toHaveClass('self-end');
    expect(panel.textContent).not.toContain('[supporting-documents]');
    expect(panel.querySelector('a')).toBeNull();
  });

  it.each(['', 'existing notes'])('E68 inserts the entire draft into %s locally and saves only explicitly', async (existing) => {
    const draft = ' draft with whitespace\nsecond line ';
    api.fetchDispute.mockResolvedValue(detail({ agentNotes: existing, assistant: assistant({ draft }) }));
    api.saveNotes.mockResolvedValue(detail({ agentNotes: existing ? `${existing}\n\n${draft}` : draft, assistant: assistant({ draft }) }));
    render(<DisputeDetail ticketId="t-1" />);
    const insert = await screen.findByRole('button', { name: 'Insert into notes' });
    const box = screen.getByLabelText(/Internal notes/);
    await waitFor(() => expect(box).toHaveValue(existing));
    fireEvent.click(insert);
    const expected = existing ? `${existing}\n\n${draft}` : draft;
    expect(box).toHaveValue(expected);
    expect(api.saveNotes).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: 'Save notes' }));
    await waitFor(() => expect(api.saveNotes).toHaveBeenCalledWith('t-1', expected));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Save notes' })).toBeDisabled());
  });

  it.each([
    ['OPEN', null], ['IN_REVIEW', 'agent-2'], ['RESOLVED_REJECTED', 'agent-1'],
  ] as const)('E68 disables insertion for %s assigned to %s', async (status, assignedAgentId) => {
    api.fetchDispute.mockResolvedValue(detail({ assistant: assistant() }, { status, assignedAgentId }));
    render(<DisputeDetail ticketId="t-1" />);
    const insert = await screen.findByRole('button', { name: 'Insert into notes' });
    expect(insert).toBeDisabled();
    expect(insert).toHaveAttribute('title', 'Assign the ticket to yourself to edit notes.');
  });

  it('E68 disables insertion while an action is busy', async () => {
    api.fetchDispute.mockResolvedValue(detail({ assistant: assistant() }));
    api.saveNotes.mockReturnValue(new Promise(() => {}));
    render(<DisputeDetail ticketId="t-1" />);
    const insert = await screen.findByRole('button', { name: 'Insert into notes' });
    fireEvent.click(insert);
    fireEvent.click(screen.getByRole('button', { name: 'Save notes' }));
    expect(insert).toBeDisabled();
    expect(insert).toHaveAttribute('title', 'Assign the ticket to yourself to edit notes.');
  });

  it('E68 keeps inserted dirty notes through polling and retains the full over-limit draft on save failure', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    try {
      const draft = 'x'.repeat(6000);
      api.fetchDispute.mockResolvedValue(detail({ agentNotes: 'local start', assistant: assistant({ draft }) }));
      api.saveNotes.mockRejectedValue(new BackendApiError('VALIDATION_ERROR', 'Notes must be at most 4000 characters', undefined, 400));
      render(<DisputeDetail ticketId="t-1" />);
      const insert = await screen.findByRole('button', { name: 'Insert into notes' });
      const box = screen.getByLabelText(/Internal notes/);
      await waitFor(() => expect(box).toHaveValue('local start'));
      await act(async () => { fireEvent.click(insert); });
      const expected = `local start\n\n${draft}`;
      expect(box).toHaveValue(expected);
      expect(box).toHaveAttribute('maxlength', '4000');
      expect(box).toBeEnabled();
      api.fetchDispute.mockResolvedValue(detail({ agentNotes: 'changed remotely', assistant: assistant({ draft: 'new draft' }) }));
      await act(async () => { await vi.advanceTimersByTimeAsync(10500); });
      expect(box).toHaveValue(expected);
      fireEvent.click(screen.getByRole('button', { name: 'Save notes' }));
      expect(await screen.findByRole('alert')).toHaveTextContent('Notes must be at most 4000 characters');
      expect(box).toHaveValue(expected);
      expect(screen.getByRole('button', { name: 'Save notes' })).toBeEnabled();
      fireEvent.change(box, { target: { value: 'edited down' } });
      expect(box).toHaveValue('edited down');
    } finally {
      vi.useRealTimers();
    }
  });

  it('unassigned: Assign is offered, actions are disabled', async () => {
    api.fetchDispute.mockResolvedValue(detail({ assignedAgentName: null }, { status: 'OPEN', assignedAgentId: null }));
    render(<DisputeDetail ticketId="t-1" />);

    await screen.findByRole('button', { name: 'Assign to me' });
    expect(screen.getByText('Assign this ticket to reply or resolve.')).toBeInTheDocument();
    expect(buttons().accept).toBeDisabled();
    expect(screen.getByRole('textbox', { name: 'Reply to Gina…' })).toBeDisabled();
  });

  it('mine with escrow held: everything is enabled', async () => {
    api.fetchDispute.mockResolvedValue(detail());
    render(<DisputeDetail ticketId="t-1" />);

    await screen.findByText('Amount held in escrow');
    expect(screen.getByText('SGD 120.00')).toBeInTheDocument();
    expect(buttons().accept).toBeEnabled();
    expect(screen.queryByRole('button', { name: 'Assign to me' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Unassign' })).toBeEnabled();
  });

  it('actions are disabled once the escrow is released (E72)', async () => {
    api.fetchDispute.mockResolvedValue(detail({ escrowHeld: false }));
    render(<DisputeDetail ticketId="t-1" />);

    await screen.findByText('Escrow already released.');
    expect(buttons().reject).toBeDisabled();
    expect(screen.getByText('Settled')).toBeInTheDocument();
  });

  it('another agent: can reply, cannot resolve', async () => {
    api.fetchDispute.mockResolvedValue(detail({ assignedAgentName: 'Bo' }, { assignedAgentId: 'agent-2' }));
    render(<DisputeDetail ticketId="t-1" />);

    await screen.findByText('Assigned to Bo.');
    expect(buttons().manual).toBeDisabled();
    expect(screen.getByRole('textbox', { name: 'Reply to Gina…' })).toBeEnabled();
  });

  it('resolved: read-only with a banner', async () => {
    api.fetchDispute.mockResolvedValue(detail({}, { status: 'RESOLVED_REJECTED', resolutionReason: 'No evidence' }));
    render(<DisputeDetail ticketId="t-1" />);

    expect(await screen.findByRole('status')).toHaveTextContent('Resolved (rejected)');
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

  it('unassigns a ticket the agent holds', async () => {
    api.fetchDispute.mockResolvedValue(detail());
    api.unassignDispute.mockResolvedValue(detail({ assignedAgentName: null }, { status: 'OPEN', assignedAgentId: null }));
    render(<DisputeDetail ticketId="t-1" />);

    fireEvent.click(await screen.findByRole('button', { name: 'Unassign' }));
    await waitFor(() => expect(api.unassignDispute).toHaveBeenCalledWith('t-1'));
  });

  it("shows another agent's ticket with a disabled assignment button", async () => {
    api.fetchDispute.mockResolvedValue(detail({ assignedAgentName: 'Bo' }, { assignedAgentId: 'agent-2' }));
    render(<DisputeDetail ticketId="t-1" />);

    expect(await screen.findByRole('button', { name: 'Assign to me' })).toBeDisabled();
  });

  it('lists a thread oldest first even though the API sends it newest first', async () => {
    api.fetchDispute.mockResolvedValue(detail());
    api.fetchThread.mockImplementation((_ticket: string, channel: string) =>
      Promise.resolve(channel === 'GUEST' ? [
        { id: 'm3', authorId: 'a', authorRole: 'AGENT', body: 'third', sentAt: '2026-10-01T06:00:00.500Z' },
        { id: 'm2', authorId: 'g', authorRole: 'GUEST', body: 'second', sentAt: '2026-10-01T05:00:00Z' },
        { id: 'm1', authorId: 'g', authorRole: 'GUEST', body: 'first', sentAt: '2026-10-01T05:00:00.250Z' },
      ] : []));
    render(<DisputeDetail ticketId="t-1" />);

    const panel = await screen.findByRole('region', { name: 'Guest messages' });
    await within(panel).findByText('third');
    const order = within(panel).getAllByText(/^(first|second|third)$/).map((node) => node.textContent);
    expect(order).toEqual(['second', 'first', 'third']);
  });

  it('resizes both chat boxes together with the arrow keys within the desktop limits', async () => {
    api.fetchDispute.mockResolvedValue(detail());
    render(<DisputeDetail ticketId="t-1" />);

    const grip = await screen.findByRole('separator', { name: 'Resize guest messages' });
    expect(grip).toHaveAttribute('aria-valuenow', '460');
    fireEvent.keyDown(grip, { key: 'ArrowDown' });
    expect(screen.getByRole('separator', { name: 'Resize host messages' })).toHaveAttribute('aria-valuenow', '476');
    for (let i = 0; i < 40; i += 1) fireEvent.keyDown(grip, { key: 'ArrowUp' });
    expect(grip).toHaveAttribute('aria-valuenow', '240');
  });

  it('resizes the notes box between 56 and 400', async () => {
    api.fetchDispute.mockResolvedValue(detail());
    render(<DisputeDetail ticketId="t-1" />);

    const grip = await screen.findByRole('separator', { name: 'Resize notes' });
    expect(grip).toHaveAttribute('aria-valuenow', '60');
    for (let i = 0; i < 40; i += 1) fireEvent.keyDown(grip, { key: 'ArrowDown' });
    expect(grip).toHaveAttribute('aria-valuenow', '400');
  });
});
