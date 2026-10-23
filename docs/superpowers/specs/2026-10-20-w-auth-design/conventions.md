# Conventions and security rules

- Use Firebase Admin `GoogleCredentials.getApplicationDefault()` for backend initialization. Local credentials come from `GOOGLE_APPLICATION_CREDENTIALS`; Render supplies the service-account JSON through a protected secret file or equivalent runtime environment and `GOOGLE_APPLICATION_CREDENTIALS` points to it. Missing credentials fail backend startup with a safe configuration error.
- Verify ID tokens with Firebase Admin revocation checking enabled, and require the expected Firebase project issuer/audience and non-expired token. Invalid, expired, revoked or disabled-user tokens map to `401 INVALID_TOKEN`; an unavailable Admin provider maps to `503 AUTH_PROVIDER_UNAVAILABLE`.
- Local Firebase web configuration requires `NEXT_PUBLIC_FIREBASE_API_KEY`, `NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN`, `NEXT_PUBLIC_FIREBASE_PROJECT_ID`, `NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET`, `NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID` and `NEXT_PUBLIC_FIREBASE_APP_ID`; a production build fails if any is missing. `FIREBASE_PROJECT_ID` is the backend project identifier and must match the verified token project.
- Missing or blank `SNOOZESHARE_HOST_REGISTRATION_CODE` or `SNOOZESHARE_AGENT_REGISTRATION_CODE` fails backend startup; invalid tokens and runtime Firebase Admin outages use the API error mapping in `api.md`.
- Keep Firebase initialization in configuration and inject a small token-verifier interface into the filter so tests do not need real Firebase credentials.
- Use Spring Security’s `SecurityFilterChain`; add `spring-boot-starter-security`, stateless sessions, CSRF disabled for bearer-token API requests, explicit public `GET /actuator/health` and `OPTIONS` handling, and JSON authentication/authorization error handlers.
- Store the verified Firebase UID in the authenticated principal. Resolve the application role from the database for authorization.
- On first registration only, accept the requested role from the body after validating it against the allowed enum and registration-code policy. Never accept UID or email from a request body; after registration, the database role is authoritative.
- Never log bearer tokens, service-account JSON, registration codes, passwords or raw Firebase exceptions.
- Keep the service-account JSON outside the repository. Frontend `NEXT_PUBLIC_FIREBASE_*` values are configuration identifiers, not Admin credentials.
- Use stable error codes for frontend branching and a generic response for authorization failures.
- The authenticated principal contains the verified Firebase UID and resolved backend user ID/role; downstream services derive actor ownership from it.
- Frontend route protection is client-side through `AuthProvider` and protected layouts; no Next.js middleware or Firebase session-cookie exchange is part of W-AUTH.
- Every role-sensitive service operation repeats the role/ownership check from the authenticated principal; URL filters are a coarse boundary, not the only authorization layer.
- The W-AUTH service-account deployment handoff uses the Render protected secret store and a runtime path exposed through `GOOGLE_APPLICATION_CREDENTIALS`; the exact Render secret-file wiring is verified in W-DEPLOY, while local development uses the documented JSON path.
