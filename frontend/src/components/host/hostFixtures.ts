import type { FixtureResult, HostFixtureState } from './hostFixtureState';
import { resultFor } from './hostFixtureState';
import type { CalendarData, Conversation, HostListing, ListingDetail, StatementRow, RequestData, WalletSummary } from './hostTypes';

const listings: HostListing[] = [
  { id: 'lakeside-cabin', title: 'Lakeside Cabin', type: 'House', city: 'Lakeview', region: 'WI', rate: 120, bookings: 18, rating: 4.8, status: 'active' },
  { id: 'downtown-loft', title: 'Downtown Loft', type: 'Apartment', city: 'Lakeview', region: 'WI', rate: 95, bookings: 11, rating: 4.6, status: 'active' },
  { id: 'garden-studio', title: 'Garden Studio', type: 'Private room', city: 'Lakeview', region: 'WI', rate: 58, bookings: 4, rating: 4.9, status: 'inactive' },
  { id: 'hilltop-house', title: 'Hilltop House', type: 'House', city: 'Millbrook', region: 'WI', rate: 210, bookings: 7, rating: 4.7, status: 'active' },
];

const lakeside: ListingDetail = {
  ...listings[0], address: '142 Shoreline Dr', postalCode: '53211', maxGuests: 4, bedrooms: 2, bathrooms: 1,
  checkIn: '3:00 PM', checkOut: '11:00 AM', description: 'A cozy cabin steps from the water, with a private dock and wood-burning stove. Perfect for a quiet long weekend away from the city.',
  amenities: ['Wifi', 'Parking', 'Kitchen', 'Washer'], reviewCount: 23, occupancy30d: 62, earnings30d: 1860,
  reviews: [
    { author: 'Marcus T.', rating: 5, text: 'Exactly as pictured, host was very responsive.' },
    { author: 'Priya S.', rating: 5, text: 'Loved the dock. Would book again.' },
  ],
};

const requests: RequestData = {
  pending: [
    { id: 'request-jane', guest: 'Jane G.', listing: 'Lakeside Cabin', dates: 'Oct 3–6', nights: 3, gross: 360, net: 349.2, rating: 4.9, status: 'pending' },
    { id: 'request-marcus', guest: 'Marcus T.', listing: 'Downtown Loft', dates: 'Oct 10–12', nights: 2, gross: 190, net: 184.3, rating: null, status: 'pending' },
    { id: 'request-priya', guest: 'Priya S.', listing: 'Lakeside Cabin', dates: 'Oct 18–22', nights: 4, gross: 480, net: 465.6, rating: 4.7, status: 'pending' },
  ],
  past: [
    { id: 'request-wei', guest: 'Wei L.', listing: 'Hilltop House', dates: 'Sep 1–4', nights: 3, gross: 630, net: null, rating: null, status: 'confirmed' },
    { id: 'request-ana', guest: 'Ana R.', listing: 'Lakeside Cabin', dates: 'Aug 20–22', nights: 2, gross: 240, net: null, rating: null, status: 'rejected' },
    { id: 'request-dana', guest: 'Dana K.', listing: 'Downtown Loft', dates: 'Aug 12–15', nights: 3, gross: 285, net: null, rating: null, status: 'confirmed' },
  ],
};

const directMessageDetails: Record<string, Pick<Conversation, 'messages'>> = {
  'request-jane': {
    messages: [
      { sender: 'Jane G.', body: 'Hi, I submitted a request for Oct 3–6. What time does check-in start?' },
      { sender: 'You', body: 'Check-in starts at 3:00 PM.' },
      { sender: 'Jane G.', body: 'Thanks, that works for me.' },
    ],
  },
  'request-marcus': {
    messages: [
      { sender: 'Marcus T.', body: 'Hi, I sent a request for Oct 10–12. Could you let me know when you have reviewed it?' },
      { sender: 'You', body: 'I’ll review your request shortly.' },
    ],
  },
  'request-priya': {
    messages: [
      { sender: 'Priya S.', body: 'Hello! I just sent a request for Oct 18–22.' },
      { sender: 'You', body: 'Thanks for reaching out. I’ll get back to you soon.' },
    ],
  },
};

