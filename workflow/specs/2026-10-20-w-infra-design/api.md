# API

Two operations, both public. W-AUTH adds the security layer; until then the backend has no authenticated endpoint.

Backend runtime settings that these operations rely on (decided here so tests can assert them):
- `server.port=${PORT:8080}`; `spring.profiles.default=dev` (so `./gradlew bootRun` uses `dev`; Render sets `SPRING_PROFILES_ACTIVE=prod`).
- `management.endpoints.web.exposure.include=health`; `management.endpoints.web.discovery.enabled=false`; `management.endpoint.health.show-details=never`; `management.endpoint.health.show-components=never`; the health group contains the `db` indicator only.
- `spring.datasource.hikari.connection-timeout=5000` (ms), so a down database yields 503 and a failed start within 20 s.
- Actuator CORS is configured separately from application CORS in Spring Boot, so one property feeds both: `management.endpoints.web.cors.allowed-origins=${snoozeshare.cors.allowed-origin}` with methods `GET,OPTIONS` and headers `Authorization,Content-Type`; and `CorsConfig` (a `WebMvcConfigurer`) for application endpoints with methods `GET,POST,PUT,DELETE,OPTIONS`.

## GET /actuator/health
| Field | Definition |
|---|---|
| Caller and principal | Anyone, unauthenticated (Render health check, the frontend page). No role |
| Authorization rule | None. This is the only public surface; W-AUTH must keep it public |
| Tenancy scope | None; returns no user data |
| Inputs | None |
| Outputs | `{"status":"UP"}` or `{"status":"DOWN"}`; no components, details, URLs or versions under any query string |
| Errors | Database unreachable -> 503 `{"status":"DOWN"}`. Any other path under `/actuator` (including `/actuator` itself) -> 404 |
| Idempotency | Safe and idempotent |
| Transaction boundary | Read-only connectivity check; no commit |
| Events / audit | None. Not audited |
| Logging | Request line at the framework default level; never the datasource URL, username or password |
| Compatibility | New. Render's `healthCheckPath` and the frontend page call it |

## OPTIONS /actuator/health (CORS preflight)
| Field | Definition |
|---|---|
| Caller and principal | A browser on the configured origin, unauthenticated |
| Authorization rule | Origin must equal `snoozeshare.cors.allowed-origin` exactly (scheme, host, port, no trailing slash, no `*`); enforced by the actuator CORS keys above, and for application endpoints by `com.snoozeshare.config.CorsConfig` |
| Tenancy scope | None |
| Inputs | `Origin`, `Access-Control-Request-Method`, `Access-Control-Request-Headers` |
| Outputs | Allowed methods `GET, OPTIONS` on the actuator and `GET, POST, PUT, DELETE, OPTIONS` on application endpoints; allowed headers `Authorization, Content-Type`; credentials not allowed; max age 3600 s |
| Errors | Origin not equal -> 403 with no CORS headers (Spring default for a rejected preflight). Property blank, containing `*`, or ending in `/` -> the application does not start (validation on `CorsProperties`) |
| Idempotency | Safe |
| Transaction boundary | None |
| Events / audit | None |
| Logging | Default framework logging only |
| Compatibility | `CorsConfig` maps `/**`, so W-AUTH's endpoints inherit it |
