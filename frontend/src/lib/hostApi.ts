import { BackendApiError, authenticatedFetch } from './api';
import { signOutUser } from './firebase';
import type {
  BookingConversationDto,
  CreateHostListingPayload,
  HostBookingDto,
  HostBlockDto,
  HostCalendarDto,
  HostListingDetailDto,
  HostListingDto,
  HostListingSummaryDto,
  HostMessageDto,
  HostWalletDto,
  HostWalletMutationDto,
  TicketConversationDto,
  UpdateHostListingPayload,
} from '@/components/host/hostTypes';

async function getJson<T>(path: string): Promise<T> {
  const response = await request(path);
  return response.json() as Promise<T>;
}

async function sendJson<T>(path: string, method: 'POST' | 'PUT', body: unknown): Promise<T> {
  const response = await request(path, { method, body: JSON.stringify(body) });
  return response.json() as Promise<T>;
}

async function sendVoid(path: string, method: 'POST' | 'DELETE'): Promise<void> {
  await request(path, { method });
}

async function request(path: string, init?: RequestInit) {
  try {
    return await (init ? authenticatedFetch(path, init) : authenticatedFetch(path));
  } catch (error) {
    if (error instanceof BackendApiError && error.status === 401) await signOutUser();
    throw error;
  }
}

export function hostSafeErrorMessage(error: unknown) {
  if (!(error instanceof BackendApiError)) return 'Unable to complete this request. Please try again.';
  if (error.code === 'DATE_RANGE_UNAVAILABLE') return 'These dates overlap an existing booking or blocked period. Choose another date range.';
  if (error.status === 401) return 'Your session has expired. Sign in again to continue.';
  if (error.status === 403) return 'You do not have permission to do that.';
  if (error.status === 404) return 'This item is no longer available.';
  if (error.status === 409) return 'This item changed. Refresh and try again.';
  if (error.status === 422) return 'This request cannot be completed.';
  if (error.status >= 500 || error.status === 0) return 'The service is temporarily unavailable. Please try again.';
  return 'Unable to complete this request. Please try again.';
}

const segment = (id: string) => encodeURIComponent(id);

export function fetchHostListings() {
  return getJson<HostListingSummaryDto[]>('/api/host/listings');
}

export function fetchHostListing(listingId: string) {
  return getJson<HostListingDetailDto>(`/api/host/listings/${segment(listingId)}`);
}

export function createHostListing(payload: CreateHostListingPayload) {
  return sendJson<HostListingDto>('/api/host/listings', 'POST', payload);
}

export function updateHostListing(listingId: string, payload: UpdateHostListingPayload) {
  return sendJson<HostListingDto>(`/api/host/listings/${segment(listingId)}`, 'PUT', payload);
}

export function updateHostListingStatus(listingId: string, status: 'ACTIVE' | 'INACTIVE') {
  return sendJson<HostListingDto>(`/api/host/listings/${segment(listingId)}/status`, 'PUT', { status });
}

export function fetchHostCalendar(listingId: string, year: number, month: number) {
  return getJson<HostCalendarDto>(
    `/api/host/listings/${segment(listingId)}/calendar?${new URLSearchParams({ year: String(year), month: String(month) })}`,
  );
}

export function createHostBlock(listingId: string, payload: { startDate: string; endDate: string; reason?: string }) {
  return sendJson<HostBlockDto>(`/api/host/listings/${segment(listingId)}/blocks`, 'POST', payload);
}

export function deleteHostBlock(blockId: string) {
  return sendVoid(`/api/host/blocks/${segment(blockId)}`, 'DELETE');
}

export function fetchHostRequests(view: 'pending' | 'history') {
  const query = new URLSearchParams({ view });
  return getJson<HostBookingDto[]>(`/api/host/requests?${query}`);
}

export function decideHostBooking(bookingId: string, approve: boolean, message?: string) {
  return sendJson<unknown>(`/api/host/requests/${segment(bookingId)}/decision`, 'POST', { approve, message });
}

export function fetchBookingConversations() {
  return getJson<BookingConversationDto[]>('/api/host/messages/bookings');
}

export function fetchBookingMessages(bookingId: string) {
  return getJson<HostMessageDto[]>(`/api/host/messages/bookings/${segment(bookingId)}`);
}

export function postBookingMessage(bookingId: string, body: string) {
  return sendJson<HostMessageDto>(`/api/host/messages/bookings/${segment(bookingId)}`, 'POST', { body });
}

export function markBookingConversationRead(bookingId: string) {
  return sendVoid(`/api/host/messages/bookings/${segment(bookingId)}/read`, 'POST');
}

export function fetchTicketConversations() {
  return getJson<TicketConversationDto[]>('/api/host/messages/tickets');
}

export function fetchTicketMessages(ticketId: string) {
  return getJson<HostMessageDto[]>(`/api/host/messages/tickets/${segment(ticketId)}?channel=HOST`);
}

export function postTicketMessage(ticketId: string, body: string) {
  return sendJson<HostMessageDto>(`/api/host/messages/tickets/${segment(ticketId)}?channel=HOST`, 'POST', { body });
}

export function markTicketConversationRead(ticketId: string) {
  return sendVoid(`/api/host/messages/tickets/${segment(ticketId)}/read?channel=HOST`, 'POST');
}

export function fetchHostWallet() {
  return getJson<HostWalletDto>('/api/host/wallet');
}

export function topUpHostWallet(amount: number) {
  return sendJson<HostWalletMutationDto>('/api/host/wallet/top-up', 'POST', { amount });
}

export function withdrawFromHostWallet(amount: number) {
  return sendJson<HostWalletMutationDto>('/api/host/wallet/withdraw', 'POST', { amount });
}
