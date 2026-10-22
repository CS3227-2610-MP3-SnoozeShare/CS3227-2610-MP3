'use client';

import { BackendApiError } from '@/lib/api';
import { useAuth } from '@/providers/AuthProvider';

const MESSAGES: Record<string, string> = {
  PROFILE_INACTIVE: 'Your account is suspended.',
  PROFILE_NOT_REGISTERED: 'Your profile is not set up.',
  ROLE_MISMATCH: 'This area is for support agents.',
};

function messageFor(error: unknown): string {
  if (error instanceof BackendApiError && MESSAGES[error.code]) return MESSAGES[error.code];
  return 'Your session has ended. Sign in again.';
}

/** Replaces a page body when the API reports that the session can no longer use it. */
export function SessionNotice({ error }: { error: unknown }) {
  const { signOut } = useAuth();
  return (
    <div role="alert" className="mx-auto mt-16 max-w-md rounded-md border border-[var(--border)] bg-card p-6 text-center">
      <p className="mb-4 text-sm font-semibold">{messageFor(error)}</p>
      <button type="button" className="rounded-md bg-primary px-4 py-2 text-sm font-bold text-primary-foreground" onClick={() => void signOut()}>
        Sign out
      </button>
    </div>
  );
}
