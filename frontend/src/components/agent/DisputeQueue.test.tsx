import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BackendApiError } from '@/lib/api';
import { DisputeQueue } from './DisputeQueue';
import { summary } from './fixtures';

const fetchQueue = vi.fn();
const impl = vi.hoisted(() => ({ current: undefined as undefined | ((...args: unknown[]) => Promise<unknown>) }));
vi.mock('@/lib/agent-api/disputes', () => ({
  fetchQueue: (...args: unknown[]) => (impl.current ?? fetchQueue)(...args),
}));
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => ({ signOut: vi.fn() }) }));

describe('DisputeQueue (F3, F14)', () => {
  beforeEach(() => {
    fetchQueue.mockReset();
    impl.current = undefined;
  });

  it('shows loading, then rows with links and the global unassigned pill', async () => {
    fetchQueue.mockResolvedValue({
      items: [summary(), summary({ ticketId: 't-2', ticketLabel: '#0002', status: 'IN_REVIEW', assignedAgentName: 'Ann' })],
      unassignedCount: 4,
    });
    render(<DisputeQueue />);
    expect(screen.getByText('Loading disputes...')).toBeInTheDocument();

    const link = await screen.findByRole('link', { name: '#0001' });
    expect(link).toHaveAttribute('href', '/agent/disputes/t-1');
    expect(screen.getByText('4 unassigned')).toBeInTheDocument();
    const table = within(screen.getByRole('table'));
    expect(table.getByText('In review')).toBeInTheDocument();
    expect(table.getByText('Unassigned')).toBeInTheDocument();
    expect(table.getByText('—')).toBeInTheDocument();
  });

  it('reloads with the chosen assignee', async () => {
    fetchQueue.mockResolvedValue({ items: [], unassignedCount: 0 });
    render(<DisputeQueue />);
    await screen.findByText('No disputes match these filters.'); // E50

    fireEvent.click(screen.getByRole('button', { name: 'Mine' }));
    await waitFor(() => expect(fetchQueue).toHaveBeenLastCalledWith('MINE', undefined));
    expect(screen.getByRole('button', { name: 'Mine' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('shows an error banner with Retry', async () => {
    fetchQueue.mockRejectedValueOnce(new Error('Boom')).mockResolvedValue({ items: [], unassignedCount: 0 });
    render(<DisputeQueue />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Boom');

    fireEvent.click(screen.getByRole('button', { name: 'Retry' }));
    await screen.findByText('No disputes match these filters.');
  });

  it('replaces the table with the session notice on a 403 PROFILE_INACTIVE (E45)', async () => {
    // A plain function: a spy that returns a rejected promise is reported as an unhandled error by vitest.
    impl.current = () => Promise.reject(new BackendApiError('PROFILE_INACTIVE', 'x', undefined, 403));
    render(<DisputeQueue />);
    expect(await screen.findByText('Your account is suspended.')).toBeInTheDocument();
    expect(screen.queryByRole('table')).toBeNull();
  });

  it('renders user text literally (E36)', async () => {
    fetchQueue.mockResolvedValue({ items: [summary({ title: '<img src=x onerror=alert(1)>' })], unassignedCount: 1 });
    const { container } = render(<DisputeQueue />);
    expect(await screen.findByText('<img src=x onerror=alert(1)>')).toBeInTheDocument();
    expect(container.querySelector('img')).toBeNull();
  });

  it('filters by status through the drop-down and shows the desktop pill words', async () => {
    fetchQueue.mockResolvedValue({
      items: [summary({ status: 'RESOLVED_APPROVED' }), summary({ ticketId: 't-9', status: 'RESOLVED_REJECTED' })],
      unassignedCount: 0,
    });
    render(<DisputeQueue />);
    const table = await screen.findByRole('table');
    expect(within(table).getByText('Resolved')).toBeInTheDocument();
    expect(within(table).getByText('Rejected')).toBeInTheDocument();

    fireEvent.change(screen.getByRole('combobox', { name: 'Status' }), { target: { value: 'IN_REVIEW' } });
    await waitFor(() => expect(fetchQueue).toHaveBeenLastCalledWith('ALL', 'IN_REVIEW'));
  });
});
