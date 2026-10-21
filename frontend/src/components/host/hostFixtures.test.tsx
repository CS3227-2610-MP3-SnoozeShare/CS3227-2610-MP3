import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { HostFixtureProvider, useHostFixtures } from './HostFixtureProvider';
import { createHostFixtureAdapter } from './hostFixtures';

function FixtureProbe() {
  const fixtures = useHostFixtures();
  const listings = fixtures.getListings();
  const wallet = fixtures.getWalletSummary();
  return (
    <output>
      {fixtures.getState()}|{listings.data?.map((listing) => listing.id).join(',')}|{wallet.data?.balance ?? wallet.state}
    </output>
  );
}

describe('Host fixture adapter', () => {
  it('returns the exact populated synthetic records and route IDs', () => {
    const adapter = createHostFixtureAdapter();
    expect(adapter.getState()).toBe('populated');
    expect(adapter.getListings().data?.map((listing) => listing.id)).toEqual([
      'lakeside-cabin', 'downtown-loft', 'garden-studio', 'hilltop-house',
    ]);
    expect(adapter.getListing('lakeside-cabin').data).toMatchObject({
      title: 'Lakeside Cabin', type: 'House', city: 'Lakeview', region: 'WI', rate: 120, bookings: 18, rating: 4.8, status: 'active',
    });
    expect(adapter.getWalletSummary().data).toMatchObject({ balance: 3920.5 });
    expect(adapter.getRequests().data?.pending.map((request) => request.guest)).toEqual(['Jane G.', 'Marcus T.', 'Priya S.']);
    expect(adapter.getMessages().data?.map((message) => message.title)).toEqual([
      'Lakeside Cabin · Oct 3–6', 'Ticket #1042 · Lakeside Cabin · Support Agent',
    ]);
    expect(adapter.getStatement().data?.[0]).toMatchObject({ type: 'BOOKING_PAYOUT', amount: 349.2 });
  });

  it('returns deterministic empty, loading, and error states', () => {
    for (const state of ['empty', 'loading', 'error'] as const) {
      const adapter = createHostFixtureAdapter(state);
      expect(adapter.getState()).toBe(state);
      expect(adapter.getListings().state).toBe(state);
      expect(adapter.getWalletSummary().state).toBe(state);
    }
    expect(createHostFixtureAdapter('empty').getListings().data).toEqual([]);
    expect(createHostFixtureAdapter('loading').getListings().data).toBeUndefined();
    expect(createHostFixtureAdapter('error').getListings().error).toBe('Unable to load your listings.');
  });

  it('returns not-found for unknown or partial listing IDs', () => {
    expect(createHostFixtureAdapter().getListing('unknown').state).toBe('not-found');
    expect(createHostFixtureAdapter().getListing('lakeside').state).toBe('not-found');
  });

  it('replaces loading with error without stale populated data', () => {
    const { rerender } = render(
      <HostFixtureProvider adapter={createHostFixtureAdapter('loading')}><FixtureProbe /></HostFixtureProvider>,
    );
    expect(screen.getByRole('status')).toHaveTextContent('loading||loading');
    rerender(<HostFixtureProvider adapter={createHostFixtureAdapter('error')}><FixtureProbe /></HostFixtureProvider>);
    expect(screen.getByRole('status')).toHaveTextContent('error||error');
    expect(screen.getByRole('status')).not.toHaveTextContent('Lakeside');
  });
});
