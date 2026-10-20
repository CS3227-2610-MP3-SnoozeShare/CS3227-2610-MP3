'use client';

import { FormEvent, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { AuthHeader } from '@/components/auth/AuthHeader';
import { signIn } from '@/lib/firebase';
import { useAuth } from '@/providers/AuthProvider';

export default function LoginPage() {
  const router = useRouter();
  const { state, profile } = useAuth();
  const [returnTo, setReturnTo] = useState<string | null>(null);
  const [signInPending, setSignInPending] = useState(false);
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    setReturnTo(safeReturnTo(params.get('returnTo')));
  }, []);
  useEffect(() => {
    if (!signInPending) return;
    if (state === 'registering') {
      router.push('/register');
      setSignInPending(false);
    } else if (state === 'authenticated' && profile) {
      router.push(returnTo ?? `/${profile.role.toLowerCase()}`);
      setSignInPending(false);
    }
  }, [profile, returnTo, router, signInPending, state]);
  useEffect(() => {
    if (!signInPending && state === 'authenticated' && profile) router.replace(`/${profile.role.toLowerCase()}`);
  }, [profile, router, signInPending, state]);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  async function submit(event: FormEvent) {
    event.preventDefault();
    setError('');
    try { await signIn(email, password); setSignInPending(true); } catch { setError('Unable to sign in with those details.'); }
  }
  return <main className="flex min-h-screen items-center justify-center p-6"><form className="ss-card w-full max-w-md space-y-4 p-8" onSubmit={submit}>
    <AuthHeader title="Welcome back" />
    <label className="block">Email<input className="mt-1 w-full rounded-md border bg-background p-2" type="email" placeholder="you@example.com" required value={email} onChange={(e) => setEmail(e.target.value)} /></label>
    <label className="block">Password<input className="mt-1 w-full rounded-md border bg-background p-2" type="password" placeholder="Enter your password" required value={password} onChange={(e) => setPassword(e.target.value)} /></label>
    {error && <p role="alert" className="text-destructive">{error}</p>}<Button type="submit" className="w-full">Sign in</Button>
    <p className="text-sm"><Link className="text-primary underline" href="/forgot-password">Forgot password?</Link> · <Link className="text-primary underline" href="/register">Create account</Link></p>
  </form></main>;
}

function safeReturnTo(value: string | null) {
  return value && value.startsWith('/') && !value.startsWith('//') ? value : null;
}
