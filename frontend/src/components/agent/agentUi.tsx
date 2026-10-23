import type { ReactNode } from 'react';
import { cn } from 'cn';

// The look of the Support Agent boards in the design artifact (Fall Light tokens from globals.css).

export type Tone = 'danger' | 'warning' | 'success' | 'accent' | 'neutral';

const TONES: Record<Tone, string> = {
  danger: 'bg-[var(--danger-subtle)] text-destructive',
  warning: 'bg-[var(--warning-subtle)] text-[var(--warning)]',
  success: 'bg-[var(--success-subtle)] text-[var(--success)]',
  accent: 'bg-accent text-primary',
  neutral: 'bg-muted text-[#502c20]',
};

export function Pill({ tone, children, className }: { tone: Tone; children: ReactNode; className?: string }) {
  return (
    <span className={cn('inline-block whitespace-nowrap rounded-full px-[11px] py-[3px] text-[10px] font-bold uppercase', TONES[tone], className)}>
      {children}
    </span>
  );
}

/** Page body of every agent screen: 24px / 32px padding, 16px rhythm, full width. */
export const pageClass = 'flex flex-col gap-4 px-8 py-6';
export const titleClass = 'm-0 text-xl font-extrabold';
export const inputClass = 'box-border w-full rounded-lg border border-border bg-background px-2.5 py-2 text-xs';
export const primaryButton = 'rounded-lg bg-primary font-bold text-primary-foreground disabled:opacity-50';
export const outlineButton = 'rounded-lg border border-border bg-transparent font-semibold text-[var(--text-muted)] disabled:opacity-50';
export const dangerOutlineButton = 'rounded-lg border border-[#f07878] bg-transparent font-bold text-destructive disabled:opacity-50';

export function ErrorBanner({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div role="alert" className="flex items-center justify-between rounded-lg border border-[#f07878] bg-[var(--danger-subtle)] px-4 py-2.5 text-[13px] text-destructive">
      <span>{message}</span>
      {onRetry && (
        <button type="button" className={`${outlineButton} px-3 py-1 text-xs`} onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  );
}
