import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ForgotPasswordPage from './page';

const { resetPassword, replace, auth } = vi.hoisted(() => ({
  resetPassword: vi.fn(),
  replace: vi.fn(),
  auth: { state: 'signed_out' as string, profile: null as { role: string } | null },
}));
vi.mock('@/lib/firebase', () => ({ resetPassword }));
vi.mock('next/navigation', () => ({ useRouter: () => ({ replace }) }));
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => auth }));

describe('forgot password page', () => {
  beforeEach(() => { vi.clearAllMocks(); auth.state = 'signed_out'; auth.profile = null; resetPassword.mockResolvedValue(undefined); });

  it('redirects an already authenticated user to their role page', async () => {
    auth.state = 'authenticated';
    auth.profile = { role: 'GUEST' };
    render(<ForgotPasswordPage />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith('/guest'));
  });

  it('shows a generic success message after requesting a reset', async () => {
    render(<ForgotPasswordPage />);
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'user@example.com' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Send reset link' }));
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Check your email for the reset link.'));
    expect(resetPassword).toHaveBeenCalledWith('user@example.com');
  });
});
