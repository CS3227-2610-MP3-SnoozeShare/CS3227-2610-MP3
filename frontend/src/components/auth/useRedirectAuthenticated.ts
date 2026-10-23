'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/providers/AuthProvider';

/** Keeps completed users out of authentication screens. */
export function useRedirectAuthenticated() {
  const router = useRouter();
  const { state, profile } = useAuth();

  useEffect(() => {
    if (state === 'authenticated' && profile) router.replace(`/${profile.role.toLowerCase()}`);
  }, [profile, router, state]);
}
