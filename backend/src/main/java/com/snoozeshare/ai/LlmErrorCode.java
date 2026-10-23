package com.snoozeshare.ai;

/** Why a call returned its fallback instead of model output; the table is in the spec's api.md. */
public enum LlmErrorCode {
    /** The gateway is switched off. */
    DISABLED,
    /** The caller's role is not allowed for the feature. */
    ROLE_DENIED,
    /** The per-user minute window or in-flight limit is used up. */
    USER_THROTTLED,
    /** The last user turn was empty after sanitizing. */
    BLOCKED_INPUT,
    /** The per-user or global daily cap is used up. */
    DAILY_CAP,
    /** SoCLaaS reported its spend budget as used up. */
    BUDGET_EXHAUSTED,
    /** Recent failures opened the circuit breaker. */
    CIRCUIT_OPEN,
    /** The scheduler queue is full. */
    QUEUE_FULL,
    /** The call waited too long for a scheduler slot. */
    QUEUE_TIMEOUT,
    /** SoCLaaS answered 429 for the request rate. */
    RATE_LIMITED,
    /** SoCLaaS answered 5xx. */
    SERVER,
    /** An attempt or the whole call ran out of time. */
    TIMEOUT,
    /** The connection failed. */
    NETWORK,
    /** SoCLaaS rejected the request (4xx other than 401, 403, 429) or answered a redirect. */
    BAD_REQUEST,
    /** SoCLaaS rejected the key (401). */
    AUTH,
    /** SoCLaaS forbade the key (403). */
    FORBIDDEN,
    /** The reply failed output validation or could not be used. */
    INVALID_OUTPUT,
    /** The model returned the refusal marker. */
    REFUSED,
    /** A cap count or the audit row could not be written. */
    AUDIT_FAILED
}
