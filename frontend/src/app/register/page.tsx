'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { AuthHeader } from '@/components/auth/AuthHeader';
import { useRedirectAuthenticated } from '@/components/auth/useRedirectAuthenticated';
import { SelectField } from '@/components/ui/select';
import { registerWithEmail } from '@/lib/firebase';
import { useAuth } from '@/providers/AuthProvider';

export default function RegisterPage() {
  const router = useRouter();
  const { state, register } = useAuth();
  useRedirectAuthenticated();
  const [email, setEmail] = useState(''); const [password, setPassword] = useState('');
  const [role, setRole] = useState<'GUEST' | 'HOST' | 'AGENT'>('GUEST'); const [code, setCode] = useState(''); const [name, setName] = useState(''); const [error, setError] = useState('');
  async function submit(event: FormEvent) {
    event.preventDefault(); setError('');
    try { await registerWithEmail(email, password); await register({ role, registrationCode: code || undefined, displayName: name || undefined }); router.push(`/${role.toLowerCase()}`); } catch (caught) { setError(isDuplicateAccountError(caught) ? 'An account with this email already exists. Sign in instead.' : 'Registration could not be completed. Check your details and try again.'); }
  }
  return <main className="flex min-h-screen items-center justify-center p-6"><form className="ss-card w-full max-w-md space-y-4 p-8" onSubmit={submit}><AuthHeader title="Create your account" />
    <label className="block">Email<input className="mt-1 w-full rounded-md border bg-background p-2" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></label>
    <label className="block">Password<input className="mt-1 w-full rounded-md border bg-background p-2" type="password" minLength={6} required value={password} onChange={(e) => setPassword(e.target.value)} /></label>
    <label className="block">Display name<input className="mt-1 w-full rounded-md border bg-background p-2" value={name} onChange={(e) => setName(e.target.value)} /></label>
    <label className="block">Role<SelectField aria-label="Role" name="role" value={role} onValueChange={(value) => setRole(value as typeof role)} options={[{ value: 'GUEST', label: 'GUEST' }, { value: 'HOST', label: 'HOST' }, { value: 'AGENT', label: 'AGENT' }]} /></label>
    {role !== 'GUEST' && <label className="block">Registration code<input className="mt-1 w-full rounded-md border bg-background p-2" required value={code} onChange={(e) => setCode(e.target.value)} /></label>}
    {error && <p role="alert" className="text-destructive">{error}</p>}<Button type="submit" className="w-full" disabled={state === 'loading'}>Create account</Button><Button type="button" variant="outline" className="w-full" onClick={() => router.push('/login')}>I have an account</Button>
  </form></main>;
}

function isDuplicateAccountError(error: unknown): boolean {
  return typeof error === 'object' && error !== null && 'code' in error && error.code === 'auth/email-already-in-use';
}
