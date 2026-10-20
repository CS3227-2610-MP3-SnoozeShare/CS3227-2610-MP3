# Frontend registration flow

For Guest, the current sequence remains Firebase signup followed by backend profile registration.

For Host and Agent, the sequence is:

1. Call a new unauthenticated `validateRegistrationCode` helper in `frontend/src/lib/api.ts`.
   It sends JSON to the public endpoint, treats `204` as success, parses the stable error body for
   `400`, `403`, and `429`, and maps network/5xx responses to a retryable backend error.
2. If validation succeeds, call Firebase Email/Password signup.
3. Call authenticated backend profile registration with the same role and code.
4. Redirect to the selected role landing route.

If step 1 fails, show `Registration code is invalid` for `INVALID_REGISTRATION_CODE`,
`Too many attempts. Try again later` for `REGISTRATION_RATE_LIMITED`, and `Unable to validate
registration right now` for validation/network/backend failures. Keep the page on `/register` and
do not call Firebase signup. If step 2 or later fails, retain the existing W-AUTH recovery
behavior: preserve the Firebase session when one exists, show a retryable error, and offer a sign-out
action that calls Firebase sign-out before returning to `/login`. A retry reruns pre-validation; if
the Firebase session already exists, it skips Firebase signup and calls only authenticated backend
profile registration after pre-validation. It never attempts to create the same Firebase account
again.

The submit control is disabled while pre-validation, Firebase signup, or profile registration is
in flight, so duplicate submits cannot race the sequence.
