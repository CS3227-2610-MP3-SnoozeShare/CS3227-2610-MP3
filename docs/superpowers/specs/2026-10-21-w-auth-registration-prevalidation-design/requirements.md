# Requirements

- **F9:** Given HOST or AGENT registration, the frontend shall call backend code validation before Firebase signup.
- **F10:** Given a wrong, blank, cross-role, or malformed privileged code, validation shall fail with a stable error and the frontend shall not call Firebase signup.
- **F11:** Given a valid privileged code, validation shall succeed without creating a database row, Firebase identity, or session, after which the frontend may call Firebase signup.
- **F12:** The authenticated `/api/auth/register` endpoint shall revalidate the code and preserve existing profile, wallet, role, idempotency, and rate-limit behavior.
- **F13:** Guest signup shall not call privileged code validation.
- **F14:** Validation failures and Firebase signup failures shall remain distinguishable to the frontend and shall not expose the configured code.
- **N4:** Backend and frontend tests shall cover ordering, no-signup-on-invalid-code, valid-code continuation, rate limiting, and final-check bypass resistance.
