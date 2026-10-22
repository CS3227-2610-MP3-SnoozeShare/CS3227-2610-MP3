'use client';

import { useEffect, useRef, useState } from 'react';
import { Check, CircleAlert, X } from 'lucide-react';
import { postHostAiListingDescription, hostAiGatewayMessage, hostAiSafeErrorMessage, type AiAnalysis, type AiDescriptionLength, type AiMode, type AiTargetAudience, type AiWritingStyle, type HostAiListingDescriptionResponse, type ListingFactsPayload } from '@/lib/hostAiApi';
import { SelectField } from '@/components/ui/select';

type Props = {
  open: boolean;
  listingId?: string;
  listing: ListingFactsPayload;
  description: string;
  onApply: (description: string) => void;
  onClose: () => void;
};

const styles: { value: AiWritingStyle; label: string }[] = [
  { value: 'FRIENDLY', label: 'Friendly' }, { value: 'PROFESSIONAL', label: 'Professional' }, { value: 'PERSUASIVE', label: 'Persuasive' },
];
const audiences: { value: AiTargetAudience; label: string }[] = [
  { value: 'FAMILIES', label: 'Families' }, { value: 'COUPLES', label: 'Couples' }, { value: 'BUSINESS_TRAVELLERS', label: 'Business travellers' }, { value: 'ALL_TRAVELLERS', label: 'All travellers' },
];
const lengths: { value: AiDescriptionLength; label: string }[] = [
  { value: 'SHORT', label: 'Short' }, { value: 'MEDIUM', label: 'Medium' },
];

