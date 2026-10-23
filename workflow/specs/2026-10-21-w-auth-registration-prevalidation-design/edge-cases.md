# Edge cases and abuse cases

| ID | Requirement | Scenario | Expected behaviour |
|---|---|---|---|
| E1 | F9, F10 | Wrong Host/Agent code | Return `403 INVALID_REGISTRATION_CODE`; do not call Firebase signup; no DB write |
| E2 | F13 | Guest submits code | Existing registration validation remains authoritative; pre-validation is not called |
| E3 | F11, F14 | Valid code then Firebase signup fails | No backend profile is created; surface the Firebase error without exposing the code |
| E4 | F12 | Valid pre-validation followed by modified-client invalid backend request | Authenticated backend endpoint rejects the invalid code; no profile or wallet is created |
| E5 | F10 | Repeated invalid pre-validation | The atomic IP+role counter returns `429` on the sixth invalid attempt; the atomic global IP counter returns `429` on the twenty-first; do not call Firebase signup |
| E6 | F11, N4 | Concurrent valid or invalid pre-validation requests | Counter updates are atomic; valid requests create no state; invalid requests cannot exceed the configured boundary through races |
| E7 | F9, F14 | Backend unavailable during pre-validation | Do not call Firebase signup; show retryable backend error |
| E8 | F12 | Code is changed between pre-validation and registration | Final authenticated check rejects the stale code; no profile or wallet is created |

| E9 | F10, N4 | Ten-minute boundary and successful validation | Counters expire at the defined boundary; a valid check resets only the selected role bucket; authenticated limiter buckets remain separate |
| E10 | F9, F14 | Forwarded header supplied by a client or proxy | Ignore `Forwarded` and `X-Forwarded-For` and use the direct peer address |
| E11 | F14 | Unsupported media type or malformed body | Return `400 VALIDATION_ERROR` in the established error shape with `Cache-Control: no-store` |
| E12 | F9, F14 | Retry after Firebase signup or duplicate submit | Disable submission during the flow; on retry with an existing Firebase session, skip signup and call backend registration only |

## What could corrupt data

- Pre-validation must not write Firebase, profile, wallet, or session state.
- The final authenticated registration check must remain in place so a stale pre-validation result
  cannot create a profile after code rotation.
- Existing unique Firebase UID and wallet constraints remain responsible for retry and concurrency
  safety after signup.