const messages: Conversation[] = [
  ...requests.pending.map((request) => ({
    id: `direct-${request.id.replace('request-', '')}`,
    title: `${request.listing} · ${request.dates}`,
    subtitle: ` Booking #B-2291 · Direct message with ${request.guest} (Guest)`,
    status: 'open' as const,
    messages: directMessageDetails[request.id]?.messages ?? [],
  })),
  { id: 'ticket-1042', title: 'Refund not received', subtitle: 'Ticket #1042 · Lakeside Cabin · Support Agent', status: 'resolved', messages: [] },
];

const wallet: WalletSummary = { balance: 3920.5, escrowNote: 'Pending booking funds are held in escrow until the stay is complete.' };
const statement: StatementRow[] = [
  { date: 'Sep 24, 2026', type: 'BOOKING_PAYOUT', relatedTo: 'Booking #B-2291', amount: 349.2, balanceAfter: 3920.5 },
  { date: 'Sep 20, 2026', type: 'TOP_UP', relatedTo: '—', amount: 500, balanceAfter: 3571.3 },
  { date: 'Sep 7, 2026', type: 'BOOKING_PAYOUT', relatedTo: 'Booking #B-2255', amount: 203.7, balanceAfter: 3071.3 },
  { date: 'Aug 30, 2026', type: 'ESCROW_HOLD', relatedTo: 'Booking #B-2255', amount: -210, balanceAfter: 2867.6 },
  { date: 'Aug 20, 2026', type: 'WITHDRAWAL', relatedTo: '—', amount: -300, balanceAfter: 3077.6 },
];

const calendar: CalendarData = {
  monthLabel: 'September 2026',
  days: Array.from({ length: 35 }, (_, index) => {
    const date = new Date(2026, 7, 30 + index);
    const inCurrentMonth = date.getMonth() === 8;
    const day = date.getDate();
    return {
      date: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`,
      month: inCurrentMonth ? 'current' : 'other',
      booked: inCurrentMonth && (day === 4 || day === 5),
      blocked: inCurrentMonth && day >= 15 && day <= 17,
    };
  }),
  blockedDates: [{ reason: 'Maintenance', startDate: '2026-10-03', endDate: '2026-10-05' }],
};

export type HostFixtureAdapter = {
  getState: () => HostFixtureState;
  getWalletSummary: () => FixtureResult<WalletSummary>;
  getListings: () => FixtureResult<HostListing[]>;
  getListing: (id: string) => FixtureResult<ListingDetail>;
  getCalendar: (id: string) => FixtureResult<CalendarData>;
  getRequests: () => FixtureResult<RequestData>;
  getMessages: () => FixtureResult<Conversation[]>;
  getStatement: () => FixtureResult<StatementRow[]>;
};

export function createHostFixtureAdapter(state: HostFixtureState = 'populated'): HostFixtureAdapter {
  const result = <T,>(value: T, empty: T, error: string) => resultFor(state, value, empty, error);
  return {
    getState: () => state,
    getWalletSummary: () => result(wallet, { balance: 0, escrowNote: '' }, 'Unable to load wallet.'),
    getListings: () => result(listings, [], 'Unable to load your listings.'),
    getListing: (id) => {
      const detail = id === 'lakeside-cabin' ? lakeside : listings.find((listing) => listing.id === id);
      if (!detail) return { state: 'not-found', error: 'Listing not found' };
      if (state === 'empty') return { state: 'empty', data: detail as ListingDetail };
      if (state === 'loading') return { state, error: 'Loading' };
      if (state === 'error') return { state, error: 'Unable to load this listing.' };
      return { state, data: detail as ListingDetail };
    },
    getCalendar: () => result(calendar, { monthLabel: 'September 2026', days: [], blockedDates: [] }, 'Unable to load the booking calendar.'),
    getRequests: () => result(requests, { pending: [], past: [] }, 'Unable to load booking requests.'),
    getMessages: () => result(messages, [], 'Unable to load messages.'),
    getStatement: () => result(statement, [], 'Unable to load wallet statement.'),
  };
}
