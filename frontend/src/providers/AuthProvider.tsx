'use client';

import { createContext, useContext, useEffect, useMemo, useRef, useState } from 'react';
import type { User } from 'firebase/auth';
import { BackendApiError, fetchCurrentProfile, registerProfile, type AuthProfile } from '@/lib/api';
import { observeAuth, signOutUser } from '@/lib/firebase';

export type AuthState = 'loading' | 'signed_out' | 'registering' | 'authenticated' | 'inactive' | 'error';

type AuthContextValue = {
  state: AuthState;
  user: User | null;
  profile: AuthProfile | null;
  error: BackendApiError | Error | null;
  register: (payload: Parameters<typeof registerProfile>[0]) => Promise<void>;
  retry: () => Promise<void>;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>('loading');
  const [user, setUser] = useState<User | null>(null);
  const [profile, setProfile] = useState<AuthProfile | null>(null);
  const [error, setError] = useState<BackendApiError | Error | null>(null);
  const requestGeneration = useRef(0);

  async function loadUser(nextUser: User | null) {
    const generation = ++requestGeneration.current;
    setUser(nextUser);
    setProfile(null);
    setError(null);
    if (!nextUser) {
      setState('signed_out');
      return;
    }
    try {
      const nextProfile = await fetchCurrentProfile();
      if (generation !== requestGeneration.current) return;
      setProfile(nextProfile);
      setState('authenticated');
    } catch (caught) {
      if (generation !== requestGeneration.current) return;
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

  useEffect(() => {
    try {
      return observeAuth((nextUser) => void loadUser(nextUser));
    } catch (caught) {
      setState('error');
      setError(caught instanceof Error ? caught : new Error('Unable to initialize authentication'));
      return undefined;
    }
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    state,
    user,
    profile,
    error,
    async register(payload) {
      const generation = ++requestGeneration.current;
      setError(null);
      let nextProfile: AuthProfile;
      try {
        nextProfile = await registerProfile(payload);
      } catch (caught) {
        // The Firebase session outlives a failed registration (E10); stay on the registration step so the
        // guards do not treat the live session as signed out while the user retries.
        if (generation === requestGeneration.current) setState('registering');
        throw caught;
      }
      if (generation !== requestGeneration.current) return;
      setProfile(nextProfile);
      setState('authenticated');
    },
    async retry() {
      await loadUser(user);
    },
    async signOut() {
      ++requestGeneration.current;
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
