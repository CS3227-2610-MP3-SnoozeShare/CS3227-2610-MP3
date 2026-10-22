'use client';

import { useCallback, useEffect, useState } from 'react';
import { searchListings, type SearchResult } from '@/lib/guest-api';
import { BackendApiError } from '@/lib/api';
import { ListingCard } from '@/components/guest/ListingCard';
import { ListingDetailSheet } from '@/components/guest/ListingDetailSheet';

export default function SearchPage() {
  const [city, setCity] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [guests, setGuests] = useState('');
  const [maxRate, setMaxRate] = useState('');

  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedListing, setSelectedListing] = useState<string | null>(null);

  const doSearch = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await searchListings({
        city: city || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        guests: guests ? Number(guests) : undefined,
        maxNightlyRate: maxRate ? Number(maxRate) : undefined,
      });
      setResults(data);
    } catch (e) {
      setError(e instanceof BackendApiError ? e.message : 'Failed to load listings');
    } finally {
      setLoading(false);
    }
  }, [city, startDate, endDate, guests, maxRate]);

  useEffect(() => {
    doSearch();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const today = new Date().toISOString().split('T')[0];

  return (
    <main aria-label="Search listings" className="min-h-[calc(100vh-4rem)] p-6">
      {/* Filter bar */}
      <form
        data-testid="search-filters"
        onSubmit={(e) => { e.preventDefault(); doSearch(); }}
        className="ss-card mb-6 flex flex-wrap items-end gap-3 p-4"
      >
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-muted-foreground">City</label>
          <input type="text" value={city} onChange={(e) => setCity(e.target.value)} placeholder="Any city"
            className="w-36 rounded border px-2 py-1.5 text-sm" />
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-muted-foreground">Check-in</label>
          <input type="date" min={today} value={startDate} onChange={(e) => setStartDate(e.target.value)}
            className="rounded border px-2 py-1.5 text-sm" />
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-muted-foreground">Check-out</label>
          <input type="date" min={startDate || today} value={endDate} onChange={(e) => setEndDate(e.target.value)}
            className="rounded border px-2 py-1.5 text-sm" />
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-muted-foreground">Guests</label>
          <input type="number" min="1" value={guests} onChange={(e) => setGuests(e.target.value)} placeholder="Any"
            className="w-20 rounded border px-2 py-1.5 text-sm" />
        </div>
        <div className="flex flex-col gap-1">
          <label className="text-xs font-medium text-muted-foreground">Max rate</label>
          <input type="number" min="0" value={maxRate} onChange={(e) => setMaxRate(e.target.value)} placeholder="Any"
            className="w-24 rounded border px-2 py-1.5 text-sm" />
        </div>
        <button type="submit" className="rounded bg-primary px-4 py-1.5 text-sm font-medium text-primary-foreground hover:opacity-90">
          Search
        </button>
      </form>

      {/* Results */}
      {loading && <p className="text-muted-foreground">Loading listings...</p>}
      {error && <p className="text-red-600">{error}</p>}
      {!loading && !error && results.length === 0 && (
        <p data-testid="empty-state" className="text-muted-foreground">No listings found. Try adjusting your filters.</p>
      )}
      {!loading && !error && results.length > 0 && (
        <div data-testid="results-grid" className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {results.map((r) => (
            <ListingCard key={r.property.id} result={r} onSelect={setSelectedListing} />
          ))}
        </div>
      )}

      {/* Detail sheet */}
      <ListingDetailSheet listingId={selectedListing} onClose={() => setSelectedListing(null)} />
    </main>
  );
}
