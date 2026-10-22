'use client';

import { useEffect, useState } from 'react';
import { isSessionFailure } from '@/lib/api';
import { createCategory, deleteCategory, fetchCategories, renameCategory, setCategoryActive } from '@/lib/agent-api/categories';
import type { TicketCategory } from '@/lib/agent-api/types';
import { AgentDialog } from './AgentDialog';
import { SessionNotice } from './SessionNotice';
import { usePolling } from './usePolling';

type Editing = { category: TicketCategory | null };

function message(caught: unknown, fallback: string): string {
  return caught instanceof Error && caught.message ? caught.message : fallback;
}

function CategoryDialog({ editing, onClose, onDone, onSessionFailure }: {
  editing: Editing;
  onClose: () => void;
  onDone: () => void;
  onSessionFailure: (error: unknown) => void;
}) {
  const existing = editing.category;
  const [label, setLabel] = useState(existing?.label ?? '');
  const [pending, setPending] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [error, setError] = useState<string | undefined>(undefined);

  async function run(action: () => Promise<unknown>, fallback: string) {
    if (pending) return;
    setPending(true);
    setError(undefined);
    try {
      await action();
      onDone();
    } catch (caught) {
      if (isSessionFailure(caught)) onSessionFailure(caught);
      setError(message(caught, fallback));
    } finally {
      setPending(false);
    }
  }

  const trimmed = label.trim();
  return (
    <AgentDialog title={existing ? 'Edit category' : 'Add category'} onClose={onClose}>
      <label className="flex flex-col gap-1 text-xs font-semibold">
        Label
        <input value={label} maxLength={60} onChange={(event) => setLabel(event.target.value)} className="rounded-md border border-[var(--border)] p-2 text-sm" />
      </label>
      {error && <p role="alert" className="text-xs text-red-700">{error}</p>}
      {confirmingDelete && existing && (
        <div className="flex items-center gap-2 rounded-md border border-red-300 p-2 text-xs">
          <span>Delete {existing.label}? This cannot be undone.</span>
          <button type="button" disabled={pending} className="rounded-md bg-red-700 px-3 py-1 font-bold text-white" onClick={() => void run(() => deleteCategory(existing.id), 'Could not delete the category')}>
            Confirm delete
          </button>
        </div>
      )}
      <div className="flex justify-between">
        {existing ? (
          <button type="button" className="rounded-md border border-red-600 px-3 py-2 text-xs font-semibold text-red-700" onClick={() => setConfirmingDelete(true)}>
            Delete
          </button>
        ) : <span />}
        <button
          type="button"
          disabled={pending || trimmed === ''}
          onClick={() => void run(() => (existing ? renameCategory(existing.id, trimmed) : createCategory(trimmed)), 'Could not save the category')}
          className="rounded-md bg-primary px-4 py-2 text-xs font-bold text-primary-foreground disabled:opacity-50"
        >
          Save
        </button>
      </div>
    </AgentDialog>
  );
}

export function CategoriesTable() {
  const { data, error, loading, reload } = usePolling(fetchCategories, 0);
  const [rows, setRows] = useState<TicketCategory[]>([]);
  const [editing, setEditing] = useState<Editing | undefined>(undefined);
  const [toggleError, setToggleError] = useState<string | undefined>(undefined);
  const [sessionError, setSessionError] = useState<unknown>(undefined);

  useEffect(() => {
    if (data) setRows(data);
  }, [data]);

  const failure = [error, sessionError].find((candidate) => candidate !== undefined && isSessionFailure(candidate));
  if (failure) return <SessionNotice error={failure} />;

  async function toggle(category: TicketCategory) {
    const next = !category.active;
    setToggleError(undefined);
    setRows((current) => current.map((row) => (row.id === category.id ? { ...row, active: next } : row)));
    try {
      await setCategoryActive(category.id, next);
    } catch (caught) {
      setRows((current) => current.map((row) => (row.id === category.id ? { ...row, active: category.active } : row)));
      if (isSessionFailure(caught)) setSessionError(caught);
      setToggleError(message(caught, 'Could not update the category'));
    }
  }

  return (
    <section className="mx-auto max-w-4xl p-6">
      <div className="mb-4 flex items-start justify-between">
        <div>
          <h1 className="text-xl font-extrabold">Ticket categories</h1>
          <p className="text-sm text-muted-foreground">Shown to guests when filing a dispute ticket</p>
        </div>
        <button type="button" onClick={() => setEditing({ category: null })} className="rounded-md bg-primary px-4 py-2 text-xs font-bold text-primary-foreground">
          + Add category
        </button>
      </div>
      {(error !== undefined || toggleError) && (
        <div role="alert" className="mb-3 flex items-center justify-between rounded-md border border-red-300 bg-red-50 p-3 text-sm text-red-800">
          <span>{toggleError ?? message(error, 'Could not load categories')}</span>
          {error !== undefined && (
            <button type="button" className="rounded-md border border-red-300 px-3 py-1 text-xs font-semibold" onClick={reload}>Retry</button>
          )}
        </div>
      )}
      <table className="w-full border-collapse text-left text-sm">
        <thead>
          <tr className="border-b border-[var(--border)] text-xs uppercase text-muted-foreground">
            <th scope="col" className="p-2">Label</th>
            <th scope="col" className="p-2">Active</th>
            <th scope="col" className="p-2">Action</th>
          </tr>
        </thead>
        <tbody>
          {loading && !data ? (
            <tr><td colSpan={3} className="p-4 text-muted-foreground">Loading categories...</td></tr>
          ) : rows.length === 0 ? (
            <tr><td colSpan={3} className="p-4 text-muted-foreground">No categories yet</td></tr>
          ) : (
            rows.map((category) => (
              <tr key={category.id} className="border-b border-[var(--border)]">
                <td className="p-2 font-semibold">{category.label}</td>
                <td className="p-2">
                  <input type="checkbox" role="switch" aria-label={`Toggle active: ${category.label}`} checked={category.active} onChange={() => void toggle(category)} />
                </td>
                <td className="p-2">
                  <button type="button" className="rounded-md border border-[var(--border)] px-3 py-1 text-xs font-semibold" onClick={() => setEditing({ category })}>
                    Edit
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
      {editing && (
        <CategoryDialog
          editing={editing}
          onClose={() => setEditing(undefined)}
          onDone={() => {
            setEditing(undefined);
            reload();
          }}
          onSessionFailure={setSessionError}
        />
      )}
    </section>
  );
}
