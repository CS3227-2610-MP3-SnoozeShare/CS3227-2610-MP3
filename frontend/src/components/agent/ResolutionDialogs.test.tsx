import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BackendApiError } from '@/lib/api';
import { ResolutionDialog } from './ResolutionDialogs';
import { detail } from './fixtures';

const resolveDispute = vi.fn();
vi.mock('@/lib/agent-api/disputes', () => ({ resolveDispute: (...args: unknown[]) => resolveDispute(...args) }));

const onResolved = vi.fn();
const onFailed = vi.fn();
const onClose = vi.fn();

function open(kind: 'ACCEPT' | 'REJECT' | 'MANUAL', remedy: 'FULL_REFUND' | 'PARTIAL_REFUND' | 'HOST_PAYOUT' | 'OTHER' = 'FULL_REFUND') {
  render(<ResolutionDialog kind={kind} detail={detail({}, { requestedRemedy: remedy })} onClose={onClose} onResolved={onResolved} onFailed={onFailed} />);
}

const reason = (text: string) => fireEvent.change(screen.getByLabelText('Reason'), { target: { value: text } });

describe('ResolutionDialog (F7, F14)', () => {
  beforeEach(() => {
    [resolveDispute, onResolved, onFailed, onClose].forEach((fn) => fn.mockReset());
  });

  it('Reject needs a reason and sends mode REJECT', async () => {
    resolveDispute.mockResolvedValue(detail());
    open('REJECT');
    const confirm = screen.getByRole('button', { name: 'Confirm reject' });
    expect(confirm).toBeDisabled();

    reason('No proof');
    fireEvent.click(confirm);

    await waitFor(() => expect(onResolved).toHaveBeenCalled());
    expect(resolveDispute).toHaveBeenCalledWith('t-1', { mode: 'REJECT', guestRefund: null, reason: 'No proof' });
  });

  it('Accept a full refund shows the escrow and sends no amount', async () => {
    resolveDispute.mockResolvedValue(detail());
    open('ACCEPT', 'FULL_REFUND');
    expect(screen.getByText('Full refund SGD 120.00')).toBeInTheDocument();

    reason('Agreed');
    fireEvent.click(screen.getByRole('button', { name: 'Confirm accept' }));

    await waitFor(() => expect(resolveDispute).toHaveBeenCalledWith('t-1', { mode: 'ACCEPT', guestRefund: null, reason: 'Agreed' }));
  });

  it('Accept a partial refund checks only the amount format (E73)', async () => {
    resolveDispute.mockResolvedValue(detail());
    open('ACCEPT', 'PARTIAL_REFUND');
    reason('Half');
    const amount = screen.getByLabelText('Guest refund (SGD)');
    const confirm = screen.getByRole('button', { name: 'Confirm accept' });

    for (const bad of ['', 'abc', '12.345', '-5', '0']) {
      fireEvent.change(amount, { target: { value: bad } });
      expect(confirm).toBeDisabled();
    }
    fireEvent.change(amount, { target: { value: '999999.99' } }); // larger than the escrow: the server decides
    expect(confirm).toBeEnabled();
    fireEvent.click(confirm);

    await waitFor(() => expect(resolveDispute).toHaveBeenCalledWith('t-1', { mode: 'ACCEPT', guestRefund: '999999.99', reason: 'Half' }));
  });

  it('Manual locks the amount for the two full overrides', async () => {
    resolveDispute.mockResolvedValue(detail());
    open('MANUAL');
    const amount = screen.getByLabelText('Guest refund (SGD)') as HTMLInputElement;
    expect(amount.value).toBe('120.00');
    expect(amount).toHaveAttribute('readonly');

    fireEvent.change(screen.getByLabelText('Override type'), { target: { value: 'HOST_PAYOUT' } });
    expect(amount.value).toBe('0.00');
    reason('Host was right');
    fireEvent.click(screen.getByRole('button', { name: 'Confirm' }));

    await waitFor(() => expect(resolveDispute).toHaveBeenCalledWith('t-1', { mode: 'MANUAL', guestRefund: '0.00', reason: 'Host was right' }));
  });

  it('Manual custom accepts zero but not a bad format', () => {
    open('MANUAL');
    fireEvent.change(screen.getByLabelText('Override type'), { target: { value: 'CUSTOM' } });
    reason('Custom');
    const amount = screen.getByLabelText('Guest refund (SGD)');
    const confirm = screen.getByRole('button', { name: 'Confirm' });

    fireEvent.change(amount, { target: { value: '1.234' } });
    expect(confirm).toBeDisabled();
    fireEvent.change(amount, { target: { value: '0' } });
    expect(confirm).toBeEnabled();
  });

  it('sends one request on a double click (E47)', async () => {
    resolveDispute.mockReturnValue(new Promise(() => undefined));
    open('REJECT');
    reason('Twice');
    const confirm = screen.getByRole('button', { name: 'Confirm reject' });

    fireEvent.click(confirm);
    fireEvent.click(confirm);

    expect(resolveDispute).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(confirm).toBeDisabled());
  });

  it('shows a 409 inline, stays open and tells the page to refetch (E48, E64)', async () => {
    resolveDispute.mockRejectedValue(new BackendApiError('CONFLICT', 'Ticket is not in review', undefined, 409));
    open('REJECT');
    reason('Late');
    fireEvent.click(screen.getByRole('button', { name: 'Confirm reject' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Ticket is not in review');
    expect(onFailed).toHaveBeenCalledTimes(1);
    expect(onResolved).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('Escape closes the dialog', () => {
    open('REJECT');
    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' });
    expect(onClose).toHaveBeenCalled();
  });
});
