'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { AuthHeader } from '@/components/auth/AuthHeader';
import { useRedirectAuthenticated } from '@/components/auth/useRedirectAuthenticated';
import { SelectField } from '@/components/ui/select';
import { auth, registerWithEmail } from '@/lib/firebase';
import { validateRegistrationCode } from '@/lib/api';
import { useAuth } from '@/providers/AuthProvider';

export default function RegisterPage() {
  const router = useRouter();
  const { state, register } = useAuth();
  useRedirectAuthenticated();
  const [email, setEmail] = useState(''); const [password, setPassword] = useState('');
  const [role, setRole] = useState<'GUEST' | 'HOST' | 'AGENT'>('GUEST'); const [code, setCode] = useState(''); const [name, setName] = useState(''); const [error, setError] = useState(''); const [isSubmitting, setIsSubmitting] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault(); setError('');
    if (isSubmitting) return;
    setIsSubmitting(true);
    try {
      if (role !== 'GUEST') await validateRegistrationCode(role, code);
      if (!auth.currentUser) await registerWithEmail(email, password);
      await register({ role, registrationCode: code || undefined, displayName: name || undefined });
      router.push(`/${role.toLowerCase()}`);
    } catch (caught) {
      setError(registrationErrorMessage(caught));
    } finally {
      setIsSubmitting(false);
    }
  }
  return <main className="flex min-h-screen items-center justify-center p-6"><form className="ss-card w-full max-w-md space-y-4 p-8" onSubmit={submit}><AuthHeader title="Create your account" />
    <label className="block">Email<input className="mt-1 w-full rounded-md border bg-background p-2" type="email" placeholder="you@example.com" required value={email} onChange={(e) => setEmail(e.target.value)} /></label>
    <label className="block">Password<input className="mt-1 w-full rounded-md border bg-background p-2" type="password" placeholder="Create a password" minLength={6} required value={password} onChange={(e) => setPassword(e.target.value)} /></label>
    <label className="block">Display name<input className="mt-1 w-full rounded-md border bg-background p-2" placeholder="How should we call you?" value={name} onChange={(e) => setName(e.target.value)} /></label>
    <label className="block">Role<SelectField aria-label="Role" name="role" value={role} onValueChange={(value) => setRole(value as typeof role)} options={[{ value: 'GUEST', label: 'GUEST' }, { value: 'HOST', label: 'HOST' }, { value: 'AGENT', label: 'AGENT' }]} /></label>
    {role !== 'GUEST' && <label className="block">Registration code<input className="mt-1 w-full rounded-md border bg-background p-2" placeholder="Enter your registration code" required value={code} onChange={(e) => setCode(e.target.value)} /></label>}
    {error && <p role="alert" className="text-destructive">{error}</p>}<Button type="submit" className="w-full" disabled={state === 'loading' || isSubmitting}>Create account</Button><Button type="button" variant="outline" className="w-full" onClick={() => router.push('/login')}>I have an account</Button>
  </form></main>;
}

function isDuplicateAccountError(error: unknown): boolean {
  return typeof error === 'object' && error !== null && 'code' in error && error.code === 'auth/email-already-in-use';
}

function registrationErrorMessage(error: unknown): string {
  if (isDuplicateAccountError(error)) return 'An account with this email already exists. Sign in instead.';
  if (typeof error === 'object' && error !== null && 'code' in error) {
    if (error.code === 'INVALID_REGISTRATION_CODE') return 'Registration code is invalid';
    if (error.code === 'REGISTRATION_RATE_LIMITED') return 'Too many attempts. Try again later';
    if (error.code === 'API_UNAVAILABLE' || error.code === 'VALIDATION_ERROR') {
      return 'Unable to validate registration right now';
    }
  }
  return 'Registration could not be completed. Check your details and try again.';
}
