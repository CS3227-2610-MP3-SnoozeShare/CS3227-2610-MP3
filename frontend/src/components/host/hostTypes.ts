export type ListingStatus = 'active' | 'inactive';

export type HostListing = {
  id: string; title: string; type: string; city: string; region: string; rate: number;
  bookings: number; rating: number; status: ListingStatus;
};

export type ListingDetail = HostListing & {
  description: string; address: string; postalCode: string; maxGuests: number; bedrooms: number;
  bathrooms: number; checkIn: string; checkOut: string; amenities: string[]; reviewCount: number;
  occupancy30d: number; earnings30d: number; reviews: { author: string; rating: number; text: string }[];
};

export type CalendarDay = { date: string; month: 'current' | 'other'; booked: boolean; blocked: boolean };
export type CalendarData = { monthLabel: string; days: CalendarDay[]; blockedDates: { id?: string; reason: string; startDate: string; endDate: string }[] };

export type BookingRequest = {
  id: string; guest: string; listing: string; dates: string; nights: number; gross: number;
  net: number | null; rating: number | null; status: 'pending' | 'confirmed' | 'rejected';
};
export type RequestData = { pending: BookingRequest[]; past: BookingRequest[] };

export type Message = { sender: string; body: string; timestamp?: string };
export type Conversation = {
  id: string; title: string; subtitle: string; status: 'open' | 'resolved';
  messages: Message[];
};

export type WalletSummary = { balance: number; escrowNote: string };
export type StatementRow = { date: string; type: string; relatedTo: string; amount: number; balanceAfter: number };

export type HostPage<T> = { items: T[]; page: number; size: number; hasMore: boolean };
export type HostListingDto = {
  id: string; hostId: string; status: 'ACTIVE' | 'INACTIVE'; title: string; description: string;
  propertyType: string; streetAddress: string; city: string; region: string; postalCode: number;
  maxGuests: number; bedrooms: number; bathrooms: number; baseNightlyRate: number;
  checkInTime: string; checkOutTime: string; amenities: string[]; createdAt: string;
};
export type HostListingDetailDto = { listing: HostListingDto; metrics: { bookingCount: number; averageRating: number; reviews: { guestName: string; rating: number; comment: string }[]; occupancyPercentage: number; earnings: number } };
export type CreateHostListingPayload = Omit<HostListingDto, 'id' | 'hostId' | 'createdAt'>;
export type UpdateHostListingPayload = Omit<CreateHostListingPayload, 'status'>;
export type HostCalendarDto = { year: number; month: number; days: { date: string; booked: boolean; blocked: boolean }[]; blocks: { id: string; reason: string | null; startDate: string; endDate: string }[] };
export type HostBlockDto = { id: string; propertyId: string; startDate: string; endDate: string; source: string; bookingId: string | null; reason: string | null };
export type HostBookingDto = { booking: { id: string; status: string; startDate: string; endDate: string }; guestDisplayName: string; listingTitle: string; nights: number; grossAmount: number; projectedNetAmount: number; guestAverageRating: number | null };
export type HostMessageDto = { id: string; authorId: string; authorRole: string; body: string; sentAt: string };
export type BookingConversationDto = { bookingId: string; listingTitle: string; startDate: string; endDate: string; counterpartName: string; lastMessage: HostMessageDto | null; unreadCount: number; open: boolean };
export type TicketConversationDto = { ticketId: string; channel: 'HOST'; ticketLabel: string; ticketTitle: string; listingTitle: string; counterpartName: string; lastMessage: HostMessageDto | null; unreadCount: number; open: boolean };
export type HostTransactionDto = { id: string; actionType: string; walletAdjustment: number; balanceAfter: number; reason: string; bookingId: string | null; ticketId: string | null; timestamp: string };
export type HostWalletDto = { wallet: { id: string; userId: string; balance: number; currency: string; updatedAt: string }; statement: HostPage<HostTransactionDto> };
export type HostWalletMutationDto = { mocked: true; wallet: HostWalletDto['wallet']; transaction: HostTransactionDto };

