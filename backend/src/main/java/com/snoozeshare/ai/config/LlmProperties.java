package com.snoozeshare.ai.config;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The {@code snoozeshare.ai.*} settings. Every component carries its default so a context that sets only some
 * keys still binds; an out-of-range value fails startup naming the key. The key never appears in
 * {@link #toString()}.
 *
 * @param enabled whether the gateway runs
 * @param mock whether calls are answered from the feature's mock response instead of SoCLaaS
 * @param baseUrl the SoCLaaS base URL ending {@code /v1}; required when enabled and not mock
 * @param apiKey the SoCLaaS key; required when enabled and not mock
 * @param modelFast model name for the FAST tier
 * @param modelStandard model name for the STANDARD tier
 * @param modelStrong model name for the STRONG tier
 * @param maxRpm requests per minute the process may send, 1 to 90 (the development key allows 30)
 * @param maxConcurrent requests in flight at once, 2 to 16
 * @param maxQueue jobs that may wait for a slot, 1 to 200
 * @param queueWaitMs longest wait for a slot, 100 to 60,000
 * @param requestTimeoutMs longest single attempt, 1,000 to 120,000
 * @param deadlineMs longest whole call, at least {@code requestTimeoutMs}
 * @param maxRetries retries after the first attempt, 0 to 5
 * @param breakerFailures failed attempts that open the circuit breaker, 3 to 50
 * @param breakerOpenMs how long the breaker stays open, 1,000 to 600,000
 * @param budgetBlockMs how long a budget 429 blocks calls, 60,000 to 86,400,000
 * @param perUserMaxInFlight calls one user may have in flight, 1 to 8
 * @param dailyCapPerUser calls one user may make per UTC day, 1 to 10,000
 * @param globalDailyCap calls all users may make per UTC day, 1 to 100,000
 * @param priorityZeroReservePercent share of the global cap only priority 0 calls may use, 0 to 50
 * @param maxPromptChars upper bound of the assembled prompt, 2,000 to 200,000
 * @param responseFormatJson whether JSON features send {@code response_format}
 * @param reasoningEffort sent as {@code reasoning_effort}; {@code none} by default, empty omits the field,
 *     otherwise {@code low}, {@code medium} or {@code high}
 */
@ConfigurationProperties(prefix = "snoozeshare.ai")
public record LlmProperties(
    @DefaultValue("false") boolean enabled,
    @DefaultValue("false") boolean mock,
    @DefaultValue("") String baseUrl,
    @DefaultValue("") String apiKey,
    @DefaultValue("llama3.1:8b") String modelFast,
    @DefaultValue("qwen3.5:9b") String modelStandard,
    @DefaultValue("gemma4:26b") String modelStrong,
    @DefaultValue("25") int maxRpm,
    @DefaultValue("4") int maxConcurrent,
    @DefaultValue("20") int maxQueue,
    @DefaultValue("10000") long queueWaitMs,
    @DefaultValue("30000") long requestTimeoutMs,
    @DefaultValue("45000") long deadlineMs,
    @DefaultValue("2") int maxRetries,
    @DefaultValue("5") int breakerFailures,
    @DefaultValue("30000") long breakerOpenMs,
    @DefaultValue("900000") long budgetBlockMs,
    @DefaultValue("2") int perUserMaxInFlight,
    @DefaultValue("200") int dailyCapPerUser,
    @DefaultValue("3000") int globalDailyCap,
    @DefaultValue("5") int priorityZeroReservePercent,
    @DefaultValue("40000") int maxPromptChars,
    @DefaultValue("false") boolean responseFormatJson,
    @DefaultValue("none") String reasoningEffort) {

    private static final String PREFIX = "snoozeshare.ai.";
    private static final Set<String> REASONING_EFFORTS = Set.of("", "none", "low", "medium", "high");

    /** Checks every range and cross-field rule; a violation names the key. */
    public LlmProperties {
        baseUrl = baseUrl == null ? "" : baseUrl.trim();
        apiKey = apiKey == null ? "" : apiKey.trim();
        reasoningEffort = reasoningEffort == null ? "" : reasoningEffort.trim();
        checkModel("model-fast", modelFast);
        checkModel("model-standard", modelStandard);
        checkModel("model-strong", modelStrong);
        range("max-rpm", maxRpm, 1, 90);
        range("max-concurrent", maxConcurrent, 2, 16);
        range("max-queue", maxQueue, 1, 200);
        range("queue-wait-ms", queueWaitMs, 100, 60_000);
        range("request-timeout-ms", requestTimeoutMs, 1_000, 120_000);
        if (deadlineMs < requestTimeoutMs) {
            throw invalid("deadline-ms", "must be at least request-timeout-ms");
        }
        range("max-retries", maxRetries, 0, 5);
        range("breaker-failures", breakerFailures, 3, 50);
        range("breaker-open-ms", breakerOpenMs, 1_000, 600_000);
        range("budget-block-ms", budgetBlockMs, 60_000, 86_400_000);
        range("per-user-max-in-flight", perUserMaxInFlight, 1, 8);
        range("daily-cap-per-user", dailyCapPerUser, 1, 10_000);
        range("global-daily-cap", globalDailyCap, 1, 100_000);
        range("priority-zero-reserve-percent", priorityZeroReservePercent, 0, 50);
        range("max-prompt-chars", maxPromptChars, 2_000, 200_000);
        if (!REASONING_EFFORTS.contains(reasoningEffort)) {
            throw invalid("reasoning-effort", "must be empty, none, low, medium or high");
        }
        if (enabled && !mock) {
            checkBaseUrl(baseUrl);
            if (isBlankOrPlaceholder(apiKey)) {
                throw invalid("api-key", "must be set when the gateway is enabled and not in mock mode");
            }
        }
    }

    /**
     * Whether a setting is unset: blank, or still holding an unresolved {@code ${...}} placeholder.
     *
     * @param value the setting value
     * @return true when it counts as missing
     */
    public static boolean isBlankOrPlaceholder(String value) {
        return value == null || value.isBlank() || value.contains("${");
    }

    private static void checkBaseUrl(String url) {
        if (isBlankOrPlaceholder(url)) {
            throw invalid("base-url", "must be set when the gateway is enabled and not in mock mode");
        }
        boolean https = url.startsWith("https://");
        boolean loopbackHttp = url.matches("^http://(localhost|127\\.0\\.0\\.1|\\[::1\\])(:\\d+)?/.*");
        if (!https && !loopbackHttp) {
            throw invalid("base-url", "must start with https:// (http:// is allowed only for localhost)");
        }
        if (!url.endsWith("/v1") || url.chars().anyMatch(Character::isWhitespace)) {
            throw invalid("base-url", "must end with /v1, with no trailing slash and no spaces");
        }
    }

    private static void checkModel(String key, String value) {
        if (value == null || value.isBlank() || value.length() > 100) {
            throw invalid(key, "must be 1 to 100 characters");
        }
    }

    private static void range(String key, long value, long min, long max) {
        if (value < min || value > max) {
            throw invalid(key, "must be " + min + " to " + max);
        }
    }

    private static IllegalArgumentException invalid(String key, String rule) {
        return new IllegalArgumentException(PREFIX + key + " " + rule);
    }

    @Override
    public String toString() {
        return "LlmProperties[enabled=" + enabled + ", mock=" + mock + ", baseUrl=" + baseUrl
            + ", apiKey=***, modelFast=" + modelFast + ", modelStandard=" + modelStandard
            + ", modelStrong=" + modelStrong + ", maxRpm=" + maxRpm + ", maxConcurrent=" + maxConcurrent
            + ", maxQueue=" + maxQueue + ", queueWaitMs=" + queueWaitMs + ", requestTimeoutMs=" + requestTimeoutMs
            + ", deadlineMs=" + deadlineMs + ", maxRetries=" + maxRetries + ", breakerFailures=" + breakerFailures
            + ", breakerOpenMs=" + breakerOpenMs + ", budgetBlockMs=" + budgetBlockMs
            + ", perUserMaxInFlight=" + perUserMaxInFlight + ", dailyCapPerUser=" + dailyCapPerUser
            + ", globalDailyCap=" + globalDailyCap + ", priorityZeroReservePercent=" + priorityZeroReservePercent
            + ", maxPromptChars=" + maxPromptChars + ", responseFormatJson=" + responseFormatJson
            + ", reasoningEffort=" + reasoningEffort + "]";
    }
}
