import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
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

describe('AuditLogView (F11, F15)', () => {
  const audit = vi.fn();
  beforeEach(() => {
    audit.mockReset();
    api.audit = audit;
    api.actions = () => Promise.resolve(['TOP_UP', 'BOOKING_FORCE_CANCELLED']);
  });

  it('loads the action list from the backend and formats rows', async () => {
    audit.mockResolvedValue([
      entry(1, { walletAdjustment: 40, subjectName: 'Gina Guest', reason: '<img src=x onerror=alert(1)>' }),
      entry(2, { beforeState: null, afterState: 'ACTIVE' }),
    ]);
    const { container } = render(<AuditLogView />);

    expect(await screen.findByText('Gina Guest')).toBeInTheDocument();
    expect(screen.getByText('+SGD 40.00')).toBeInTheDocument();
    expect(screen.getByText('IN_REVIEW -> RESOLVED_APPROVED')).toBeInTheDocument();
    expect(screen.getByText('Ticket 12345678')).toBeInTheDocument();
    expect(screen.getByText('<img src=x onerror=alert(1)>')).toBeInTheDocument(); // E36
    expect(container.querySelector('img')).toBeNull();
    expect(await screen.findByRole('option', { name: 'BOOKING_FORCE_CANCELLED' })).toBeInTheDocument();
  });

  it('Apply reloads from offset 0 with the filters; Clear resets', async () => {
    audit.mockResolvedValue([]);
    render(<AuditLogView />);
    expect(await screen.findByText('No audit entries match')).toBeInTheDocument(); // E50

    fireEvent.change(screen.getByLabelText('Search'), { target: { value: 'gina' } });
    fireEvent.change(screen.getByLabelText('From'), { target: { value: '2026-10-01' } });
    fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }));
    await waitFor(() => expect(audit).toHaveBeenLastCalledWith({ text: 'gina', action: '', from: '2026-10-01', to: '' }, 0));

    fireEvent.click(screen.getByRole('button', { name: 'Clear' }));
    await waitFor(() => expect(audit).toHaveBeenLastCalledWith({ text: '', action: '', from: '', to: '' }, 0));
    expect((screen.getByLabelText('Search') as HTMLInputElement).value).toBe('');
  });

  it('shows Load more only after a full page and appends the next one', async () => {
    const page = Array.from({ length: 200 }, (_, n) => entry(n));
    audit.mockResolvedValueOnce(page).mockResolvedValueOnce([entry(200)]);
    render(<AuditLogView />);

    fireEvent.click(await screen.findByRole('button', { name: 'Load more' }));
    await waitFor(() => expect(audit).toHaveBeenLastCalledWith(expect.anything(), 200));
    await waitFor(() => expect(screen.getAllByRole('row')).toHaveLength(202));
    expect(screen.queryByRole('button', { name: 'Load more' })).toBeNull();
  });

  it('a new Apply replaces the list instead of appending (E49)', async () => {
    audit.mockResolvedValueOnce([entry(1)]).mockResolvedValueOnce([entry(2, { subjectName: 'Only me' })]);
    render(<AuditLogView />);
    await screen.findAllByText('Ann Agent');

    fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }));

    expect(await screen.findByText('Only me')).toBeInTheDocument();
    expect(screen.getAllByRole('row')).toHaveLength(2);
  });

  it('disables Apply when From is after To', async () => {
    audit.mockResolvedValue([]);
    render(<AuditLogView />);
    await screen.findByText('No audit entries match');

    fireEvent.change(screen.getByLabelText('From'), { target: { value: '2026-10-05' } });
    fireEvent.change(screen.getByLabelText('To'), { target: { value: '2026-10-01' } });

    expect(screen.getByRole('button', { name: 'Apply filters' })).toBeDisabled();
    expect(screen.getByText('From must not be after To')).toBeInTheDocument();
  });

  it('replaces the page with the session notice on a 403 (E45)', async () => {
    api.audit = () => Promise.reject(new BackendApiError('PROFILE_INACTIVE', 'x', undefined, 403));
    render(<AuditLogView />);

    expect(await screen.findByText('Your account is suspended.')).toBeInTheDocument();
    expect(screen.queryByRole('table')).toBeNull();
  });
});
