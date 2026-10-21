import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from '@/providers/AuthProvider';

const { observeAuth, signOutUser, fetchCurrentProfile, registerProfile } = vi.hoisted(() => ({
  observeAuth: vi.fn(),
  signOutUser: vi.fn(),
  fetchCurrentProfile: vi.fn(),
  registerProfile: vi.fn(),
}));

vi.mock('@/lib/firebase', () => ({ observeAuth, resendVerification: vi.fn(), signOutUser }));
vi.mock('@/lib/api', () => ({ BackendApiError: class BackendApiError extends Error {}, fetchCurrentProfile, registerProfile }));

function StateProbe() {
  const { state, signOut } = useAuth();
  return <><output>{state}</output><button onClick={() => void signOut()}>Sign out</button></>;
}

describe('AuthProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    observeAuth.mockImplementation((callback: (user: null) => void) => { callback(null); return () => undefined; });
  });

  it('enters signed-out state when Firebase has no current user', async () => {
    render(<AuthProvider><StateProbe /></AuthProvider>);
    await waitFor(() => expect(screen.getByText('signed_out')).toBeInTheDocument());
  });

  it('surfaces incomplete Firebase configuration without crashing the root layout', async () => {
    observeAuth.mockImplementation(() => { throw new Error('Firebase browser configuration is incomplete'); });
    render(<AuthProvider><StateProbe /></AuthProvider>);
    await waitFor(() => expect(screen.getByText('error')).toBeInTheDocument());
  });

  it('ignores a delayed profile response after sign-out', async () => {
    let resolveProfile!: (profile: { role: string }) => void;
    fetchCurrentProfile.mockReturnValue(new Promise((resolve) => { resolveProfile = resolve; }));
    observeAuth.mockImplementation((callback: (user: { uid: string }) => void) => {
      callback({ uid: 'firebase-user' });
      return () => undefined;
    });
    signOutUser.mockResolvedValue(undefined);
    render(<AuthProvider><StateProbe /></AuthProvider>);
    await waitFor(() => expect(fetchCurrentProfile).toHaveBeenCalled());

    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }));
    await waitFor(() => expect(screen.getByText('signed_out')).toBeInTheDocument());
    resolveProfile({ role: 'GUEST' });
    await waitFor(() => expect(screen.getByText('signed_out')).toBeInTheDocument());
  });
});
