import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { BackendApiError } from '@/lib/api';
import type { AuditEntry } from '@/lib/agent-api/types';
import { AuditLogView } from './AuditLogView';

const api = vi.hoisted(() => ({
  audit: undefined as undefined | ((...args: unknown[]) => Promise<unknown>),
  actions: undefined as undefined | (() => Promise<unknown>),
}));
vi.mock('@/lib/agent-api/audit', () => ({
  AUDIT_PAGE_SIZE: 200,
  fetchAudit: (...args: unknown[]) => api.audit!(...args),
  fetchAuditActions: () => api.actions!(),
}));
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => ({ signOut: vi.fn() }) }));

function entry(n: number, overrides: Partial<AuditEntry> = {}): AuditEntry {
  return {
    id: `e-${n}`,
    timestamp: '2026-10-21T23:30:00Z',
    actorName: 'Ann Agent',
    actionType: 'TICKET_RESOLVED',
    entityType: 'Ticket',
    entityId: '12345678-aaaa-bbbb-cccc-1234567890ab',
    beforeState: 'IN_REVIEW',
    afterState: 'RESOLVED_APPROVED',
    walletAdjustment: null,
    balanceAfter: null,
    reason: null,
    subjectName: null,
    bookingId: null,
    ticketId: null,
    ...overrides,
  };
}

const EMPTY = { text: '', actions: [], from: '', to: '' };

/** `yyyy-MM-dd` of a day in the month the date picker opens on (the current one). */
function dayOfThisMonth(day: number): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

function pick(label: 'From' | 'To', day: number) {
  fireEvent.click(screen.getByRole('button', { name: label }));
  fireEvent.click(screen.getByRole('button', { name: dayOfThisMonth(day) }));
}

