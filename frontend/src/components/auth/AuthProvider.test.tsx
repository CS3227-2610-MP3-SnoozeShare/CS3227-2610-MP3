import { render, screen, waitFor } from '@testing-library/react';
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
  const { state } = useAuth();
  return <output>{state}</output>;
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
});
