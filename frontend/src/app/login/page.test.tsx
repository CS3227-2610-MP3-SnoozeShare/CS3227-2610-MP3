import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import LoginPage from './page';

const { signIn, push, replace, auth } = vi.hoisted(() => ({
  signIn: vi.fn(),
  push: vi.fn(),
  replace: vi.fn(),
  auth: { state: 'signed_out' as string, profile: null as { role: string } | null },
}));
vi.mock('@/lib/firebase', () => ({ signIn }));
vi.mock('next/navigation', () => ({ useRouter: () => ({ push, replace }) }));
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => auth }));

describe('login page', () => {
  beforeEach(() => { vi.clearAllMocks(); auth.state = 'signed_out'; auth.profile = null; signIn.mockResolvedValue({}); });

  it('redirects an already authenticated user to their role page', async () => {
    auth.state = 'authenticated';
    auth.profile = { role: 'HOST' };
    render(<LoginPage />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith('/host'));
  });

  it('waits for the existing profile before navigating to its role area', async () => {
    const view = render(<LoginPage />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Sign in' }));
    await waitFor(() => expect(signIn).toHaveBeenCalled());
    expect(push).not.toHaveBeenCalled();
    auth.state = 'authenticated';
    auth.profile = { role: 'GUEST' };
    view.rerender(<LoginPage />);
    await waitFor(() => expect(push).toHaveBeenCalledWith('/guest'));
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

  it('rejects backslash-based external return paths', async () => {
    window.history.pushState({}, '', '/login?returnTo=/%5Cevil.example');
    const view = render(<LoginPage />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Sign in' }));
    await waitFor(() => expect(signIn).toHaveBeenCalled());
    auth.state = 'authenticated';
    auth.profile = { role: 'GUEST' };
    view.rerender(<LoginPage />);
    await waitFor(() => expect(push).toHaveBeenCalledWith('/guest'));
  });

  it.each([
    ['a tab-split protocol-relative path', '/%09/evil.example'],
    ['a newline-split protocol-relative path', '/%0A/evil.example'],
    ['a protocol-relative path', '//evil.example'],
    ['an absolute URL', 'https://evil.example'],
  ])('ignores %s as a return path', async (_label, returnTo) => {
    window.history.pushState({}, '', `/login?returnTo=${returnTo}`);
    const view = render(<LoginPage />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Sign in' }));
    await waitFor(() => expect(signIn).toHaveBeenCalled());
    auth.state = 'authenticated';
    auth.profile = { role: 'GUEST' };
    view.rerender(<LoginPage />);
    await waitFor(() => expect(push).toHaveBeenCalledWith('/guest'));
  });

  it('keeps a same-origin return path with its query', async () => {
    window.history.pushState({}, '', '/login?returnTo=%2Fguest%2Ftrips%3Ftab%3Dupcoming');
    const view = render(<LoginPage />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password' } });
    fireEvent.submit(screen.getByRole('button', { name: 'Sign in' }));
    await waitFor(() => expect(signIn).toHaveBeenCalled());
    auth.state = 'authenticated';
    auth.profile = { role: 'GUEST' };
    view.rerender(<LoginPage />);
    await waitFor(() => expect(push).toHaveBeenCalledWith('/guest/trips?tab=upcoming'));
  });
});
