'use client';

import { useMemo, useState } from 'react';
import { isSessionFailure } from '@/lib/api';
import { fetchAccounts, setAccountSuspended } from '@/lib/agent-api/accounts';
import type { AccountSummary } from '@/lib/agent-api/types';
import { AgentDialog, FieldLabel, confirmButton, dialogArea } from './AgentDialog';
import { AgentGrid, Cell, GridMessage, GridRow, type GridColumn } from './AgentGrid';
import { ErrorBanner, Pill, outlineButton, dangerOutlineButton, pageClass, titleClass } from './agentUi';
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
  const { account } = target;

  async function confirm() {
    if (pending || reason.trim() === '') return;
    setPending(true);
    setError(undefined);
    try {
      await setAccountSuspended(account.userId, target.suspend, reason.trim());
      onDone();
    } catch (caught) {
      if (isSessionFailure(caught)) onSessionFailure(caught);
      setError(caught instanceof Error && caught.message ? caught.message : `Could not ${verb.toLowerCase()} the account`);
    } finally {
      setPending(false);
    }
  }

  return (
    <AgentDialog
      title={`${verb} account`}
      width={460}
      onClose={onClose}
      confirm={
        <button type="button" disabled={pending || reason.trim() === ''} onClick={() => void confirm()} className={`${confirmButton(target.suspend ? 'danger' : 'success')} disabled:opacity-50`}>
          {target.suspend ? 'Confirm suspend' : 'Confirm reactivate'}
        </button>
      }
    >
      <div className={`flex flex-col gap-1 rounded-xl p-[18px] text-[#fdf8f0] ${target.suspend ? 'bg-destructive' : 'bg-[var(--success)]'}`}>
        <div className="text-2xl font-extrabold">{account.displayName}</div>
        <div className="text-[13px] opacity-90">{account.email}</div>
        <div className="text-[13px] opacity-90">{account.role === 'HOST' ? 'Host' : 'Guest'} · joined {joined(account.createdAt)}</div>
      </div>
      <div className="rounded-lg bg-muted px-3 py-2.5 text-xs text-[var(--text-muted)]">
        {target.suspend
          ? 'Pending and upcoming bookings will be cancelled and refunded.'
          : 'The user can sign in again. Bookings and listings cancelled by the suspension are not restored.'}
      </div>
      <label className="block">
        <FieldLabel>Reason (required, written to the audit log)</FieldLabel>
        <textarea
          value={reason}
          maxLength={1000}
          placeholder="e.g. repeated late cancellations flagged across 3 bookings"
          onChange={(event) => setReason(event.target.value)}
          className={dialogArea}
        />
      </label>
      {error && <p role="alert" className="m-0 text-xs text-destructive">{error}</p>}
    </AgentDialog>
  );
}

const COLUMNS: GridColumn[] = [
  { label: 'Display name', min: 130, fr: 1.2 },
  { label: 'Email', min: 190, fr: 1.7 },
  { label: 'Role', min: 70, fr: 0.6 },
  { label: 'Joined', min: 90, fr: 0.8 },
  { label: 'Status', min: 110, fr: 1.2 },
  { label: 'Action', min: 110, fr: 0.9 },
];

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
    <section className={pageClass}>
      <div className="flex flex-wrap items-center gap-2.5">
        <div>
          <h1 className={titleClass}>Account governance</h1>
          <div className="mt-0.5 text-xs text-[var(--text-subtle)]">Suspend or reactivate Guest and Host accounts</div>
        </div>
        <input
          aria-label="Search accounts"
          placeholder="Search..."
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          className="ml-auto box-border w-[260px] rounded-lg border border-border bg-card px-3 py-2 text-xs"
        />
      </div>
      {error !== undefined && (
        <ErrorBanner message={error instanceof Error && error.message ? error.message : 'Could not load accounts'} onRetry={reload} />
      )}
      <AgentGrid columns={COLUMNS} label="Accounts">
        {loading && !data ? (
          <GridMessage>Loading accounts...</GridMessage>
        ) : rows.length === 0 ? (
          <GridMessage>No accounts match your search</GridMessage>
        ) : (
          rows.map((account) => (
            <GridRow key={account.userId}>
              <Cell className="font-bold">{account.displayName}</Cell>
              <Cell className="break-all text-[var(--text-muted)]">{account.email}</Cell>
              <Cell className="text-[var(--text-muted)]">{account.role === 'HOST' ? 'Host' : 'Guest'}</Cell>
              <Cell className="text-[var(--text-muted)]">{joined(account.createdAt)}</Cell>
              <Cell>
                <Pill tone={account.status === 'ACTIVE' ? 'success' : 'danger'}>{account.status === 'ACTIVE' ? 'Active' : 'Suspended'}</Pill>
                {account.status === 'SUSPENDED' && account.suspensionReason && (
                  <div className="mt-[3px] text-[11px] text-[var(--text-subtle)]">Reason: {account.suspensionReason}</div>
                )}
              </Cell>
              <Cell>
                {account.status === 'ACTIVE' ? (
                  <button type="button" className={`${dangerOutlineButton} px-3.5 py-1.5 text-[11px]`} onClick={() => setTarget({ account, suspend: true })}>
                    Suspend
                  </button>
                ) : (
                  <button type="button" className={`${outlineButton} px-3.5 py-1.5 text-[11px] font-bold`} onClick={() => setTarget({ account, suspend: false })}>
                    Reactivate
                  </button>
                )}
              </Cell>
            </GridRow>
          ))
        )}
      </AgentGrid>
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
