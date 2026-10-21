import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { HostListingForm } from './HostListingForm';
import { HostCalendarPanel } from './HostCalendarPanel';
import { HostMessagesPanel } from './HostMessagesPanel';
import { HostWalletPanel } from './HostWalletPanel';
import { HostRequestsTable } from './HostRequestsTable';
import { createHostFixtureAdapter } from './hostFixtures';

const push = vi.fn();
vi.mock('next/navigation', () => ({ useRouter: () => ({ push }) }));

describe('W-HOST local-only interactions', () => {
  it('validates malformed listing form values without network calls', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch');
    render(<HostListingForm />);
    fireEvent.click(screen.getByRole('button', { name: 'Save listing' }));
    await waitFor(() => expect(screen.getByText('Title is required')).toBeInTheDocument());
    expect(fetchSpy).not.toHaveBeenCalled();
    fetchSpy.mockRestore();
  });

  it('rejects an invalid calendar range and leaves the fixture unchanged', () => {
    render(<HostCalendarPanel calendar={createHostFixtureAdapter().getCalendar('lakeside-cabin').data!} />);
    fireEvent.change(screen.getByLabelText('FROM'), { target: { value: '2026-09-17' } });
    fireEvent.change(screen.getByLabelText('TO'), { target: { value: '2026-09-15' } });
    fireEvent.click(screen.getByRole('button', { name: 'Block these dates' }));
    expect(screen.getByText('End date must be on or after start date')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('appends a direct message locally and keeps a resolved thread read-only', async () => {
    render(<HostMessagesPanel conversations={createHostFixtureAdapter().getMessages().data!} />);
    fireEvent.change(screen.getByPlaceholderText('Write a message…'), { target: { value: 'See you soon.' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send' }));
    await waitFor(() => expect(screen.getByText('See you soon.')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /Ticket #1042/ }));
    expect(screen.getByPlaceholderText('Write a message…')).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled();
  });

  it('opens and closes local request and wallet presentations without mutating records', async () => {
    const adapter = createHostFixtureAdapter();
    render(<><HostRequestsTable requests={adapter.getRequests().data!} /><HostWalletPanel wallet={adapter.getWalletSummary().data!} statement={adapter.getStatement().data!} /></>);
    fireEvent.click(screen.getAllByRole('button', { name: 'Approve' })[0]);
    await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Withdraw' }));
    await waitFor(() => expect(screen.getByRole('dialog')).toHaveTextContent('no real payout rail'));
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.getByText('Wallet: $3,920.50')).toBeInTheDocument();
  });
});
