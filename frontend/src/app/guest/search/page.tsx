'use client';

import { useCallback, useEffect, useState } from 'react';
import { searchListings, aiSearch, type SearchResult, type AiSearchFiltersResponse } from '@/lib/guest-api';
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

  // AI search state
  const [aiQuery, setAiQuery] = useState('');
  const [aiResults, setAiResults] = useState<SearchResult[] | null>(null);
  const [aiExplanation, setAiExplanation] = useState<string | null>(null);
  const [aiFilters, setAiFilters] = useState<AiSearchFiltersResponse | null>(null);
  const [aiAvailable, setAiAvailable] = useState(true);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState<string | null>(null);

  const clearAiState = useCallback(() => {
    setAiResults(null);
    setAiExplanation(null);
    setAiFilters(null);
    setAiAvailable(true);
    setAiError(null);
  }, []);

  const doSearch = useCallback(async () => {
    setLoading(true);
    setError(null);
    clearAiState();
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
  }, [city, startDate, endDate, guests, maxRate, clearAiState]);

  const doAiSearch = useCallback(async () => {
    if (!aiQuery.trim()) return;
    setAiLoading(true);
    setAiError(null);
    clearAiState();
    try {
      const data = await aiSearch(aiQuery);
      if (data.aiAvailable) {
        setAiResults(data.results);
        setAiExplanation(data.explanation);
        setAiFilters(data.filters);
        setAiAvailable(true);
      } else {
        setAiAvailable(false);
        setAiError(data.errorCode ?? 'AI search is temporarily unavailable. Please use the manual filters.');
      }
    } catch (e) {
      setAiError(e instanceof BackendApiError ? e.message : 'AI search failed. Please try again.');
    } finally {
      setAiLoading(false);
    }
  }, [aiQuery, clearAiState]);

  useEffect(() => {
    doSearch();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const today = new Date().toISOString().split('T')[0];
  const showAiResults = aiResults !== null;
  const displayResults = showAiResults ? aiResults : results;

  return (
    <main aria-label="Search listings" className="min-h-[calc(100vh-4rem)] p-6">
      {/* AI search bar */}
      <form
        data-testid="ai-search-form"
        onSubmit={(e) => { e.preventDefault(); doAiSearch(); }}
        className="ss-card mb-4 flex items-center gap-3 p-4"
      >
        <input
          data-testid="ai-search-input"
          type="text"
          value={aiQuery}
          onChange={(e) => setAiQuery(e.target.value)}
          placeholder="Describe what you're looking for..."
          maxLength={500}
          disabled={aiLoading}
          className="flex-1 rounded border px-3 py-2 text-sm"
        />
        <button
          data-testid="ai-search-button"
          type="submit"
          disabled={!aiQuery.trim() || aiLoading}
          className="rounded bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50"
        >
          {aiLoading ? 'Searching...' : 'Search with AI'}
        </button>
      </form>

      {/* AI degraded alert */}
      {!aiAvailable && aiError && (
        <div data-testid="ai-alert" role="alert" className="ss-card mb-4 border-orange-300 bg-orange-50 p-4 text-sm text-orange-800">
          AI search is temporarily unavailable. Please use the manual filters below.
        </div>
      )}

      {/* AI explanation card */}
      {showAiResults && aiExplanation && (
        <div data-testid="ai-explanation" className="ss-card mb-4 p-4">
          <p className="text-sm font-medium">{aiExplanation}</p>
          {aiFilters && (
            <div data-testid="ai-filters" className="mt-2 flex flex-wrap gap-2">
              {aiFilters.city && <span className="rounded-full bg-muted px-3 py-1 text-xs">City: {aiFilters.city}</span>}
              {aiFilters.startDate && <span className="rounded-full bg-muted px-3 py-1 text-xs">Check-in: {aiFilters.startDate}</span>}
              {aiFilters.endDate && <span className="rounded-full bg-muted px-3 py-1 text-xs">Check-out: {aiFilters.endDate}</span>}
              {aiFilters.guests && <span className="rounded-full bg-muted px-3 py-1 text-xs">Guests: {aiFilters.guests}</span>}
              {aiFilters.maxNightlyRate && <span className="rounded-full bg-muted px-3 py-1 text-xs">Max rate: ${aiFilters.maxNightlyRate}</span>}
            </div>
          )}
          <button
            data-testid="ai-clear"
            type="button"
            onClick={() => { clearAiState(); setAiQuery(''); }}
            className="mt-2 text-xs text-primary underline hover:opacity-80"
          >
            Clear AI search
          </button>
        </div>
      )}

      {/* Manual filter bar */}
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
      {loading && !showAiResults && <p className="text-muted-foreground">Loading listings...</p>}
      {error && <p className="text-red-600">{error}</p>}
      {!loading && !error && displayResults.length === 0 && !aiLoading && (
        <p data-testid="empty-state" className="text-muted-foreground">No listings found. Try adjusting your filters.</p>
      )}
      {!error && displayResults.length > 0 && (
        <div data-testid="results-grid" className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {displayResults.map((r) => (
            <ListingCard key={r.property.id} result={r} onSelect={setSelectedListing} />
          ))}
        </div>
      )}

      {/* Detail sheet */}
      <ListingDetailSheet listingId={selectedListing} onClose={() => setSelectedListing(null)} />
    </main>
  );
}
