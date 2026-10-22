'use client';

import type { SearchResult } from '@/lib/guest-api';

export function ListingCard({
  result,
  onSelect,
}: {
  result: SearchResult;
  onSelect: (id: string) => void;
}) {
  const p = result.property;
  return (
    <button
      type="button"
      onClick={() => onSelect(p.id)}
      className="ss-card flex w-full cursor-pointer flex-col gap-2 p-4 text-left transition-shadow hover:shadow-md"
    >
      <h3 className="text-base font-semibold text-foreground">{p.title}</h3>
      <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
        <span className="ss-pill ss-status-neutral text-xs">{p.propertyType}</span>
        <span>{p.city}</span>
        {p.district && <span>· {p.district}</span>}
      </div>
      <p className="line-clamp-2 text-sm text-muted-foreground">{p.description}</p>
      <div className="mt-auto flex items-center justify-between pt-2">
        <span className="text-lg font-bold text-primary">
          ${p.baseNightlyRate.toFixed(0)}<span className="text-sm font-normal text-muted-foreground"> / night</span>
        </span>
        <span className="text-xs text-muted-foreground">
          Up to {p.maxGuests} guest{p.maxGuests !== 1 ? 's' : ''}
        </span>
      </div>
    </button>
  );
}
