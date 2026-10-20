# Requirements

- **F1:** Given valid Firebase web configuration, auth screens shall call Firebase Email/Password SDK actions for sign-up, sign-in, sign-out and password reset and expose loading/success/error states.
- **F2:** Given a protected API request with a missing or invalid bearer token, the backend shall return `401` and shall not call a role-protected controller.
- **F3:** Given a verified Firebase UID with `email_verified=true` and without a profile, registration shall create one backend profile and return its role and ID; an unverified email shall receive `403 EMAIL_NOT_VERIFIED`.
- **F4:** Given HOST or AGENT registration with an absent or invalid role-specific code, the backend shall return `403 INVALID_REGISTRATION_CODE` and create no profile.
- **F5:** Given successful registration, Guest/Host shall have exactly one wallet and Agent shall have no wallet; a retry shall create no additional wallet.
- **F6:** Given a profile role and a mismatched role namespace request, the backend shall return `403 ROLE_MISMATCH`.
- **F7:** Given a valid token and profile, `/api/auth/me` shall return the profile keyed by the verified UID.
- **F8:** Given auth state transitions, the provider shall expose `loading`, `signed-out`, `needs-verification`, `needs-registration`, `authenticated` and `error`, and shall implement the exact guard matrix for sign-in, verification refresh, registration, wrong-role access, sign-out and backend failure.
- **N1:** Admin credentials, bearer tokens, passwords and registration codes shall never appear in committed files, response bodies or application logs.
- **N2:** Backend auth tests shall use a verifier double and frontend tests shall use Firebase client mocks; the suite shall make no network call to Firebase.
- **N3:** Concurrent or repeated registration for one UID shall produce at most one profile and one required wallet.
