'use client';

import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import type { User } from 'firebase/auth';
import { BackendApiError, fetchCurrentProfile, registerProfile, type AuthProfile } from '@/lib/api';
import { observeAuth, resendVerification, signOutUser } from '@/lib/firebase';

export type AuthState = 'loading' | 'signed_out' | 'verify_email' | 'registering' | 'authenticated' | 'inactive' | 'error';

type AuthContextValue = {
  state: AuthState;
  user: User | null;
  profile: AuthProfile | null;
  error: BackendApiError | Error | null;
  register: (payload: Parameters<typeof registerProfile>[0]) => Promise<void>;
  refreshVerification: () => Promise<void>;
  resendVerification: () => Promise<void>;
  retry: () => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>('loading');
  const [user, setUser] = useState<User | null>(null);
  const [profile, setProfile] = useState<AuthProfile | null>(null);
  const [error, setError] = useState<BackendApiError | Error | null>(null);

  async function loadUser(nextUser: User | null) {
    setUser(nextUser);
    setProfile(null);
    setError(null);
    if (!nextUser) {
      setState('signed_out');
      return;
    }
    if (!nextUser.emailVerified) {
      setState('verify_email');
      return;
    }
    try {
      setProfile(await fetchCurrentProfile());
      setState('authenticated');
    } catch (caught) {
      if (caught instanceof BackendApiError && caught.code === 'PROFILE_NOT_REGISTERED') {
        setState('registering');
      } else if (caught instanceof BackendApiError && caught.code === 'PROFILE_INACTIVE') {
        setState('inactive');
        setError(caught);
      } else {
        setState('error');
        setError(caught instanceof Error ? caught : new Error('Unable to load profile'));
      }
    }
  }

  useEffect(() => observeAuth((nextUser) => void loadUser(nextUser)), []);

  const value = useMemo<AuthContextValue>(() => ({
    state,
    user,
    profile,
    error,
    async register(payload) {
      setError(null);
      setProfile(await registerProfile(payload));
      setState('authenticated');
    },
    async refreshVerification() {
      if (!user) return;
      await user.reload();
      await loadUser(user);
    },
    async resendVerification() {
      if (user) await resendVerification(user);
    },
    async retry() {
      await loadUser(user);
    },
    async signOut() {
      await signOutUser();
      setProfile(null);
      setUser(null);
      setState('signed_out');
    },
  }), [error, profile, state, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}

export async function sendVerificationEmail(user: User) {
  return resendVerification(user);
}
