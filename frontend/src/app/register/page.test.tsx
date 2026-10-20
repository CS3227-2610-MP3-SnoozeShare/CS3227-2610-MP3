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
});
