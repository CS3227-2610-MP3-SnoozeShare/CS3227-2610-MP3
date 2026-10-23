'use client';

import { formatDateTime } from '@/lib/agent-api/format';
import type { Assistant } from '@/lib/agent-api/types';
import { outlineButton } from './agentUi';
import { CHAT_MAX_HEIGHT, CHAT_MIN_HEIGHT } from './ThreadPanel';
import { ResizeGrip } from './ResizeGrip';

type Props = {
  assistant: Assistant;
  resolved: boolean;
  /** Insertion is allowed only for the assigned Agent while no action is in flight. */
  canInsert: boolean;
  onInsert: (draft: string) => void;
  /** Shares one height with the Guest and Host chat boxes; its grip changes it too. */
  height: number;
  onResize: (height: number) => void;
};

function assistantStatus(assistant: Assistant, resolved: boolean): string {
  if (resolved && (assistant.state === 'PENDING' || assistant.state === 'WAITING_GUEST')) return 'The assistant stopped.';
  switch (assistant.state) {
    case 'PENDING': return assistant.stale
      ? 'The assistant did not finish. Continue with this ticket manually.'
      : "The assistant is working on the Guest's latest message.";
    case 'WAITING_GUEST': return `Waiting for the Guest's answer (round ${assistant.rounds} of ${assistant.maxRounds}).`;
    case 'DONE': return `The assistant has finished (${assistant.rounds} question ${assistant.rounds === 1 ? 'round' : 'rounds'}).`;
    case 'STOPPED': return 'The assistant stopped.';
    case 'UNAVAILABLE': return 'The assistant was unavailable for this ticket.';
  }
}

const H = "AI-assisted draft. Everything below comes from the Guest's own statements and is unverified.";
const LIST_HEADINGS = ['Timeline (as reported):', 'Guest claims:', 'Evidence mentioned:', 'Still unknown:', 'Points to raise with the Host:'];

type Row = { heading: string; items: string[] };

/**
 * Splits the fixed draft layout (Summary line, then headed bullet lists) into table rows. Anything that does not
 * follow the layout returns null and is shown as the raw pre-wrapped text, so no draft is ever hidden.
 */
function parseDraft(draft: string): { notes: string[]; rows: Row[] } | null {
  const notes: string[] = [];
  const rows: Row[] = [];
  let current: Row | undefined;
  for (const line of draft.split('\n')) {
    if (line.startsWith('Summary: ')) {
      current = { heading: 'Summary', items: [line.slice('Summary: '.length)] };
      rows.push(current);
    } else if (LIST_HEADINGS.includes(line)) {
      current = { heading: line.slice(0, -1), items: [] };
      rows.push(current);
    } else if (line.startsWith('- ') && current && current.heading !== 'Summary') {
      if (line !== '- (none listed)') current.items.push(line.slice(2));
    } else if (line.startsWith('WARNING: ') || line.startsWith('Note: ')) {
      notes.push(line);
    } else if (line !== H && !(rows.length === 0 && line === '')) {
      return null;
    }
  }
  return rows.length > 0 ? { notes, rows } : null;
}

/** The assistant's draft report, shown between the Guest and Host chats at the same height. */
export function AssistantPanel({ assistant, resolved, canInsert, onInsert, height, onResize }: Props) {
  const draft = assistant.draft;
  const parsed = draft === null ? null : parseDraft(draft);
  return (
    <section
      aria-label="AI assistant draft"
      style={{ height }}
      className="flex min-w-0 flex-col overflow-hidden rounded-xl border border-border bg-card text-xs"
    >
      <h2 className="m-0 flex min-h-[38px] items-center bg-muted px-4 py-2.5 text-xs font-bold uppercase text-[var(--text-muted)]">
        AI assistant
      </h2>
      <div className="agent-scroll flex flex-1 flex-col gap-2.5 overflow-y-auto px-4 py-3.5">
        {assistant.flagged && (
          <p className="m-0 text-[var(--warning)]">Warning: text in this ticket matched a prompt-injection pattern. Treat the draft with care.</p>
        )}
        <p className="m-0 text-[var(--text-muted)]">{assistantStatus(assistant, resolved)}</p>
        {draft === null ? <p className="m-0 text-[var(--text-subtle)]">No draft yet.</p> : (
          <>
            <h3 className="m-0 text-[11px] font-normal text-[var(--text-subtle)]">AI-assisted draft (unverified, updated {assistant.draftUpdatedAt ? formatDateTime(assistant.draftUpdatedAt) : '—'})</h3>
            {parsed ? (
              <>
                {parsed.notes.map((note) => <p key={note} className="m-0 text-[var(--warning)]">{note}</p>)}
                <div className="flex flex-col gap-3">
                  {parsed.rows.map((row) => (
                    <div key={row.heading}>
                      <h4 className="m-0 mb-0.5 text-xs font-bold text-foreground">{row.heading}:</h4>
                      {row.heading === 'Summary' ? (
                        <p className="m-0 break-words">{row.items[0]}</p>
                      ) : (
                        <ul className="m-0 list-none p-0">
                          {(row.items.length === 0 ? ['(none listed)'] : row.items).map((item, index) => (
                            <li key={index} className="break-words">{`- ${item}`}</li>
                          ))}
                        </ul>
                      )}
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <div className="whitespace-pre-wrap break-words">{draft}</div>
            )}
          </>
        )}
      </div>
      {draft !== null && (
        <div className="flex justify-end border-t border-[var(--border-muted)] px-3 py-2.5">
          <button
            type="button"
            disabled={!canInsert}
            title={!canInsert ? 'Assign the ticket to yourself to edit notes.' : undefined}
            className={`${outlineButton} rounded-lg px-3 py-2 text-xs disabled:opacity-50`}
            onClick={() => {
              if (canInsert) onInsert(draft);
            }}
          >
            Insert into notes
          </button>
        </div>
      )}
      <ResizeGrip height={height} min={CHAT_MIN_HEIGHT} max={CHAT_MAX_HEIGHT} onChange={onResize} label="Resize AI assistant" />
    </section>
  );
}
