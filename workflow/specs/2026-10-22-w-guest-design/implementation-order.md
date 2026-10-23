# Implementation Order

| ID | Task | Delivers | Satisfies |
|---|---|---|---|
| T1 | GuestSearchController + PropertyDetailResponse DTO | Search, listing detail and price estimate endpoints (`GET /api/guest/listings`, `GET /api/guest/listings/{id}`, `GET /api/guest/listings/{id}/estimate`) | F1, F2, F3 |
| T2 | GuestBookingController + GuestTripResponse DTO | Booking creation, trip listing, cancel preview and cancel endpoints (`POST /api/guest/bookings`, `GET /api/guest/bookings`, `GET /api/guest/bookings/{id}/cancel-preview`, `POST /api/guest/bookings/{id}/cancel`) | F4, F5, F6 |
| T3 | GuestWalletController | Wallet balance, transactions, top-up and withdraw endpoints (`GET /api/guest/wallet`, `GET /api/guest/wallet/transactions`, `POST /api/guest/wallet/topup`, `POST /api/guest/wallet/withdraw`) | F10 |
| T4 | GuestMessageController (tickets + messages) | Ticket filing, ticket listing, categories, conversation list, ticket/booking threads, post and mark-read endpoints | F7, F8 |
| T5 | GuestReviewController | Review submission endpoint (`POST /api/guest/bookings/{id}/review`) | F9 |
| T6 | Search page + listing detail sheet (frontend) | `/guest/search` page with filter form, results grid, listing detail Shadcn Sheet with booking flow | F1, F2, F3, F4, N3 |
| T7 | Trips page + cancel and review dialogs (frontend) | `/guest/trips` page with grouped trip cards, Cancel dialog, Review dialog | F5, F6, F9, N3 |
| T8 | Messages page + ticket filing dialog (frontend) | `/guest/messages` page with conversation sidebar, thread view, message input, ticket filing dialog | F7, F8, N3 |
| T9 | Wallet page (frontend) | `/guest/wallet` page with balance card, transaction history, top-up/withdraw forms | F10, N3 |
| T10 | Backend controller integration tests | Tests for all 19 endpoints covering happy paths, permission checks (N1) and key edge cases (E1-E39); audit row assertions (N4) | N1, N2, N4 |

## Dependencies

| Task | Depends on | Can run concurrently with | Wave |
|---|---|---|---|
| T1 GuestSearchController | — | T2, T3, T4, T5 | 1 |
| T2 GuestBookingController | — | T1, T3, T4, T5 | 1 |
| T3 GuestWalletController | — | T1, T2, T4, T5 | 1 |
| T4 GuestMessageController | — | T1, T2, T3, T5 | 1 |
| T5 GuestReviewController | — | T1, T2, T3, T4 | 1 |
| T6 Search page | T1 | T7, T8, T9 | 2 |
| T7 Trips page | T2, T5 | T6, T8, T9 | 2 |
| T8 Messages page | T4 | T6, T7, T9 | 2 |
| T9 Wallet page | T3 | T6, T7, T8 | 2 |
| T10 Backend tests | T1, T2, T3, T4, T5 | T6, T7, T8, T9 | 2 |

Shared-file hazards: T1-T5 each create separate controller classes and DTOs — no shared files. T6-T9 each create separate page directories — no shared files. T6 and T7 both create components that could share a listing-card component; if a shared component is extracted, serialize T6 before T7.
