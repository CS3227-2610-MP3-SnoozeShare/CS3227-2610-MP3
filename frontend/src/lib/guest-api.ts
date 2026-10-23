import { authenticatedFetch } from './api';

/* ─── Types ─── */

export type PropertyResponse = {
  id: string;
  hostId: string;
  status: string;
  title: string;
  description: string;
  propertyType: string;
  address: string;
  city: string;
  district: string;
  postalCode: number;
  maxGuests: number;
  bedrooms: number;
  bathrooms: number;
  baseNightlyRate: number;
  checkInTime: string;
  checkOutTime: string;
  amenities: string[];
  createdAt: string;
};

export type SearchResult = {
  property: PropertyResponse;
};

export type PropertyDetail = {
  property: PropertyResponse;
  hostDisplayName: string;
  hostingSinceYear: number;
  reviews: ListingReview[];
  averageRating: number;
  reviewCount: number;
};

export type ListingReview = {
  guestDisplayName: string;
  rating: number;
  comment: string;
  createdAt: string;
};

export type PriceBreakdown = {
  nights: number;
  nightlyRate: number;
  totalAmount: number;
};

export type BookingResponse = {
  id: string;
  listingId: string;
  guestId: string;
  startDate: string;
  endDate: string;
  status: string;
  nightlyRateSnapshot: number;
  totalAmount: number;
  createdAt: string;
  decidedAt: string | null;
  completedAt: string | null;
  hostDecisionMessage: string | null;
};

export type GuestTrip = {
  booking: BookingResponse;
  listingTitle: string;
  hostDisplayName: string;
  canCancel: boolean;
  canFileDispute: boolean;
  hasReview: boolean;
};

export type CancellationRefund = {
  amount: number;
  percent: number;
};

export type WalletResponse = {
  id: string;
  userId: string;
  balance: number;
  currency: string;
  updatedAt: string;
};

export type TransactionResponse = {
  id: string;
  actionType: string;
  subjectUserId: string;
  walletAdjustment: number;
  balanceAfter: number;
  reason: string | null;
  bookingId: string | null;
  ticketId: string | null;
  timestamp: string;
};

export type TicketResponse = {
  id: string;
  bookingId: string;
  raisedByUserId: string;
  raisedByRole: string;
  category: string;
  title: string;
  description: string;
  requestedRemedy: string;
  supportingText: string | null;
  status: string;
  assignedAgentId: string | null;
  agentNotes: string | null;
  resolutionReason: string | null;
  createdAt: string;
  resolvedAt: string | null;
};

export type MessageResponse = {
  id: string;
  authorId: string;
  authorRole: string;
  body: string;
  sentAt: string;
};

export type ConversationSummary = {
  ticketId: string;
  channel: string;
  ticketLabel: string;
  ticketTitle: string;
  listingTitle: string;
  counterpartName: string;
  lastMessage: MessageResponse | null;
  unreadCount: number;
  open: boolean;
};

export type BookingConversationSummary = {
  bookingId: string;
  listingTitle: string;
  startDate: string;
  endDate: string;
  counterpartName: string;
  lastMessage: MessageResponse | null;
  unreadCount: number;
  open: boolean;
};

export type ConversationList = {
  ticketConversations: ConversationSummary[];
  bookingConversations: BookingConversationSummary[];
};

export type ReviewResponse = {
  id: string;
  bookingId: string;
  guestId: string;
  rating: number;
  comment: string;
  createdAt: string;
};

export type AiSearchFiltersResponse = {
  city: string | null;
  startDate: string | null;
  endDate: string | null;
  guests: number | null;
  maxNightlyRate: number | null;
};

export type AiSearchResponse = {
  results: SearchResult[];
  explanation: string;
  filters: AiSearchFiltersResponse | null;
  aiAvailable: boolean;
  errorCode?: string;
};

/* ─── Search ─── */

export async function searchListings(params?: {
  city?: string;
  startDate?: string;
  endDate?: string;
  guests?: number;
  maxNightlyRate?: number;
}): Promise<SearchResult[]> {
  const query = new URLSearchParams();
  if (params?.city) query.set('city', params.city);
  if (params?.startDate) query.set('startDate', params.startDate);
  if (params?.endDate) query.set('endDate', params.endDate);
  if (params?.guests) query.set('guests', String(params.guests));
  if (params?.maxNightlyRate) query.set('maxNightlyRate', String(params.maxNightlyRate));
  const qs = query.toString();
  const res = await authenticatedFetch(`/api/guest/listings${qs ? `?${qs}` : ''}`);
  return res.json();
}

export async function aiSearch(query: string): Promise<AiSearchResponse> {
  const res = await authenticatedFetch('/api/guest/ai/search', {
    method: 'POST',
    body: JSON.stringify({ query }),
  });
  return res.json();
}

