# Frontend auth UI and state

The auth screens use the shared Fall Light stylesheet and the Claude artifact’s card, field, button, spacing and status treatment. The artifact does not show role/code fields, so registration extends the visual language with a role selector and conditional registration-code field.

## State

`AuthProvider` exposes:

- `firebaseUser`: Firebase client user or `null`;
- `profile`: backend profile or `null`;
- `role`: profile role or `null`;
- `status`: `loading`, `signed-out`, `needs-verification`, `needs-registration`, `authenticated`, or `error`;
- actions for sign-in, sign-up, sign-out and password reset.

On Firebase auth changes, the provider obtains the current ID token and calls `/api/auth/me`. Firebase `email_verified=false` transitions to `needs-verification`; the UI offers resend verification and a refresh-status action. A `404 PROFILE_NOT_REGISTERED` transitions to `needs-registration`; `401` signs the user out; `403 PROFILE_INACTIVE` produces a non-retryable unavailable state; `503` produces an `error` state with a retry action. Other failures are surfaced without exposing raw exceptions.

## Routes

- `/login`: email/password sign-in and link to registration.
- `/register`: email/password creation, role selection, conditional HOST/AGENT code and display name.
- `/forgot-password`: Firebase password-reset request.
- `/guest/*`, `/host/*`, `/agent/*`: protected client-side layouts. Feature content is outside W-AUTH; placeholder landing screens may be used for guard tests. Ordinary Next.js middleware does not perform Firebase auth checks in this workstream.

## Failure recovery

If Firebase signup succeeds but backend registration fails, keep the user signed in, show a recoverable error, and allow retrying registration. Do not silently create another Firebase account or ask for the password again. A Cancel/Sign out action calls Firebase sign-out and returns to `/login`. Successful registration redirects to the selected role landing route. `returnTo` is accepted only when it begins with `/` and does not begin with `//`; otherwise the redirect target is `/`.

## Guard matrix

| Auth state | Route request | Exact result |
|---|---|---|
| `loading` | Any protected route | Render the shared loading state; do not redirect until resolved |
| `signed-out` | Any protected route | Redirect to `/login?returnTo=<encoded-path>` |
| `needs-verification` | Any protected route | Render verification prompt; protected API calls are not made |
| `needs-registration` | Role route | Redirect to `/register` |
| `authenticated` | Matching role route | Render the role layout |
| `authenticated` | Wrong role route | Redirect to the role landing route |
| `error` from `503` | Any protected route | Render retry action; retain current route and Firebase session |

Email-verification tests cover resend success/error, refresh-status transition to `needs-registration`, and sign-out to `/login`.
