package com.snoozeshare.ai;

import java.util.UUID;

/**
 * The outcome of {@code LlmGateway.run}. A failure never throws; it returns the feature's fallback with a code.
 *
 * @param ok true when {@code value} is validated model output
 * @param value the validated output, or the feature's fallback when {@code ok} is false
 * @param code null when ok; otherwise why the fallback was returned
 * @param flagged true when the input matched an injection pattern; role code decides what to do about it
 * @param truncated true when input was cut to fit the limits
 * @param callId a fresh random id for this call, also when no audit row is written
 * @param <T> the feature's output type
 */
public record LlmResult<T>(boolean ok, T value, LlmErrorCode code, boolean flagged, boolean truncated,
                           UUID callId) {

    /**
     * A successful result.
     *
     * @param value the validated output
     * @param flagged whether the input matched an injection pattern
     * @param truncated whether input was cut
     * @param callId the call id
     * @param <T> the output type
     * @return the result
     */
    public static <T> LlmResult<T> ok(T value, boolean flagged, boolean truncated, UUID callId) {
        return new LlmResult<>(true, value, null, flagged, truncated, callId);
    }

    /**
     * A degraded result carrying the feature's fallback.
     *
     * @param fallback the feature's fallback value
     * @param code why the call degraded
     * @param flagged whether the input matched an injection pattern
     * @param truncated whether input was cut
     * @param callId the call id
     * @param <T> the output type
     * @return the result
     */
    public static <T> LlmResult<T> degraded(T fallback, LlmErrorCode code, boolean flagged, boolean truncated,
                                            UUID callId) {
        return new LlmResult<>(false, fallback, code, flagged, truncated, callId);
    }
}
