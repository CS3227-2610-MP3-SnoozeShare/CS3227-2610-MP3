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
    expect(adapter.getListings().data).toHaveLength(12);
    expect(adapter.getListings().data?.map((listing) => listing.id)).toEqual(expect.arrayContaining([
      'lakeside-cabin', 'downtown-loft', 'garden-studio', 'hilltop-house', 'sunroom-studio',
    ]));
    expect(adapter.getListing('lakeside-cabin').data).toMatchObject({
      title: 'Lakeside Cabin', type: 'House', city: 'Lakeview', region: 'WI', rate: 120, bookings: 18, rating: 4.8, status: 'active',
    });
    expect(adapter.getWalletSummary().data).toMatchObject({ balance: 3920.5 });
    expect(adapter.getRequests().data?.pending.map((request) => request.guest).slice(0, 3)).toEqual(['Jane G.', 'Marcus T.', 'Priya S.']);
    expect(adapter.getRequests().data?.pending).toHaveLength(8);
    expect(adapter.getRequests().data?.past).toHaveLength(10);
    expect(adapter.getMessages().data?.map((message) => message.title).slice(0, 3)).toEqual([
      'Lakeside Cabin · Oct 3–6', 'Downtown Loft · Oct 10–12', 'Lakeside Cabin · Oct 18–22',
    ]);
    expect(adapter.getMessages().data).toHaveLength(9);
    expect(adapter.getStatement().data?.[0]).toMatchObject({ type: 'BOOKING_PAYOUT', amount: 349.2 });
    expect(adapter.getStatement().data).toHaveLength(31);
    expect(adapter.getStatement().data).toEqual(expect.arrayContaining([
      expect.objectContaining({ type: 'ESCROW_HOLD', relatedTo: 'Booking #B-2255', amount: -210 }),
      expect.objectContaining({ type: 'TICKET_REMEDY', relatedTo: 'Ticket #1031 (approved)', amount: 60 }),
      expect.objectContaining({ type: 'WITHDRAWAL', amount: -300 }),
    ]));
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

  it('keeps the sample statement balances consistent across transactions', () => {
    const statement = createHostFixtureAdapter().getStatement().data!;
    for (let index = 0; index < statement.length - 1; index += 1) {
      expect(statement[index + 1].balanceAfter + statement[index].amount).toBeCloseTo(statement[index].balanceAfter, 2);
    }
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
