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
export type CalendarData = { monthLabel: string; days: CalendarDay[]; blockedDates: { reason: string; range: string }[] };

export type BookingRequest = {
  id: string; guest: string; listing: string; dates: string; nights: number; gross: number;
  net: number | null; rating: number | null; status: 'pending' | 'confirmed' | 'rejected';
};
export type RequestData = { pending: BookingRequest[]; past: BookingRequest[] };

export type Message = { sender: string; body: string; timestamp?: string };
export type Conversation = {
  id: string; title: string; subtitle: string; subject: string; status: 'open' | 'resolved';
  messages: Message[];
};

export type WalletSummary = { balance: number; escrowNote: string };
export type StatementRow = { date: string; type: string; relatedTo: string; amount: number; balanceAfter: number };
