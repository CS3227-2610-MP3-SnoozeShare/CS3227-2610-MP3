# UI

One page, `/`, role-less (no auth in this workstream).

| State | Trigger | Shown |
|---|---|---|
| Loading | First render until the fetch settles | Text `Backend: checking...`; the `Re-check` button is disabled |
| Up | `GET {apiBaseUrl}/actuator/health` returns 200 and JSON `status` equal to `UP` | Text `Backend: UP`; button enabled |
| Unreachable | Network error, CORS rejection, timeout, non-200, non-JSON, or `status` not `UP` | Text `Backend: unreachable`; button enabled |

- Heading `SnoozeShare` above the status; one Shadcn `Button` labelled `Re-check` below it that repeats the fetch and returns to Loading. It proves Shadcn/ui is wired; no other component is added. Shadcn's generated dark-mode CSS is left unused.
- `src/lib/env.ts` is the only code that reads `NEXT_PUBLIC_API_BASE_URL`; it exports `apiBaseUrl()` and a `validateEnv()` that `next.config.ts` calls. Unset in development, `apiBaseUrl()` returns `http://localhost:8080`; in a production build (`NODE_ENV=production`) `validateEnv()` throws `NEXT_PUBLIC_API_BASE_URL is required` when it is unset or not an `http(s)` URL without a trailing slash. CI and Vercel set it.
- The fetch runs in the browser only (client component) with a 5 s timeout; unmounting the page aborts an in-flight fetch.
- Layout works from 360 px wide; text only, no images; light theme only.
