---
layout: default
title: User Guide
permalink: /user-guide/
---

# SnoozeShare User Guide

SnoozeShare is a browser-based short-term rental application. Guests search and book
properties, Hosts manage listings and requests, and Support Agents handle disputes and account
governance. Wallet balances, escrow, and payouts are simulated application transactions; no
real payment provider is connected.

## Table of contents

1. [Quick start](#quick-start)
2. [Account access](#account-access)
3. [Navigation and workspaces](#navigation-and-workspaces)
4. [Guest workspace](#guest-workspace)
5. [Host workspace](#host-workspace)
6. [Support Agent workspace](#support-agent-workspace)
7. [Shared rules](#shared-rules)
8. [FAQ](#faq)
9. [Known limitations](#known-limitations)
10. [Troubleshooting](#troubleshooting)
11. [Glossary](#glossary)

## Quick start

1. Open the current frontend deployment: [SnoozeShare](https://cs3227-snoozeshare.vercel.app).
2. Choose **Create account** or sign in with an existing account.
3. Select the workspace for your role from the header.
4. If the app reports that the backend is unreachable, wait briefly and choose **Re-check**.

> **Startup warning:** Because of hosting-platform limitations, the backend may take **one to
> two minutes** to boot on the first launch or after a period of inactivity. During this time,
> the frontend may show “backend unreachable”; wait for the service to wake, then choose
> **Re-check** and retry.

The deployed frontend is connected to the hosted API. Registration codes for privileged roles
are intentionally not published here; request the current code from the project administrator.

For local development, follow the frontend instructions in the repository [README](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/README.md)
and the [development environment runbook](../runbooks/dev-environment.html). The frontend normally
runs at `http://localhost:3000` and expects the API at `http://localhost:8080` unless
`NEXT_PUBLIC_API_BASE_URL` is configured. Local development requires the backend and its
PostgreSQL environment; this is not a standalone desktop mode.

![Quick-start image](../images/quickstart-db-folder.png)


## Account access

### Sign in and reset a password

Enter your email and password on **Sign in**. SnoozeShare uses Firebase email/password
authentication; the old email-only desktop login flow no longer applies.

Choose **Forgot password?** to request a reset email. A successful request tells you to check
your email. Check spam and verify the address if the message does not arrive.

### Sample accounts

Use these seeded accounts to try each workspace on the hosted demo:

| Role | Email | Password |
| --- | --- | --- |
| Guest | `guest01@snoozeshare.test` | `guest123` |
| Host | `host01@snoozeshare.test` | `host123` |
| Support Agent | `agent01@snoozeshare.test` | `agent123` |

![Sign-in screen](../images/login.png)


### Create an account

The registration form asks for an email, a password of at least six characters, a display name,
and a role: **Guest**, **Host**, or **Support Agent**. Guest registration does not require a
code. Host and Support Agent registration requires a current administrator-provided code.

Use **Sign out** from the account menu when finished on a shared device.

![Registration screen](../images/register.png)


## Navigation and workspaces

| Role | Header destinations |
| --- | --- |
| Guest | Search, Trips, Messages, Wallet |
| Host | Listings, Requests, Messages, Wallet |
| Support Agent | Disputes, Accounts, Audit Log, Categories |

Wallet is role-specific: Guests and Hosts see their own wallet through the same header
interaction pattern. A displayed balance is an application balance, not a bank balance.

## Guest workspace

### Search and book

**Search** supports manual filters for city, check-in, check-out, guests, and maximum nightly
rate. Select a result to open its detail sheet, review availability and pricing, and choose
**Book**. A booking places the stay cost on escrow and starts as pending until the Host responds.

You can also enter a natural-language request in **Search with AI**. Describe the stay you want,
such as a city, dates, guest count, or budget. The assistant interprets the request into search
filters, displays those filters and an explanation, and returns matching listings. It only
assists with discovery: it does not book a stay, change your wallet, or modify your account.

Review the interpreted filters before choosing a listing, especially when the request is vague
or contains conflicting details. The request is limited in length and the assistant can be
temporarily unavailable or return a degraded result. If that happens, use the manual City,
Check-in, Check-out, Guests, and Max rate filters instead.

![Guest search](../images/guest-search.png)
![Guest listing detail](../images/guest-listing-detail.png)


### Trips

**Trips** groups bookings into **Upcoming**, **Active**, and **Past**. Depending on the booking
state, a trip card can offer cancellation with the applicable refund calculation, a dispute
ticket after a stay has started and within the dispute window, or a review for an eligible
completed stay.

![Guest trips](../images/guest-trips.png)
![Guest cancellation dialog](../images/guest-cancel-dialog.png)
![Guest review dialog](../images/guest-review-dialog.png)


### Messages

**Messages** contains booking conversations and support ticket conversations. Select a thread to
read it and reply. Choose **File Ticket** to open a dispute form and select the booking when
prompted. Resolved ticket conversations are read-only.

![Guest dispute form](../images/guest4.png)
![Guest messages](../images/guest-messages.png)


### Wallet

**Wallet** shows Available Balance, currency, and Transaction History. **Top Up** adds
simulated funds and **Withdraw** removes available simulated funds. The statement can include
holds, releases, charges, refunds, payouts, top-ups, and withdrawals. Funds held in escrow
cannot be withdrawn. Hosted wallet amounts currently use SGD.

![Guest wallet](../images/wallet-dashboard.png)
![Wallet top-up dialog](../images/wallet-topup-dialog.png)


## Host workspace

### Listings and calendars

**Listings** shows your properties. **New listing** and **Edit listing** cover the title,
description, property type and status, address, capacity, bedrooms, bathrooms, nightly rate,
check-in/check-out times, and amenities such as Wi-Fi, parking, air conditioning, kitchen,
washer, and work desk. Save the listing after required fields are valid. A listing's
**Booking Calendar** lets you review availability and block dates.

![Host listings](../images/host1_1.png)
![Host listing detail](../images/host-listing-detail.png)
![Host listing form](../images/host1_2.png)
![Host calendar](../images/host2.png)


### AI Description Assistant

The **AI Description Assistant** helps a Host evaluate and improve listing copy. It becomes
available from the Description field after title, property type, maximum guests, bedrooms,
bathrooms, check-in time, and check-out time are entered. It uses the listing details supplied
in the form as context and is intended to avoid inventing property facts.

**Evaluate** reviews the current description; **Generate description** creates a description and
analysis; **Apply generated description** copies the generated text into the form. Review the
result before applying it. Applying changes only the form field, while saving the listing is a
separate action; the assistant cannot publish a listing by itself.

The assistant may return a degraded response, reject invalid output, or be temporarily
unavailable. If that happens, edit the description manually and save the listing without AI.

### Requests, messages, and wallet

**Requests** shows pending requests and history. Review guest, dates, nights, gross amount, and
expected net earning, then choose **Approve** or **Decline**; an optional message can accompany
the response.

![Host requests](../images/host3.png)
![Host approval dialog](../images/host-approve-dialog.png)


**Messages** combines booking and support-ticket threads. Select a thread to load it, mark it
read, and reply. Use the ticket thread for support cases and the booking thread for stay
coordination.

![Host messages](../images/host-messages.png)


**Wallet** shows the Host balance and statement. Top-ups, withdrawals, escrow entries, and
payouts are simulated. Host payouts are net of the platform fee described below.

## Support Agent workspace

Agent access is intended for authorized staff.

![Agent shell](../images/agent-shell-overview.png)


- **Disputes:** filter the queue, open a ticket, review booking and escrow details, read Guest
  and Host messages, add internal notes, assign or unassign the ticket, and resolve it. Actions
  include rejecting, accepting, or applying a manual remedy such as a full Host payout, full
  Guest refund, or custom adjustment. A reason is required.

  ![Dispute queue](../images/admin1_1.png)
  ![Dispute detail](../images/admin1_2.png)
  ![Resolution dialog](../images/admin2_1.png)

- **Accounts:** search Guest and Host accounts, then suspend or reactivate an account with a
  required reason.

  ![Accounts table](../images/admin4.png)
  ![Suspend dialog](../images/agent-suspend-dialog.png)

- **Audit Log:** search by user, booking, ticket, or user ID; filter by action and date range;
  clear filters; and load more results.

  ![Audit log](../images/admin5.png)

- **Categories:** add, rename, activate, deactivate, or delete ticket categories. A category
  used by tickets cannot be deleted; deactivate it instead.

  ![Ticket categories](../images/admin3.png)


### Ticket clarification assistant

When a Guest files a ticket, the **AI assistant** gathers facts by asking follow-up questions in
the Guest ticket thread and prepares an unverified draft report for the Support Agent. It uses
the ticket and Guest conversation as context; it does not use the Host conversation to generate
the Guest questions. Assistant messages appear in the Guest thread as **Snoozy · Automated
assistant**. The assistant gathers facts only: it does not decide who is right, approve a
remedy, issue a refund, change a ticket, or modify a wallet.

Open a ticket's detail view to see the assistant panel between the Guest and Host conversations.
The panel can show whether the assistant is working, waiting for the Guest's answer, finished,
stopped, or unavailable. It can also show an injection warning when suspicious input was flagged.
The draft is labelled unverified and is presented as headed sections for review.

If the draft is useful, choose **Insert into notes**. This copies the draft into the Agent's
editable internal notes; it does not resolve the ticket or apply a remedy. Review and edit the
notes before saving, then use the normal dispute resolution controls. Treat the draft as a
summary aid, not as evidence or a decision.

The assistant may stop after its question-round limit, become unavailable, or remain stale if a
background job is interrupted. Treat its draft as a summary aid, not as evidence or a decision.
In those cases, continue the ticket manually using the Guest and Host conversations and available
booking evidence. Agent messages stop the assistant, and resolved tickets are read-only.

## Shared rules

| Rule | Current behavior |
| --- | --- |
| Currency | Hosted wallet amounts use SGD. |
| Escrow | A booking hold reserves the Guest's stay cost until completion or resolution. |
| Platform fee | The platform keeps 3% of the amount paid out to a Host. |
| Dispute window | A Guest can file a dispute up to 7 days after check-out. |
| Completion | A confirmed booking completes after the dispute window unless an open dispute prevents completion. |
| Resolved tickets | Resolved ticket threads cannot receive new replies. |

## FAQ

**Where is the desktop application or database file?** The supported product is the web app.
The former local-desktop startup instructions no longer apply.

**Why is my balance lower than expected?** Check the statement. A booking hold reduces available
balance while the stay is in escrow; later entries can release, refund, charge, or pay out funds.

**Why can I not withdraw the full balance?** Only available funds can be withdrawn. Escrow-held
funds remain unavailable.

**Why can I not register as Host or Agent?** Those roles require a current administrator code.

**Why did an AI feature fail?** AI can be temporarily unavailable or degraded. Use manual
filters or write the description manually.

## Known limitations

- Wallet top-ups, withdrawals, escrow, and payouts are simulated; no payment provider is
  connected.
- The hosted backend may sleep on the free Render plan, so the first request can be slow or
  require **Re-check**.
- AI features depend on backend availability and have manual fallbacks.
- The ticket clarification assistant is asynchronous and may be unavailable or stale; it does
  not automatically recover every interrupted background job.
- Privileged registration codes are administrator-managed and are not published here.

## Troubleshooting

| Symptom | What to try |
| --- | --- |
| Backend unreachable | Wait up to a minute, choose **Re-check**, and retry. |
| Sign-in fails | Confirm the email and password, then use **Forgot password?** if needed. |
| Reset email missing | Check spam, verify the address, and request another reset email. |
| Registration code rejected | Request the current Host or Agent code from an administrator. |
| No listings appear | Clear filters and search again after confirming the backend is reachable. |
| AI search unavailable | Use the manual City, date, Guests, and Max rate filters. |
| Wallet action fails | Check that the amount is positive and that withdrawal does not exceed available funds. |

For local failures, see the [development environment runbook](../runbooks/dev-environment.html) and
the repository [README](https://github.com/CS3227-2610-MP3-SnoozeShare/CS3227-2610-MP3/blob/main/README.md).

## Glossary

| Term | Meaning |
| --- | --- |
| Available balance | Wallet funds not currently held in escrow and available to withdraw. |
| Booking hold | The escrow transaction created when a Guest books a stay. |
| Dispute window | The seven days after check-out during which a Guest can file a dispute. |
| Escrow | Funds reserved for a booking until completion or Agent resolution. |
| Host payout | Amount released to a Host after escrow completion, less the 3% platform fee. |
| Registration code | Administrator-managed code required for Host or Support Agent registration. |
| Ticket | A formal Guest support or dispute record with category, messages, and resolution. |

---

For product requirements and developer handover information, see the [Developer Guide](../developer-guide/).
