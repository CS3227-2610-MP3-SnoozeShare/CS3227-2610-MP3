import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BackendApiError } from '@/lib/api';
import type { AccountSummary } from '@/lib/agent-api/types';
import { AccountsTable } from './AccountsTable';

const api = vi.hoisted(() => ({
  list: undefined as undefined | (() => Promise<unknown>),
  set: undefined as undefined | ((...args: unknown[]) => Promise<unknown>),
}));
vi.mock('@/lib/agent-api/accounts', () => ({
  fetchAccounts: () => api.list!(),
  setAccountSuspended: (...args: unknown[]) => api.set!(...args),
}));
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => ({ signOut: vi.fn() }) }));

function account(overrides: Partial<AccountSummary> = {}): AccountSummary {
  return {
    userId: 'u-1',
    displayName: 'Gina Guest',
    email: 'gina@example.com',
    role: 'GUEST',
    createdAt: '2026-03-10T04:00:00Z',
    status: 'ACTIVE',
    suspensionReason: null,
    ...overrides,
  };
}

describe('AccountsTable (F9, F10, F15)', () => {
  const set = vi.fn();
  beforeEach(() => {
    set.mockReset();
    api.set = set;
    api.list = () => Promise.resolve([account(), account({ userId: 'u-2', displayName: 'Hal Host', email: 'hal@example.com', role: 'HOST', status: 'SUSPENDED', suspensionReason: '<img src=x onerror=alert(1)>' })]);
  });

  it('shows columns, status pills and the right action per row', async () => {
    const { container } = render(<AccountsTable />);

    expect(await screen.findByText('Gina Guest')).toBeInTheDocument();
    expect(screen.getAllByText('Mar 2026')).toHaveLength(2);
    expect(screen.getByText('Active')).toBeInTheDocument();
    const rows = screen.getAllByRole('row');
    expect(within(rows[1]).getByRole('button', { name: 'Suspend' })).toBeInTheDocument();
    expect(within(rows[1]).queryByRole('button', { name: 'Reactivate' })).toBeNull();
    expect(within(rows[2]).getByRole('button', { name: 'Reactivate' })).toBeInTheDocument();
    expect(screen.getByText('Reason: <img src=x onerror=alert(1)>')).toBeInTheDocument(); // E36
    expect(container.querySelector('img')).toBeNull();
  });

  it('filters by name or email and shows the empty copy (E50)', async () => {
    render(<AccountsTable />);
    await screen.findByText('Gina Guest');

    fireEvent.change(screen.getByLabelText('Search accounts'), { target: { value: 'HAL@' } });
    expect(screen.queryByText('Gina Guest')).toBeNull();
    expect(screen.getByText('Hal Host')).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Search accounts'), { target: { value: 'zzz' } });
    expect(screen.getByText('No accounts match your search')).toBeInTheDocument();
  });

  it('needs a reason, then suspends and reloads', async () => {
    set.mockResolvedValue(account({ status: 'SUSPENDED' }));
    render(<AccountsTable />);
    fireEvent.click(await screen.findByRole('button', { name: 'Suspend' }));

    const dialog = screen.getByRole('dialog', { name: 'Suspend account' });
    expect(within(dialog).getByText('Pending and upcoming bookings will be cancelled and refunded.')).toBeInTheDocument();
    const confirm = within(dialog).getByRole('button', { name: 'Confirm suspend' });
    expect(confirm).toBeDisabled();
    fireEvent.change(within(dialog).getByLabelText(/^Reason/), { target: { value: 'Fraud' } });
    fireEvent.click(confirm);

    await waitFor(() => expect(set).toHaveBeenCalledWith('u-1', true, 'Fraud'));
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
  });

  it('shows a 409 inline and keeps the dialog open', async () => {
    set.mockRejectedValue(new BackendApiError('CONFLICT', 'Account is already suspended', undefined, 409));
    render(<AccountsTable />);
    fireEvent.click(await screen.findByRole('button', { name: 'Reactivate' }));
    fireEvent.change(screen.getByLabelText(/^Reason/), { target: { value: 'Appeal' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirm reactivate' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Account is already suspended');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('replaces the page with the session notice on a 403 PROFILE_INACTIVE (E45)', async () => {
    api.list = () => Promise.reject(new BackendApiError('PROFILE_INACTIVE', 'x', undefined, 403));
    render(<AccountsTable />);

    expect(await screen.findByText('Your account is suspended.')).toBeInTheDocument();
    expect(screen.queryByRole('table')).toBeNull();
  });
});