/** Converts the backend PropertyResponse into the stable presentation model. */
export function mapHostListing(dto: HostListingDto): HostListing {
  return {
    id: dto.id,
    title: dto.title,
    type: humanize(dto.propertyType),
    city: dto.city,
    region: dto.region,
    rate: Number(dto.baseNightlyRate),
    bookings: 0,
    rating: 0,
    status: dto.status === 'ACTIVE' ? 'active' : 'inactive',
  };
}

export function mapHostListingDetail(dto: HostListingDetailDto): ListingDetail {
  const listing = mapHostListing(dto.listing);
  return {
    ...listing,
    description: dto.listing.description,
    address: dto.listing.streetAddress,
    postalCode: String(dto.listing.postalCode),
    maxGuests: dto.listing.maxGuests,
    bedrooms: dto.listing.bedrooms,
    bathrooms: dto.listing.bathrooms,
    checkIn: formatTime(dto.listing.checkInTime),
    checkOut: formatTime(dto.listing.checkOutTime),
    amenities: dto.listing.amenities.map(humanize),
    reviewCount: dto.metrics.reviews.length,
    occupancy30d: dto.metrics.occupancyPercentage,
    earnings30d: Number(dto.metrics.earnings),
    bookings: dto.metrics.bookingCount,
    rating: dto.metrics.averageRating,
    reviews: dto.metrics.reviews.map((review) => ({ author: review.guestName, rating: review.rating, text: review.comment })),
  };
}

export function mapHostCalendar(dto: HostCalendarDto): CalendarData {
  const monthKey = `${dto.year}-${String(dto.month).padStart(2, '0')}`;
  return {
    monthLabel: new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric', timeZone: 'UTC' }).format(new Date(Date.UTC(dto.year, dto.month - 1, 1))),
    days: dto.days.map((day) => ({ date: day.date, month: day.date.startsWith(monthKey) ? 'current' : 'other', booked: day.booked, blocked: day.blocked })),
    blockedDates: dto.blocks.filter((block) => block.reason !== null).map((block) => ({ id: block.id, reason: block.reason ?? '', startDate: block.startDate, endDate: block.endDate })),
  };
}

export function mapHostBooking(dto: HostBookingDto): BookingRequest {
  const status = dto.booking.status === 'PENDING' ? 'pending' : dto.booking.status === 'REJECTED' ? 'rejected' : 'confirmed';
  const start = new Date(`${dto.booking.startDate}T12:00:00Z`);
  const end = new Date(`${dto.booking.endDate}T12:00:00Z`);
  const format = (value: Date) => new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', timeZone: 'UTC' }).format(value);
  return {
    id: dto.booking.id,
    guest: dto.guestDisplayName,
    listing: dto.listingTitle,
    dates: `${format(start)}–${format(end)}`,
    nights: dto.nights,
    gross: Number(dto.grossAmount),
    net: Number(dto.projectedNetAmount),
    rating: dto.guestAverageRating,
    status,
  };
}

export function mapHostWallet(dto: HostWalletDto): { wallet: WalletSummary; statement: StatementRow[] } {
  return {
    wallet: { balance: Number(dto.wallet.balance), escrowNote: 'Pending booking funds are held in escrow until the stay is complete.' },
    statement: dto.statement.items.map((transaction) => ({
      date: new Date(transaction.timestamp).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC' }),
      type: transaction.actionType,
      relatedTo: transaction.bookingId ? `Booking #${transaction.bookingId}` : transaction.ticketId ? `Ticket #${transaction.ticketId}` : '—',
      amount: Number(transaction.walletAdjustment),
      balanceAfter: Number(transaction.balanceAfter),
    })),
  };
}

function humanize(value: string) {
  return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function formatTime(value: string) {
  const match = /^(\d{2}):(\d{2})/.exec(value);
  if (!match) return value;
  return new Date(`2000-01-01T${match[1]}:${match[2]}:00`).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', timeZone: 'UTC' });
}
