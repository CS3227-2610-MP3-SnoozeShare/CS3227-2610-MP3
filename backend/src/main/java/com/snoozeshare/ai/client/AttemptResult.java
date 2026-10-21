package com.snoozeshare.ai.client;

import com.snoozeshare.ai.LlmErrorCode;

/**
 * The outcome of one HTTP attempt against SoCLaaS: a usable reply or a classified failure. Neither case
 * carries the upstream error body, a header or the key.
 */
public sealed interface AttemptResult {

    /**
     * A 200 reply with a non-blank first choice.
     *
     * @param content the text of {@code choices[0].message.content}
     * @param finishReason {@code choices[0].finish_reason}, or null when absent
     * @param tokensIn prompt tokens, 0 when {@code usage} is missing
     * @param tokensOut completion tokens, 0 when {@code usage} is missing
     */
    record Success(String content, String finishReason, int tokensIn, int tokensOut) implements AttemptResult {
        @Override
        public String toString() {
            return "Success[tokensIn=" + tokensIn + ", tokensOut=" + tokensOut + "]";
        }
    }

    /**
     * A failed attempt.
     *
     * @param code the gateway error code of the failure
     * @param retryAfterSeconds the {@code Retry-After} header in whole seconds, or null when absent or not a number
     * @param budget true when a 429 matched the budget rule (the code is then BUDGET_EXHAUSTED)
     * @param httpStatus the HTTP status, or 0 when no response was received
     */
    record Failure(LlmErrorCode code, Integer retryAfterSeconds, boolean budget, int httpStatus)
        implements AttemptResult { }
}
