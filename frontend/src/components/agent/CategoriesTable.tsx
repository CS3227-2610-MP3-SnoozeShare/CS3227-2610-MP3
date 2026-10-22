'use client';

import { useEffect, useState } from 'react';
import { isSessionFailure } from '@/lib/api';
import { createCategory, deleteCategory, fetchCategories, renameCategory, setCategoryActive } from '@/lib/agent-api/categories';
import type { TicketCategory } from '@/lib/agent-api/types';
import { AgentDialog, FieldLabel, confirmButton, dialogInput } from './AgentDialog';
import { ErrorBanner, dangerOutlineButton, outlineButton, pageClass, primaryButton, rowClass, tableCardClass, tdClass, thClass, theadRowClass, titleClass } from './agentUi';
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
    <AgentDialog
      title={existing ? 'Edit category' : 'Add category'}
      subtitle={existing ? 'Rename, or delete a category no ticket uses' : 'Guests will see it when filing a dispute ticket'}
      onClose={onClose}
      leftAction={
        existing ? (
          <button type="button" className={`${dangerOutlineButton} px-[18px] py-[9px] text-xs`} onClick={() => setConfirmingDelete(true)}>
            Delete
          </button>
        ) : undefined
      }
      confirm={
        <button
          type="button"
          disabled={pending || trimmed === ''}
          onClick={() => void run(() => (existing ? renameCategory(existing.id, trimmed) : createCategory(trimmed)), 'Could not save the category')}
          className={`${confirmButton('accent')} disabled:opacity-50`}
        >
          Save
        </button>
      }
    >
      <label className="block">
        <FieldLabel>Label</FieldLabel>
        <input value={label} maxLength={60} onChange={(event) => setLabel(event.target.value)} className={dialogInput} />
      </label>
      {error && <p role="alert" className="m-0 text-xs text-destructive">{error}</p>}
      {confirmingDelete && existing && (
        <div className="flex items-center gap-2 rounded-lg border border-[#f07878] px-3 py-2 text-xs">
          <span className="flex-1">Delete {existing.label}? This cannot be undone.</span>
          <button type="button" disabled={pending} className={`${confirmButton('danger')} px-3 py-1.5 disabled:opacity-50`} onClick={() => void run(() => deleteCategory(existing.id), 'Could not delete the category')}>
            Confirm delete
          </button>
        </div>
      )}
    </AgentDialog>
  );
}

export function CategoriesTable() {
  const { data, error, loading, reload } = usePolling(fetchCategories, 0);
  const [rows, setRows] = useState<TicketCategory[]>([]);
  const [editing, setEditing] = useState<Editing | undefined>(undefined);
  const [toggling, setToggling] = useState<string | undefined>(undefined);
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
    setToggling(category.id);
    setRows((current) => current.map((row) => (row.id === category.id ? { ...row, active: next } : row)));
    try {
      await setCategoryActive(category.id, next);
    } catch (caught) {
      setRows((current) => current.map((row) => (row.id === category.id ? { ...row, active: category.active } : row)));
      if (isSessionFailure(caught)) setSessionError(caught);
      setToggleError(message(caught, 'Could not update the category'));
    } finally {
      setToggling(undefined);
    }
  }

  return (
    <section className={pageClass}>
      <div className="flex items-center">
        <div>
          <h1 className={titleClass}>Ticket categories</h1>
          <div className="mt-0.5 text-xs text-[var(--text-subtle)]">Shown to guests when filing a dispute ticket</div>
        </div>
        <button type="button" onClick={() => setEditing({ category: null })} className={`${primaryButton} ml-auto px-[18px] py-[9px] text-[13px]`}>
          + Add category
        </button>
      </div>
      {(error !== undefined || toggleError) && (
        <ErrorBanner message={toggleError ?? message(error, 'Could not load categories')} onRetry={error !== undefined ? reload : undefined} />
      )}
      <div className={tableCardClass}>
        <table className="w-full table-fixed border-collapse text-left">
          <colgroup>
            <col style={{ width: '50%' }} /><col style={{ width: '25%' }} /><col style={{ width: '25%' }} />
          </colgroup>
          <thead>
            <tr className={theadRowClass}>
              <th scope="col" className={thClass}>Label</th>
              <th scope="col" className={thClass}>Active</th>
              <th scope="col" className={thClass}>Action</th>
            </tr>
          </thead>
          <tbody>
            {loading && !data ? (
              <tr><td colSpan={3} className={`${tdClass} text-[var(--text-subtle)]`}>Loading categories...</td></tr>
            ) : rows.length === 0 ? (
              <tr><td colSpan={3} className={`${tdClass} text-[var(--text-subtle)]`}>No categories yet</td></tr>
            ) : (
              rows.map((category) => (
                <tr key={category.id} className={rowClass}>
                  <td className={`${tdClass} font-bold`}>{category.label}</td>
                  <td className={tdClass}>
                    <button
                      type="button"
                      role="switch"
                      aria-checked={category.active}
                      aria-label={`Toggle active: ${category.label}`}
                      disabled={toggling === category.id}
                      onClick={() => void toggle(category)}
                      className={`relative h-[22px] w-10 rounded-full p-0 disabled:opacity-60 ${category.active ? 'bg-[var(--success)]' : 'bg-border'}`}
                    >
                      <span className={`absolute top-0.5 size-[18px] rounded-full bg-white ${category.active ? 'left-5' : 'left-0.5'}`} />
                    </button>
                  </td>
                  <td className={tdClass}>
                    <button type="button" className={`${outlineButton} px-3.5 py-1.5 text-[11px]`} onClick={() => setEditing({ category })}>
                      Edit
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
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
