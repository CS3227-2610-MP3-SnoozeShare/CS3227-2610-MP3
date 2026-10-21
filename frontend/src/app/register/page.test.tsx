import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import RegisterPage from './page';

const { registerWithEmail, validateRegistrationCode, register, push, replace, auth } = vi.hoisted(() => ({
  registerWithEmail: vi.fn(),
  validateRegistrationCode: vi.fn(),
  register: vi.fn(),
  push: vi.fn(),
  replace: vi.fn(),
  auth: { state: 'signed_out' as string, profile: null as { role: string } | null, currentUser: null as object | null },
}));
vi.mock('@/lib/firebase', () => ({ registerWithEmail, auth }));
vi.mock('@/lib/api', () => ({ validateRegistrationCode }));
vi.mock('next/navigation', () => ({ useRouter: () => ({ push, replace }) }));
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => ({ ...auth, register }) }));

describe('register page', () => {
  beforeEach(() => { vi.clearAllMocks(); auth.state = 'signed_out'; auth.profile = null; auth.currentUser = null; validateRegistrationCode.mockResolvedValue(undefined); registerWithEmail.mockResolvedValue({}); register.mockResolvedValue(undefined); });

  it('redirects an already authenticated user to their role page', async () => {
    auth.state = 'authenticated';
    auth.profile = { role: 'AGENT' };
    render(<RegisterPage />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith('/agent'));
  });

  it('requires and submits a privileged registration code', async () => {
    render(<RegisterPage />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'host@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } });
    await userEvent.click(screen.getByRole('combobox', { name: 'Role' }));
    await userEvent.click(await screen.findByRole('option', { name: 'HOST' }));
    fireEvent.change(screen.getByLabelText('Registration code'), { target: { value: 'host-code' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Create account' }));
    await waitFor(() => expect(register).toHaveBeenCalledWith({ role: 'HOST', registrationCode: 'host-code', displayName: undefined }));
    expect(validateRegistrationCode).toHaveBeenCalledWith('HOST', 'host-code');
    expect(validateRegistrationCode.mock.invocationCallOrder[0]).toBeLessThan(registerWithEmail.mock.invocationCallOrder[0]);
    expect(push).toHaveBeenCalledWith('/host');
  });

  it('does not create a Firebase account when pre-validation fails', async () => {
    validateRegistrationCode.mockRejectedValue({ code: 'INVALID_REGISTRATION_CODE' });
    render(<RegisterPage />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'host@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } });
    await userEvent.click(screen.getByRole('combobox', { name: 'Role' }));
    await userEvent.click(await screen.findByRole('option', { name: 'HOST' }));
    fireEvent.change(screen.getByLabelText('Registration code'), { target: { value: 'wrong' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Create account' }));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Registration code is invalid'));
    expect(registerWithEmail).not.toHaveBeenCalled();
  });

  it('skips pre-validation for Guest registration', async () => {
    render(<RegisterPage />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'guest@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Create account' }));
    await waitFor(() => expect(register).toHaveBeenCalledWith({ role: 'GUEST', registrationCode: undefined, displayName: undefined }));
    expect(validateRegistrationCode).not.toHaveBeenCalled();
  });

  it('does not repeat Firebase signup when retrying with an existing session', async () => {
    auth.currentUser = {};
    render(<RegisterPage />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'host@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } });
    await userEvent.click(screen.getByRole('combobox', { name: 'Role' }));
    await userEvent.click(await screen.findByRole('option', { name: 'HOST' }));
    fireEvent.change(screen.getByLabelText('Registration code'), { target: { value: 'host-code' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Create account' }));
    await waitFor(() => expect(register).toHaveBeenCalled());
    expect(registerWithEmail).not.toHaveBeenCalled();
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

  it('opens the role dropdown and updates the selected role', async () => {
    render(<RegisterPage />);
    const trigger = screen.getByRole('combobox', { name: 'Role' });
    expect(trigger.tagName).toBe('BUTTON');
    await userEvent.click(trigger);
    await userEvent.click(await screen.findByRole('option', { name: 'HOST' }));
    expect(screen.getByRole('combobox', { name: 'Role' })).toHaveTextContent('HOST');
  });
});
