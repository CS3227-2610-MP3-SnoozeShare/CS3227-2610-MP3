'use client';

import { useEffect } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth } from '@/providers/AuthProvider';

export function RouteGuard({ role, children }: { role: 'GUEST' | 'HOST' | 'AGENT'; children: React.ReactNode }) {
  const { state, profile, retry, signOut } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  useEffect(() => {
    const returnTo = encodeURIComponent(pathname || '/');
    if (state === 'signed_out') router.replace(`/login?returnTo=${returnTo}`);
    if (state === 'registering') router.replace('/register');
    if (state === 'authenticated' && profile?.role !== role) router.replace(`/${profile?.role.toLowerCase()}`);
  }, [pathname, profile, role, router, state]);
  if (state === 'loading') return <main className="flex min-h-screen items-center justify-center">Loading…</main>;
  if (state === 'inactive') return <main className="flex min-h-screen items-center justify-center"><section className="ss-card p-8"><h1 className="text-xl font-semibold">Profile unavailable</h1><p className="mt-2">Your SnoozeShare profile is inactive.</p></section></main>;
  if (state === 'error') return <main className="flex min-h-screen items-center justify-center"><section className="ss-card space-y-4 p-8"><h1 className="text-xl font-semibold">We couldn’t load your profile</h1><p>Please try again or sign out.</p><div className="flex gap-2"><button className="rounded-md bg-primary px-3 py-2 text-primary-foreground" onClick={() => void retry()}>Retry</button><button className="rounded-md border px-3 py-2" onClick={() => void signOut()}>Sign out</button></div></section></main>;
  if (state !== 'authenticated' || profile?.role !== role) return null;
  return children;
}

export function AuthOnly({
  role = 'GUEST',
  children,
}: { role?: 'GUEST' | 'HOST' | 'AGENT'; children: React.ReactNode }) {
  return <RouteGuard role={role}>{children}</RouteGuard>;
}
