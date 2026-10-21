import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { HostMessagesPanel } from './HostMessagesPanel';
import { HostWalletPanel } from './HostWalletPanel';
import { createHostFixtureAdapter } from './hostFixtures';

describe('Host messages and wallet', () => {
  it('renders the exact conversations and disables resolved composer', () => {
    const conversations = createHostFixtureAdapter().getMessages().data!;
    render(<HostMessagesPanel conversations={conversations} />);
    expect(screen.getByText('Question about check-in')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Write a message…')).not.toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: /Ticket #1042/ }));
    expect(screen.getByPlaceholderText('Write a message…')).toBeDisabled();
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
