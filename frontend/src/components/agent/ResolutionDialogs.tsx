'use client';

import { useState } from 'react';
import { resolveDispute } from '@/lib/agent-api/disputes';
import { formatSgd, isMoneyFormat } from '@/lib/agent-api/format';
import type { DisputeDetail, ResolutionMode } from '@/lib/agent-api/types';
import { AgentDialog } from './AgentDialog';

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

const TITLE: Record<ResolutionKind, string> = {
  ACCEPT: 'Accept - remedy guest',
  REJECT: 'Reject dispute',
  MANUAL: 'Manual wallet adjustment',
};
const CONFIRM: Record<ResolutionKind, string> = { ACCEPT: 'Confirm accept', REJECT: 'Confirm reject', MANUAL: 'Confirm' };

function isPositive(text: string): boolean {
  return isMoneyFormat(text) && /[1-9]/.test(text);
}

export function ResolutionDialog({ kind, detail, onClose, onResolved, onFailed }: Props) {
  const remedy = detail.ticket.requestedRemedy;
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
    <AgentDialog title={TITLE[kind]} onClose={onClose}>
      {kind === 'ACCEPT' && remedy === 'FULL_REFUND' && <p className="text-sm">Full refund {formatSgd(detail.escrowAmount)}</p>}
      {kind === 'ACCEPT' && remedy === 'HOST_PAYOUT' && <p className="text-sm">Full payout to host</p>}
      {asksAmount && (
        <label className="flex flex-col gap-1 text-xs font-semibold">
          Guest refund (SGD)
          <input value={amount} inputMode="decimal" onChange={(event) => setAmount(event.target.value)} className="rounded-md border border-[var(--border)] p-2 text-sm" />
        </label>
      )}
      {kind === 'MANUAL' && (
        <>
          <label className="flex flex-col gap-1 text-xs font-semibold">
            Override type
            <select value={override} onChange={(event) => setOverride(event.target.value as Override)} className="rounded-md border border-[var(--border)] p-2 text-sm">
              <option value="FULL_REFUND">Full refund to guest</option>
              <option value="HOST_PAYOUT">Full payout to host</option>
              <option value="CUSTOM">Custom</option>
            </select>
          </label>
          <label className="flex flex-col gap-1 text-xs font-semibold">
            Guest refund (SGD)
            <input
              value={manualAmount}
              readOnly={override !== 'CUSTOM'}
              inputMode="decimal"
              onChange={(event) => setAmount(event.target.value)}
              className="rounded-md border border-[var(--border)] p-2 text-sm"
            />
          </label>
        </>
      )}
      <label className="flex flex-col gap-1 text-xs font-semibold">
        Reason
        <textarea value={reason} maxLength={1000} onChange={(event) => setReason(event.target.value)} className="min-h-20 rounded-md border border-[var(--border)] p-2 text-sm" />
      </label>
      {error && <p role="alert" className="text-xs text-red-700">{error}</p>}
      <div className="flex justify-end">
        <button
          type="button"
          disabled={!canConfirm}
          onClick={() => void confirm()}
          className="rounded-md bg-primary px-4 py-2 text-xs font-bold text-primary-foreground disabled:opacity-50"
        >
          {CONFIRM[kind]}
        </button>
      </div>
    </AgentDialog>
  );
}
