import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LoginPage from './page';

const { signIn, push } = vi.hoisted(() => ({ signIn: vi.fn(), push: vi.fn() }));
vi.mock('@/lib/firebase', () => ({ signIn }));
vi.mock('next/navigation', () => ({ useRouter: () => ({ push }) }));
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => ({ state: 'signed_out', resendVerification: vi.fn(), refreshVerification: vi.fn() }) }));

describe('login page', () => {
  beforeEach(() => { vi.clearAllMocks(); signIn.mockResolvedValue({}); });

  it('signs in and navigates to the safe default', async () => {
    render(<LoginPage />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Sign in' }));
    await waitFor(() => expect(push).toHaveBeenCalledWith('/register'));
  });

  it('shows a generic error when sign-in fails', async () => {
    signIn.mockRejectedValue(new Error('provider detail'));
    render(<LoginPage />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Sign in' }));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Unable to sign in with those details.'));
    expect(screen.queryByText('provider detail')).not.toBeInTheDocument();
  });
});
