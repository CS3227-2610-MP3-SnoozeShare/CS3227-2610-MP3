'use client';

import { useMemo, useState } from 'react';
import { isSessionFailure } from '@/lib/api';
import { fetchAccounts, setAccountSuspended } from '@/lib/agent-api/accounts';
import type { AccountSummary } from '@/lib/agent-api/types';
import { AgentDialog } from './AgentDialog';
import { SessionNotice } from './SessionNotice';
import { usePolling } from './usePolling';

function joined(iso: string): string {
  return new Intl.DateTimeFormat('en-US', { month: 'short', year: 'numeric' }).format(new Date(iso));
}

type Target = { account: AccountSummary; suspend: boolean };

function ReasonDialog({ target, onClose, onDone, onSessionFailure }: {
  target: Target;
  onClose: () => void;
  onDone: () => void;
  onSessionFailure: (error: unknown) => void;
}) {
  const [reason, setReason] = useState('');
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | undefined>(undefined);
  const verb = target.suspend ? 'Suspend' : 'Reactivate';

  async function confirm() {
    if (pending || reason.trim() === '') return;
    setPending(true);
    setError(undefined);
    try {
      await setAccountSuspended(target.account.userId, target.suspend, reason.trim());
      onDone();
    } catch (caught) {
      if (isSessionFailure(caught)) onSessionFailure(caught);
      setError(caught instanceof Error && caught.message ? caught.message : `Could not ${verb.toLowerCase()} the account`);
    } finally {
      setPending(false);
    }
  }

  return (
    <AgentDialog title={`${verb} account`} onClose={onClose}>
      <p className="text-sm">{target.account.displayName} ({target.account.email})</p>
      {target.suspend && <p className="text-xs text-muted-foreground">Pending and upcoming bookings will be cancelled and refunded.</p>}
      <label className="flex flex-col gap-1 text-xs font-semibold">
        Reason
        <textarea value={reason} maxLength={1000} onChange={(event) => setReason(event.target.value)} className="min-h-20 rounded-md border border-[var(--border)] p-2 text-sm" />
      </label>
      {error && <p role="alert" className="text-xs text-red-700">{error}</p>}
      <div className="flex justify-end">
        <button type="button" disabled={pending || reason.trim() === ''} onClick={() => void confirm()} className="rounded-md bg-primary px-4 py-2 text-xs font-bold text-primary-foreground disabled:opacity-50">
          Confirm
        </button>
      </div>
    </AgentDialog>
  );
}

export function AccountsTable() {
  const { data, error, loading, reload } = usePolling(fetchAccounts, 0);
  const [query, setQuery] = useState('');
  const [target, setTarget] = useState<Target | undefined>(undefined);
  const [sessionError, setSessionError] = useState<unknown>(undefined);

  const rows = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return (data ?? []).filter((a) => !needle || a.displayName.toLowerCase().includes(needle) || a.email.toLowerCase().includes(needle));
  }, [data, query]);

  const failure = [error, sessionError].find((candidate) => candidate !== undefined && isSessionFailure(candidate));
  if (failure) return <SessionNotice error={failure} />;

  return (
    <section className="mx-auto max-w-6xl p-6">
      <h1 className="text-xl font-extrabold">Account governance</h1>
      <p className="mb-4 text-sm text-muted-foreground">Suspend or reactivate Guest and Host accounts</p>
      <input
        aria-label="Search accounts"
        placeholder="Search..."
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        className="mb-4 w-72 rounded-md border border-[var(--border)] p-2 text-sm"
      />
      {error !== undefined && (
        <div role="alert" className="mb-3 flex items-center justify-between rounded-md border border-red-300 bg-red-50 p-3 text-sm text-red-800">
          <span>{error instanceof Error && error.message ? error.message : 'Could not load accounts'}</span>
          <button type="button" className="rounded-md border border-red-300 px-3 py-1 text-xs font-semibold" onClick={reload}>Retry</button>
        </div>
      )}
      <table className="w-full border-collapse text-left text-sm">
        <thead>
          <tr className="border-b border-[var(--border)] text-xs uppercase text-muted-foreground">
            <th scope="col" className="p-2">Display name</th>
            <th scope="col" className="p-2">Email</th>
            <th scope="col" className="p-2">Joined</th>
            <th scope="col" className="p-2">Role</th>
            <th scope="col" className="p-2">Status</th>
            <th scope="col" className="p-2">Action</th>
          </tr>
        </thead>
        <tbody>
          {loading && !data ? (
            <tr><td colSpan={6} className="p-4 text-muted-foreground">Loading accounts...</td></tr>
          ) : rows.length === 0 ? (
            <tr><td colSpan={6} className="p-4 text-muted-foreground">No accounts match your search</td></tr>
          ) : (
            rows.map((account) => (
              <tr key={account.userId} className="border-b border-[var(--border)]">
                <td className="p-2 font-semibold">{account.displayName}</td>
                <td className="p-2">{account.email}</td>
                <td className="p-2">{joined(account.createdAt)}</td>
                <td className="p-2">{account.role === 'HOST' ? 'Host' : 'Guest'}</td>
                <td className="p-2">
                  <span className={`rounded-full px-2 py-0.5 text-xs ${account.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                    {account.status === 'ACTIVE' ? 'Active' : 'Suspended'}
                  </span>
                  {account.status === 'SUSPENDED' && account.suspensionReason && (
                    <p className="text-xs text-muted-foreground">Reason: {account.suspensionReason}</p>
                  )}
                </td>
                <td className="p-2">
                  {account.status === 'ACTIVE' ? (
                    <button type="button" className="rounded-md border border-red-600 px-3 py-1 text-xs font-semibold text-red-700" onClick={() => setTarget({ account, suspend: true })}>
                      Suspend
                    </button>
                  ) : (
                    <button type="button" className="rounded-md border border-[var(--border)] px-3 py-1 text-xs font-semibold" onClick={() => setTarget({ account, suspend: false })}>
                      Reactivate
                    </button>
                  )}
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
      {target && (
        <ReasonDialog
          target={target}
          onClose={() => setTarget(undefined)}
          onDone={() => {
            setTarget(undefined);
            reload();
          }}
          onSessionFailure={setSessionError}
        />
      )}
    </section>
  );
}