export function HostAiDescriptionAssistant({ open, listingId, listing, description, onApply, onClose }: Props) {
  const [draft, setDraft] = useState(description);
  const [style, setStyle] = useState<AiWritingStyle>('FRIENDLY');
  const [audience, setAudience] = useState<AiTargetAudience>('ALL_TRAVELLERS');
  const [length, setLength] = useState<AiDescriptionLength>('MEDIUM');
  const [pending, setPending] = useState<AiMode | null>(null);
  const [response, setResponse] = useState<HostAiListingDescriptionResponse | null>(null);
  const [error, setError] = useState('');
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; };
  }, []);

  useEffect(() => {
    if (!open) return;
    setDraft(description);
    setResponse((current) => current ? { ...current, currentAnalysis: null } : null);
  }, [open, description]);

  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !pending) {
        event.preventDefault();
        onClose();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose, pending]);

  if (!open) return null;
  const run = async (mode: AiMode) => {
    if (pending || (mode === 'EVALUATE' && !draft.trim())) return;
    setPending(mode);
    setResponse(null);
    setError('');
    try {
      const next = await postHostAiListingDescription({ listingId, mode, writingStyle: style, targetAudience: audience, length, currentDescription: draft, listing });
      if (!mounted.current) return;
      if (next.outcome === 'DEGRADED') setError(hostAiGatewayMessage(next.gatewayCode ?? 'AI_ERROR'));
      else setResponse(next);
    } catch (caught) {
      if (mounted.current) setError(hostAiSafeErrorMessage(caught));
    } finally {
      if (mounted.current) setPending(null);
    }
  };
  const applyGeneratedDescription = () => {
    if (!response?.generatedDescription) return;
    const generatedDescription = response.generatedDescription;
    setDraft(generatedDescription);
    setResponse(null);
    setError('');
    onApply(generatedDescription);
    onClose();
  };
  const disabled = pending !== null;
  return (
    <div className="fixed inset-0 z-50 flex min-h-dvh w-screen items-center justify-center bg-black/30 p-4" onClick={(event) => { if (!disabled && event.target === event.currentTarget) onClose(); }} role="presentation">
      <div aria-describedby="host-ai-description" aria-labelledby="host-ai-title" aria-modal="true" className="ss-card max-h-[calc(100dvh-2rem)] w-full max-w-5xl overflow-y-auto p-5" role="dialog">
        <div className="flex items-start justify-between gap-4">
          <div><h2 className="text-lg font-semibold" id="host-ai-title">AI Description Assistant ✨</h2><p className="mt-1 text-sm text-[var(--text-subtle)]" id="host-ai-description">Review the current description or generate a grounded suggestion.</p></div>
          <button aria-label="Close AI Description Assistant" className="rounded-md p-1 text-[var(--text-subtle)] hover:bg-muted hover:text-foreground" disabled={disabled} onClick={onClose} title="Close" type="button"><X aria-hidden="true" className="size-5" /></button>
        </div>
        <div className="mt-5 space-y-5">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              <label className="flex flex-col gap-1.5 text-xs font-semibold text-[var(--text-muted)]"><span>Writing style</span><SelectField aria-label="Writing style" disabled={disabled} name="ai-writing-style" options={styles} value={style} onValueChange={(value) => setStyle(value as AiWritingStyle)} /></label>
              <label className="flex flex-col gap-1.5 text-xs font-semibold text-[var(--text-muted)]"><span>Target audience</span><SelectField aria-label="Target audience" disabled={disabled} name="ai-target-audience" options={audiences} value={audience} onValueChange={(value) => setAudience(value as AiTargetAudience)} /></label>
            <fieldset className="space-y-2" disabled={disabled}><legend className="text-xs font-semibold text-[var(--text-muted)]">Description length</legend><div className="flex flex-wrap gap-4">{lengths.map((option) => <label className="flex items-center gap-2 text-sm" key={option.value}><input checked={length === option.value} name="ai-description-length" onChange={() => setLength(option.value)} type="radio" />{option.label}</label>)}</div></fieldset>
          </div>
          <div className="grid gap-5 lg:grid-cols-2">
            <label className="flex flex-col gap-1.5 text-xs font-semibold text-[var(--text-muted)]"><span>Current description</span><textarea aria-label="Current description" className="min-h-44 rounded-lg border border-input bg-background p-3 text-sm font-normal outline-none focus:border-ring focus:ring-3 focus:ring-ring/20" disabled={disabled} value={draft} onChange={(event) => setDraft(event.target.value)} /></label>
            <label className="flex flex-col gap-1.5 text-xs font-semibold text-[var(--text-muted)]">Generated description<textarea aria-label="Generated description" className="min-h-44 rounded-lg border border-input bg-[var(--surface-inset)] p-3 text-sm font-normal" readOnly value={response?.generatedDescription ?? ''} /></label>
          </div>
          <div className="grid gap-5 lg:grid-cols-2">
            <section aria-label="AI input" className="space-y-4">
              <AnalysisPanel heading="Current analysis" analysis={response?.currentAnalysis ?? null} />
              {error && <p className="text-sm text-destructive" role="alert">{error}</p>}
              <div className="flex flex-wrap items-center gap-2"><button className="rounded-lg border border-border px-4 py-2 text-sm font-semibold" disabled={disabled || !draft.trim()} onClick={() => void run('EVALUATE')} type="button">{pending === 'EVALUATE' ? 'Evaluating…' : 'Evaluate'}</button><button className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground" disabled={disabled} onClick={() => void run('GENERATE')} type="button">{pending === 'GENERATE' ? 'Generating…' : 'Generate and evaluate'}</button>{pending && <span aria-label="AI request loading" className="text-sm text-[var(--text-subtle)]" role="status">Loading…</span>}</div>
            </section>
            <section aria-label="AI output" className="space-y-4">
            <AnalysisPanel heading="Generated analysis" analysis={response?.generatedAnalysis ?? null} />
            <button className="rounded-lg bg-[var(--success)] px-4 py-2 text-sm font-semibold text-white" disabled={disabled || !response?.generatedDescription} onClick={applyGeneratedDescription} type="button">Apply generated description</button>
            </section>
          </div>
        </div>
      </div>
    </div>
  );
}

function AnalysisPanel({ heading, analysis }: { heading: string; analysis: AiAnalysis | null }) {
  const rows: [string, keyof AiAnalysis][] = [['Clarity', 'clarity'], ['Completeness', 'completeness'], ['Accuracy', 'accuracy']];
  return <div><h3 className="text-sm font-semibold">{heading}</h3><div className="mt-2 divide-y rounded-lg border border-border">{rows.map(([label, key]) => { const criterion = analysis?.[key]; const passes = criterion?.status === 'PASS'; return <div className="grid gap-2 p-3 text-sm sm:grid-cols-[7rem_1fr]" key={key}><span className="font-medium">{label}</span><span className="flex items-start gap-2"><span aria-hidden="true" className="mt-0.5 shrink-0">{criterion && (passes ? <Check className="size-4 text-emerald-600" data-testid="analysis-pass-icon" /> : <CircleAlert className="size-4 text-orange-500" data-testid="analysis-improve-icon" />)}</span><span className={criterion?.status === 'IMPROVE' ? 'text-orange-700' : 'text-[var(--text-subtle)]'}>{criterion?.feedback ?? '—'}</span></span></div>; })}</div></div>;
}
