'use client';

import { FormEvent, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { AuthHeader } from '@/components/auth/AuthHeader';
import { useRedirectAuthenticated } from '@/components/auth/useRedirectAuthenticated';
import { resetPassword } from '@/lib/firebase';

export default function ForgotPasswordPage() {
  useRedirectAuthenticated();
  const [email, setEmail] = useState(''); const [sent, setSent] = useState(false); const [error, setError] = useState('');
  async function submit(event: FormEvent) { event.preventDefault(); setError(''); try { await resetPassword(email); setSent(true); } catch { setError('Unable to send a reset email. Please try again.'); } }
  return <main className="flex min-h-screen items-center justify-center p-6"><form className="ss-card w-full max-w-md space-y-4 p-8" onSubmit={submit}><AuthHeader title="Reset password" /><p className="text-muted-foreground">Enter your account email and we’ll send a reset link.</p><input className="w-full rounded-md border bg-background p-2" type="email" placeholder="you@example.com" required value={email} onChange={(e) => setEmail(e.target.value)} />{sent && <p role="status" className="text-[var(--success)]">Check your email for the reset link.</p>}{error && <p role="alert" className="text-destructive">{error}</p>}<Button type="submit" className="w-full">Send reset link</Button><Link className="text-primary underline" href="/login">Back to sign in</Link></form></main>;
}
