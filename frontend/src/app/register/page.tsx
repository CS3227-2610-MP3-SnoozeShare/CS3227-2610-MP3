'use client';

import { FormEvent, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { registerWithEmail } from '@/lib/firebase';
import { useAuth } from '@/providers/AuthProvider';

export default function RegisterPage() {
  const router = useRouter();
  const { state, register } = useAuth();
  const [email, setEmail] = useState(''); const [password, setPassword] = useState('');
  const [role, setRole] = useState<'GUEST' | 'HOST' | 'AGENT'>('GUEST'); const [code, setCode] = useState(''); const [name, setName] = useState(''); const [error, setError] = useState('');
  async function submit(event: FormEvent) {
    event.preventDefault(); setError('');
    try { await registerWithEmail(email, password); await register({ role, registrationCode: code || undefined, displayName: name || undefined }); router.push(`/${role.toLowerCase()}`); } catch { setError('Registration could not be completed. Check your details and try again.'); }
  }
  return <main className="flex min-h-screen items-center justify-center p-6"><form className="ss-card w-full max-w-md space-y-4 p-8" onSubmit={submit}><h1 className="text-2xl font-semibold">Create your account</h1>
    <label className="block">Email<input className="mt-1 w-full rounded-md border bg-background p-2" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} /></label>
    <label className="block">Password<input className="mt-1 w-full rounded-md border bg-background p-2" type="password" minLength={6} required value={password} onChange={(e) => setPassword(e.target.value)} /></label>
    <label className="block">Display name<input className="mt-1 w-full rounded-md border bg-background p-2" value={name} onChange={(e) => setName(e.target.value)} /></label>
    <label className="block">Role<select className="mt-1 w-full rounded-md border bg-background p-2" value={role} onChange={(e) => setRole(e.target.value as typeof role)}><option>GUEST</option><option>HOST</option><option>AGENT</option></select></label>
    {role !== 'GUEST' && <label className="block">Registration code<input className="mt-1 w-full rounded-md border bg-background p-2" required value={code} onChange={(e) => setCode(e.target.value)} /></label>}
    {error && <p role="alert" className="text-destructive">{error}</p>}<Button type="submit" className="w-full" disabled={state === 'loading'}>Create account</Button>
  </form></main>;
}
