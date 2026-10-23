'use client';

import { useState } from 'react';
import { resolveDispute } from '@/lib/agent-api/disputes';
import { formatSgd, isMoneyFormat, shortName } from '@/lib/agent-api/format';
import type { DisputeDetail, RemedyType, ResolutionMode } from '@/lib/agent-api/types';
import { AgentDialog, FieldLabel, confirmButton, dialogArea, dialogInput } from './AgentDialog';

export type ResolutionKind = 'ACCEPT' | 'REJECT' | 'MANUAL';
type Override = 'FULL_REFUND' | 'HOST_PAYOUT' | 'CUSTOM';

type Props = {
  kind: ResolutionKind;
  detail: DisputeDetail;
  onClose: () => void;
  onResolved: (detail: DisputeDetail) => void;
  /** Called after any failed resolve so the page can refetch: the outcome may be unknown. */
  onFailed: (error: unknown) => void;
};

const REMEDY_NAME: Record<RemedyType, string> = {
  FULL_REFUND: 'Full refund',
  PARTIAL_REFUND: 'Partial refund',
  HOST_PAYOUT: 'Full payout',
  OTHER: 'Other remedy',
};

const PLACEHOLDER: Record<ResolutionKind, string> = {
  ACCEPT: 'e.g. confirmed the issue, issuing the requested remedy per policy',
  REJECT: 'e.g. no evidence of a policy violation found',
  MANUAL: 'e.g. partial refund for confirmed noise disturbance',
};

const CONFIRM: Record<ResolutionKind, string> = { ACCEPT: 'Confirm accept', REJECT: 'Confirm reject', MANUAL: 'Apply AGENT_OVERRIDE' };
const TONE = { ACCEPT: 'success', REJECT: 'danger', MANUAL: 'accent' } as const;

function isPositive(text: string): boolean {
  return isMoneyFormat(text) && /[1-9]/.test(text);
}

const OVERRIDES: { value: Override; label: string }[] = [
  { value: 'FULL_REFUND', label: 'Full refund to guest' },
  { value: 'HOST_PAYOUT', label: 'Full payout to host' },
  { value: 'CUSTOM', label: 'Custom' },
];

export function ResolutionDialog({ kind, detail, onClose, onResolved, onFailed }: Props) {
  const remedy = detail.ticket.requestedRemedy;
  const hostRaised = detail.ticket.raisedByRole === 'HOST';
  const asksAmount = kind === 'ACCEPT' && (remedy === 'PARTIAL_REFUND' || remedy === 'OTHER');
  const [reason, setReason] = useState('');
  const [amount, setAmount] = useState('');
  const [override, setOverride] = useState<Override>('FULL_REFUND');
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | undefined>(undefined);

  const escrow = detail.escrowAmount.toFixed(2);
  const manualAmount = override === 'FULL_REFUND' ? escrow : override === 'HOST_PAYOUT' ? '0.00' : amount;
  const amountOk = kind === 'MANUAL' ? isMoneyFormat(manualAmount) : asksAmount ? isPositive(amount) : true;
  const canConfirm = reason.trim() !== '' && amountOk && !pending;

  const title = kind === 'ACCEPT' ? `Accept — remedy ${hostRaised ? 'host' : 'guest'}` : kind === 'REJECT' ? 'Reject dispute' : 'Manual wallet adjustment';
  const subtitle = `Ticket ${detail.ticketLabel} · ${detail.listingTitle} · ${shortName(detail.guestName)} vs ${shortName(detail.hostName)}`;
  const target = shortName(hostRaised ? detail.hostName : detail.guestName);
  const remedyLine = kind === 'REJECT'
    ? `${REMEDY_NAME[remedy]} — will not be issued`
    : `${REMEDY_NAME[remedy]} ${remedy === 'OTHER' ? 'for' : 'to'} ${target}`;

  async function confirm() {
    if (!canConfirm) return;
    setPending(true);
    setError(undefined);
    const guestRefund = kind === 'MANUAL' ? manualAmount : asksAmount ? amount : null;
    try {
      const updated = await resolveDispute(detail.ticket.id, {
        mode: kind as ResolutionMode,
        guestRefund,
        reason: reason.trim(),
      });
      onResolved(updated);
    } catch (caught) {
      setError(caught instanceof Error && caught.message ? caught.message : 'Could not resolve the dispute');
      onFailed(caught);
    } finally {
      setPending(false);
    }
  }

  return (
    <AgentDialog
      title={title}
      subtitle={subtitle}
      onClose={onClose}
      confirm={
        <button type="button" disabled={!canConfirm} onClick={() => void confirm()} className={`${confirmButton(TONE[kind])} disabled:opacity-50`}>
          {CONFIRM[kind]}
        </button>
      }
    >
      {kind !== 'MANUAL' && (
        <div className={`rounded-xl p-4 text-[#fdf8f0] ${kind === 'REJECT' ? 'bg-destructive' : 'bg-[var(--success)]'}`}>
          <div className="text-[13px] opacity-90">Requested remedy</div>
          <div className="text-lg font-extrabold">{remedyLine}</div>
        </div>
      )}
      {kind === 'ACCEPT' && remedy === 'FULL_REFUND' && (
        <p className="m-0 rounded-lg bg-muted px-3 py-2 text-xs">Full refund {formatSgd(detail.escrowAmount)}</p>
      )}
      {kind === 'ACCEPT' && remedy === 'HOST_PAYOUT' && <p className="m-0 rounded-lg bg-muted px-3 py-2 text-xs">Full payout to host</p>}
      {asksAmount && (
        <label className="block">
          <FieldLabel>Guest refund (SGD)</FieldLabel>
          <input value={amount} inputMode="decimal" onChange={(event) => setAmount(event.target.value)} className={dialogInput} />
        </label>
      )}
      {kind === 'MANUAL' && (
        <>
          <div role="group" aria-label="Override type">
            <FieldLabel>Override type</FieldLabel>
            <div className="flex gap-1.5">
              {OVERRIDES.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  aria-pressed={override === option.value}
                  onClick={() => setOverride(option.value)}
                  className={`flex-1 rounded-lg border px-1 py-[9px] text-[11px] ${
                    override === option.value
                      ? 'border-primary bg-accent font-bold text-primary'
                      : 'border-border font-semibold text-[var(--text-muted)]'
                  }`}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>
          <label className="block">
            <FieldLabel>Guest refund (SGD)</FieldLabel>
            <input
              value={manualAmount}
              readOnly={override !== 'CUSTOM'}
              inputMode="decimal"
              onChange={(event) => setAmount(event.target.value)}
              className={dialogInput}
            />
            <span className="mt-1 block text-[10px] text-[var(--text-subtle)]">
              Locked to the full escrowed amount for “Full refund” and to 0.00 for “Full payout”; editable when Custom is selected.
            </span>
          </label>
        </>
      )}
      <label className="block">
        <FieldLabel>Reason (required, written to the audit log)</FieldLabel>
        <textarea value={reason} maxLength={1000} placeholder={PLACEHOLDER[kind]} onChange={(event) => setReason(event.target.value)} className={dialogArea} />
      </label>
      {error && <p role="alert" className="m-0 text-xs text-destructive">{error}</p>}
    </AgentDialog>
  );
}
