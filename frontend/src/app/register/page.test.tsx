import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import RegisterPage from './page';

const { registerWithEmail, register, push } = vi.hoisted(() => ({ registerWithEmail: vi.fn(), register: vi.fn(), push: vi.fn() }));
vi.mock('@/lib/firebase', () => ({ registerWithEmail }));
vi.mock('next/navigation', () => ({ useRouter: () => ({ push }) }));
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => ({ state: 'signed_out', register }) }));

describe('register page', () => {
  beforeEach(() => { vi.clearAllMocks(); registerWithEmail.mockResolvedValue({}); register.mockResolvedValue(undefined); });

  it('requires and submits a privileged registration code', async () => {
    render(<RegisterPage />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'host@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } });
    fireEvent.change(screen.getByLabelText('Role'), { target: { value: 'HOST' } });
    fireEvent.change(screen.getByLabelText('Registration code'), { target: { value: 'host-code' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Create account' }));
    await waitFor(() => expect(register).toHaveBeenCalledWith({ role: 'HOST', registrationCode: 'host-code', displayName: undefined }));
    expect(push).toHaveBeenCalledWith('/host');
  });

  it('returns an existing account to the login page', () => {
    render(<RegisterPage />);
    fireEvent.click(screen.getByRole('button', { name: 'I have an account' }));
    expect(push).toHaveBeenCalledWith('/login');
  });

  it('shows a specific message when the Firebase email is already registered', async () => {
    registerWithEmail.mockRejectedValue({ code: 'auth/email-already-in-use' });
    render(<RegisterPage />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'existing@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Create account' }));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('An account with this email already exists. Sign in instead.'));
    expect(register).not.toHaveBeenCalled();
  });
});