export async function getListingDetail(id: string): Promise<PropertyDetail> {
  const res = await authenticatedFetch(`/api/guest/listings/${id}`);
  return res.json();
}

export async function estimateCost(id: string, startDate: string, endDate: string): Promise<PriceBreakdown> {
  const res = await authenticatedFetch(`/api/guest/listings/${id}/estimate?startDate=${startDate}&endDate=${endDate}`);
  return res.json();
}

/* ─── Bookings ─── */

export async function createBooking(propertyId: string, startDate: string, endDate: string): Promise<BookingResponse> {
  const res = await authenticatedFetch('/api/guest/bookings', {
    method: 'POST',
    body: JSON.stringify({ propertyId, startDate, endDate }),
  });
  return res.json();
}

export async function listTrips(): Promise<GuestTrip[]> {
  const res = await authenticatedFetch('/api/guest/bookings');
  return res.json();
}

export async function cancelPreview(bookingId: string): Promise<CancellationRefund> {
  const res = await authenticatedFetch(`/api/guest/bookings/${bookingId}/cancel-preview`);
  return res.json();
}

export async function cancelBooking(bookingId: string): Promise<BookingResponse> {
  const res = await authenticatedFetch(`/api/guest/bookings/${bookingId}/cancel`, { method: 'POST' });
  return res.json();
}

/* ─── Wallet ─── */

export async function getWallet(): Promise<WalletResponse> {
  const res = await authenticatedFetch('/api/guest/wallet');
  return res.json();
}

export async function getTransactions(): Promise<TransactionResponse[]> {
  const res = await authenticatedFetch('/api/guest/wallet/transactions');
  return res.json();
}

export async function topUp(amount: number): Promise<WalletResponse> {
  const res = await authenticatedFetch('/api/guest/wallet/topup', {
    method: 'POST',
    body: JSON.stringify({ amount: amount.toFixed(2) }),
  });
  return res.json();
}

export async function withdraw(amount: number): Promise<WalletResponse> {
  const res = await authenticatedFetch('/api/guest/wallet/withdraw', {
    method: 'POST',
    body: JSON.stringify({ amount: amount.toFixed(2) }),
  });
  return res.json();
}

/* ─── Tickets & Messages ─── */

export async function listCategories(): Promise<string[]> {
  const res = await authenticatedFetch('/api/guest/tickets/categories');
  return res.json();
}

export async function fileTicket(req: {
  bookingId: string;
  category: string;
  title: string;
  description: string;
  requestedRemedy: string;
  supportingText?: string;
}): Promise<TicketResponse> {
  const res = await authenticatedFetch('/api/guest/tickets', {
    method: 'POST',
    body: JSON.stringify(req),
  });
  return res.json();
}

export async function myTickets(): Promise<TicketResponse[]> {
  const res = await authenticatedFetch('/api/guest/tickets');
  return res.json();
}

export async function getConversations(): Promise<ConversationList> {
  const res = await authenticatedFetch('/api/guest/messages/conversations');
  return res.json();
}

export async function getTicketThread(ticketId: string): Promise<MessageResponse[]> {
  const res = await authenticatedFetch(`/api/guest/messages/tickets/${ticketId}`);
  return res.json();
}

export async function postToTicket(ticketId: string, body: string): Promise<MessageResponse> {
  const res = await authenticatedFetch(`/api/guest/messages/tickets/${ticketId}`, {
    method: 'POST',
    body: JSON.stringify({ body }),
  });
  return res.json();
}

export async function getBookingThread(bookingId: string): Promise<MessageResponse[]> {
  const res = await authenticatedFetch(`/api/guest/messages/bookings/${bookingId}`);
  return res.json();
}

export async function postToBooking(bookingId: string, body: string): Promise<MessageResponse> {
  const res = await authenticatedFetch(`/api/guest/messages/bookings/${bookingId}`, {
    method: 'POST',
    body: JSON.stringify({ body }),
  });
  return res.json();
}

export async function markTicketRead(ticketId: string): Promise<void> {
  await authenticatedFetch(`/api/guest/messages/tickets/${ticketId}/read`, { method: 'PUT' });
}

export async function markBookingRead(bookingId: string): Promise<void> {
  await authenticatedFetch(`/api/guest/messages/bookings/${bookingId}/read`, { method: 'PUT' });
}

/* ─── Reviews ─── */

export async function submitReview(bookingId: string, rating: number, comment: string): Promise<ReviewResponse> {
  const res = await authenticatedFetch(`/api/guest/bookings/${bookingId}/review`, {
    method: 'POST',
    body: JSON.stringify({ rating, comment }),
  });
  return res.json();
}