describe('AuditLogView (F11, F15)', () => {
  const audit = vi.fn();
  beforeEach(() => {
    audit.mockReset();
    api.audit = audit;
    api.actions = () => Promise.resolve(['TOP_UP', 'BOOKING_FORCE_CANCELLED', 'TICKET_RESOLVED']);
  });
  afterEach(() => vi.useRealTimers());

  it('formats rows the way the desktop does', async () => {
    audit.mockResolvedValue([
      entry(1, { walletAdjustment: 40, subjectName: 'Gina Guest', entityType: 'WalletTransaction', reason: '<img src=x onerror=alert(1)>' }),
      entry(2, { beforeState: null, afterState: 'ACTIVE', actorName: null }),
    ]);
    const { container } = render(<AuditLogView />);

    expect(await screen.findByText('Gina Guest')).toBeInTheDocument();
    expect(screen.getByText('+SGD 40.00')).toBeInTheDocument();
    expect(screen.getByText('In review → Resolved approved')).toBeInTheDocument();
    expect(screen.getByText('Ticket #90ab')).toBeInTheDocument();
    expect(screen.getByText('Unknown user')).toBeInTheDocument();
    expect(screen.getByText('<img src=x onerror=alert(1)>')).toBeInTheDocument(); // E36
    expect(container.querySelector('img')).toBeNull();
    expect(screen.getAllByText(/^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/).length).toBe(2);
  });

  it('has no Apply button and filters live as the search text is typed', async () => {
    audit.mockResolvedValue([]);
    render(<AuditLogView />);
    expect(await screen.findByText('No audit entries match these filters.')).toBeInTheDocument(); // E50
    expect(screen.queryByRole('button', { name: /apply/i })).toBeNull();
    vi.useFakeTimers({ shouldAdvanceTime: true });

    fireEvent.change(screen.getByPlaceholderText(/User name/), { target: { value: 'gina' } });
    expect(audit).toHaveBeenCalledTimes(1);
    await act(async () => { await vi.advanceTimersByTimeAsync(350); });

    expect(audit).toHaveBeenLastCalledWith({ ...EMPTY, text: 'gina' }, 0);
  });

  it('applies a date change at once and Clear resets every filter', async () => {
    audit.mockResolvedValue([]);
    render(<AuditLogView />);
    await screen.findByText('No audit entries match these filters.');

    pick('From', 1);
    await waitFor(() => expect(audit).toHaveBeenLastCalledWith({ ...EMPTY, from: dayOfThisMonth(1) }, 0));

    fireEvent.click(screen.getByRole('button', { name: 'Clear' }));
    await waitFor(() => expect(audit).toHaveBeenLastCalledWith(EMPTY, 0));
    expect(screen.getByRole('button', { name: 'From' })).toHaveTextContent('dd/mm/yyyy');
  });

  it('takes several action types from the multi-select, listed from the backend', async () => {
    audit.mockResolvedValue([]);
    render(<AuditLogView />);
    await screen.findByText('No audit entries match these filters.');

    fireEvent.click(screen.getByRole('button', { name: 'Action type' }));
    expect(await screen.findByLabelText('BOOKING_FORCE_CANCELLED')).toBeInTheDocument();
    fireEvent.click(screen.getByLabelText('TOP_UP'));
    fireEvent.click(screen.getByLabelText('BOOKING_FORCE_CANCELLED'));

    await waitFor(() => expect(audit).toHaveBeenLastCalledWith({ ...EMPTY, actions: ['TOP_UP', 'BOOKING_FORCE_CANCELLED'] }, 0));
    expect(screen.getByRole('button', { name: 'Action type' })).toHaveTextContent('2 selected');
  });

  it('lists, shows and filters the assistant actions (E62)', async () => {
    api.actions = () => Promise.resolve(['ASSISTANT_STATE', 'ASSISTANT_MESSAGE', 'TOP_UP']);
    audit.mockResolvedValue([
      entry(1, { actionType: 'ASSISTANT_STATE', actorName: 'System', beforeState: 'PENDING', afterState: 'WAITING_GUEST' }),
      entry(2, { actionType: 'ASSISTANT_MESSAGE', actorName: 'System', entityType: 'Message', beforeState: null, afterState: 'QUESTION' }),
    ]);
    render(<AuditLogView />);

    expect(await screen.findByText('ASSISTANT_STATE')).toBeInTheDocument();
    expect(screen.getByText('ASSISTANT_MESSAGE')).toBeInTheDocument();
    expect(screen.getByText('Pending → Waiting guest')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Action type' }));
    fireEvent.click(await screen.findByLabelText('ASSISTANT_STATE'));
    await waitFor(() => expect(audit).toHaveBeenLastCalledWith({ ...EMPTY, actions: ['ASSISTANT_STATE'] }, 0));
  });

  it('shows Load more only after a full page and appends the next one', async () => {
    const page = Array.from({ length: 200 }, (_, n) => entry(n));
    audit.mockResolvedValueOnce(page).mockResolvedValueOnce([entry(200)]);
    render(<AuditLogView />);

    fireEvent.click(await screen.findByRole('button', { name: 'Load more' }));
    await waitFor(() => expect(audit).toHaveBeenLastCalledWith(EMPTY, 200));
    await waitFor(() => expect(screen.getAllByRole('row')).toHaveLength(202));
    expect(screen.queryByRole('button', { name: 'Load more' })).toBeNull();
  });

  it('a changed filter replaces the list instead of appending (E49)', async () => {
    audit.mockResolvedValueOnce([entry(1)]).mockResolvedValueOnce([entry(2, { subjectName: 'Only me', entityType: 'User' })]);
    render(<AuditLogView />);
    await screen.findAllByText('Ann Agent');

    pick('To', 28);

    expect(await screen.findByText('Only me')).toBeInTheDocument();
    expect(screen.getAllByRole('row')).toHaveLength(2);
  });

  it('asks for nothing while From is after To', async () => {
    audit.mockResolvedValue([]);
    render(<AuditLogView />);
    await screen.findByText('No audit entries match these filters.');
    const calls = audit.mock.calls.length;

    pick('From', 20);
    await waitFor(() => expect(audit.mock.calls.length).toBe(calls + 1));
    pick('To', 10);

    expect(await screen.findByText('The From date must not be after the To date.')).toBeInTheDocument();
    await new Promise((resolve) => setTimeout(resolve, 50));
    expect(audit.mock.calls.length).toBe(calls + 1);
  });

  it('replaces the page with the session notice on a 403 (E45)', async () => {
    api.audit = () => Promise.reject(new BackendApiError('PROFILE_INACTIVE', 'x', undefined, 403));
    render(<AuditLogView />);

    expect(await screen.findByText('Your account is suspended.')).toBeInTheDocument();
    expect(screen.queryByRole('table')).toBeNull();
  });
});
