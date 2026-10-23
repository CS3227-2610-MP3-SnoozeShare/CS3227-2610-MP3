import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { BackendApiError } from '@/lib/api';
import { SessionNotice } from './SessionNotice';

const signOut = vi.fn();
vi.mock('@/providers/AuthProvider', () => ({ useAuth: () => ({ signOut }) }));

describe('SessionNotice (F16, E45)', () => {
  it.each([
    [401, 'INVALID_TOKEN', 'Your session has ended. Sign in again.'],
    [403, 'PROFILE_INACTIVE', 'Your account is suspended.'],
    [404, 'PROFILE_NOT_REGISTERED', 'Your profile is not set up.'],
    [403, 'ROLE_MISMATCH', 'This area is for support agents.'],
  ])('shows the stable message for %i %s', (status, code, message) => {
    render(<SessionNotice error={new BackendApiError(code, 'raw server text', undefined, status)} />);
    expect(screen.getByRole('alert')).toHaveTextContent(message);
    expect(screen.queryByText('raw server text')).toBeNull();
  });

  it('signs out when the button is pressed', () => {
    render(<SessionNotice error={new BackendApiError('INVALID_TOKEN', 'x', undefined, 401)} />);
    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }));
    expect(signOut).toHaveBeenCalledTimes(1);
  });
});
