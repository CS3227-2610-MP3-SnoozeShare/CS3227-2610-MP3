# API — Messages and Tickets

See [api-search-booking.md](api-search-booking.md) for common error responses, auth rules, and the resolved-user-ID mechanism (SD7).

## GuestMessageController

Handles ticket filing, ticket message threads and booking conversation threads.

### GET /api/guest/tickets/categories

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role |
| Tenancy scope | Public lookup data; returns only active categories |
| Inputs | None |
| Outputs | `200 OK` — `TicketCategoryResponse[]` sorted alphabetically, with "Other" last |
| Errors | None specific |
| Idempotency | Safe, read-only |
| Transaction boundary | Read-only |
| Events / audit | None |
| Logging | None |
| Compatibility | New endpoint |

### POST /api/guest/tickets

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role; the booking must belong to the authenticated guest |
| Tenancy scope | Creates a ticket owned by the authenticated guest, linked to their booking |
| Inputs | Body — `FileTicketRequest`: `bookingId` UUID (required), `category` String (required, not blank), `title` String (required, not blank, max 200 chars), `description` String (required, not blank, max 5000 chars), `requestedRemedy` RemedyType enum (required), `supportingText` String (optional, blank → null, max 5000 chars) |
| Outputs | `201 Created` — `TicketResponse` |
| Errors | Booking not found → 404 `BOOKING_NOT_FOUND`; booking belongs to another guest → 403 `FORBIDDEN`; outside dispute window → 400 `DISPUTE_WINDOW_CLOSED` "The dispute window for this booking has closed"; booking already has a ticket → 409 `DUPLICATE_TICKET` "A ticket already exists for this booking" |
| Idempotency | Not idempotent; duplicate filing blocked by the service |
| Transaction boundary | `TicketService.fileTicket` in a transaction |
| Events / audit | Audit row `TICKET_CREATED`; `TicketOpenedEvent` after commit |
| Logging | Log booking ID and ticket ID at INFO |
| Compatibility | New endpoint |

### GET /api/guest/tickets

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role |
| Tenancy scope | Returns only tickets filed by the authenticated guest |
| Inputs | None |
| Outputs | `200 OK` — `TicketResponse[]` |
| Errors | None specific |
| Idempotency | Safe, read-only |
| Transaction boundary | Read-only |
| Events / audit | None |
| Logging | Log user ID at DEBUG |
| Compatibility | New endpoint |

### GET /api/guest/messages/conversations

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role |
| Tenancy scope | Returns only conversations the authenticated guest is a participant in |
| Inputs | None |
| Outputs | `200 OK` — `GuestConversationListResponse` (new): `ticketConversations` (`ConversationSummaryResponse[]`), `bookingConversations` (`BookingConversationSummaryResponse[]`). Each includes `lastMessage`, `unreadCount`, `open` flag |
| Errors | None specific |
| Idempotency | Safe, read-only |
| Transaction boundary | Read-only |
| Events / audit | None |
| Logging | Log user ID at DEBUG |
| Compatibility | New endpoint |

### GET /api/guest/messages/tickets/{ticketId}

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role; ticket must be filed by the authenticated guest |
| Tenancy scope | Returns only the GUEST channel of tickets filed by this guest |
| Inputs | Path — `ticketId`: UUID |
| Outputs | `200 OK` — `MessageResponse[]` ordered by `sentAt` ascending |
| Errors | Ticket not found → 404 `TICKET_NOT_FOUND`; ticket belongs to another user → 403 `FORBIDDEN` |
| Idempotency | Safe, read-only |
| Transaction boundary | Read-only |
| Events / audit | None |
| Logging | Log ticket ID at DEBUG |
| Compatibility | New endpoint |

### POST /api/guest/messages/tickets/{ticketId}

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role; ticket must be filed by the authenticated guest |
| Tenancy scope | Posts to the GUEST channel of the authenticated guest's ticket only |
| Inputs | Path — `ticketId`: UUID. Body — `PostMessageRequest`: `body` String (required, not blank, max 5000 chars) |
| Outputs | `201 Created` — `MessageResponse` |
| Errors | Ticket not found → 404 `TICKET_NOT_FOUND`; ticket belongs to another user → 403 `FORBIDDEN`; ticket resolved → 400 `THREAD_CLOSED` "Cannot post to a resolved ticket" |
| Idempotency | Not idempotent |
| Transaction boundary | `MessageService.post` in a transaction |
| Events / audit | `MessagePostedEvent` after commit |
| Logging | Log ticket ID at INFO; never log message body |
| Compatibility | New endpoint |

### GET /api/guest/messages/bookings/{bookingId}

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role; booking must belong to the authenticated guest |
| Tenancy scope | Returns only the authenticated guest's booking conversation |
| Inputs | Path — `bookingId`: UUID |
| Outputs | `200 OK` — `MessageResponse[]` ordered by `sentAt` ascending |
| Errors | Booking not found → 404 `BOOKING_NOT_FOUND`; booking belongs to another guest → 403 `FORBIDDEN` |
| Idempotency | Safe, read-only |
| Transaction boundary | Read-only |
| Events / audit | None |
| Logging | Log booking ID at DEBUG |
| Compatibility | New endpoint |

### POST /api/guest/messages/bookings/{bookingId}

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role; booking must belong to the authenticated guest |
| Tenancy scope | Posts to the authenticated guest's booking conversation only |
| Inputs | Path — `bookingId`: UUID. Body — `PostMessageRequest`: `body` String (required, not blank, max 5000 chars) |
| Outputs | `201 Created` — `MessageResponse` |
| Errors | Booking not found → 404 `BOOKING_NOT_FOUND`; booking belongs to another guest → 403 `FORBIDDEN`; conversation closed (booking ended + 7 days passed) → 400 `THREAD_CLOSED` "Booking conversation is closed" |
| Idempotency | Not idempotent |
| Transaction boundary | `BookingConversationService.post` in a transaction |
| Events / audit | `BookingMessagePostedEvent` after commit |
| Logging | Log booking ID at INFO; never log message body |
| Compatibility | New endpoint |

### PUT /api/guest/messages/tickets/{ticketId}/read

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role; ticket must be filed by the authenticated guest |
| Tenancy scope | Marks read only for the authenticated guest's ticket thread |
| Inputs | Path — `ticketId`: UUID |
| Outputs | `204 No Content` |
| Errors | Ticket not found → 404; ticket belongs to another user → 403 |
| Idempotency | Idempotent; calling again has no effect |
| Transaction boundary | `MessageService.markRead` in a transaction |
| Events / audit | None |
| Logging | Log ticket ID at DEBUG |
| Compatibility | New endpoint |

### PUT /api/guest/messages/bookings/{bookingId}/read

| Field | Definition |
|---|---|
| Caller and principal | Authenticated guest |
| Authorization rule | GUEST role; booking must belong to the authenticated guest |
| Tenancy scope | Marks read only for the authenticated guest's booking conversation |
| Inputs | Path — `bookingId`: UUID |
| Outputs | `204 No Content` |
| Errors | Booking not found → 404; booking belongs to another guest → 403 |
| Idempotency | Idempotent |
| Transaction boundary | `BookingConversationService.markRead` in a transaction |
| Events / audit | None |
| Logging | Log booking ID at DEBUG |
| Compatibility | New endpoint |
