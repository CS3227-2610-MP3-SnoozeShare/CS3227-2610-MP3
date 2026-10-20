import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ForgotPasswordPage from './page';

const resetPassword = vi.hoisted(() => vi.fn());
vi.mock('@/lib/firebase', () => ({ resetPassword }));

describe('forgot password page', () => {
  beforeEach(() => { vi.clearAllMocks(); resetPassword.mockResolvedValue(undefined); });

  it('shows a generic success message after requesting a reset', async () => {
    render(<ForgotPasswordPage />);
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'user@example.com' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Send reset link' }));
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Check your email for the reset link.'));
    expect(resetPassword).toHaveBeenCalledWith('user@example.com');
  });
});
