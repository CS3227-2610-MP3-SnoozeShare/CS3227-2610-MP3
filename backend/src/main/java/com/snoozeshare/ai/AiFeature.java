package com.snoozeshare.ai;

import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.snoozeshare.ai.config.LlmProperties;
import com.snoozeshare.domain.enums.Role;

/**
 * One AI feature, registered as a Spring bean by the role workstream that owns it.
 *
 * @param id unique, {@code ^[a-z]+\.[a-zA-Z]+$}, at most 40 characters; appears in audit rows
 * @param allowedRoles non-empty subset of GUEST, HOST and AGENT
 * @param tier which configured model to use
 * @param systemPrompt trusted instructions, 1 to 6,000 characters
 * @param maxTokens reply token limit, 1 to 4,000
 * @param temperature sampling temperature, 0.0 to 1.0
 * @param priority 0 (staff-facing, most urgent) to 2
 * @param perUserPerMinute calls one user may start per rolling 60 seconds, 1 to 60
 * @param maxInputChars longest accepted text per turn, 1 to 8,000
 * @param maxTurns most turns sent, 1 to 30
 * @param output what the reply must look like
 * @param fallback builds the value returned when the call degrades; never calls the gateway or the database
 * @param mockResponse raw model text used in mock mode, validated like a real reply
 * @param <T> the output type
 */
public record AiFeature<T>(String id, Set<Role> allowedRoles, ModelTier tier, String systemPrompt, int maxTokens,
                           double temperature, int priority, int perUserPerMinute, int maxInputChars, int maxTurns,
                           OutputSpec<T> output, Function<LlmCall, T> fallback, String mockResponse) {

    /** Default for {@code maxInputChars}. */
    public static final int DEFAULT_MAX_INPUT_CHARS = 2_000;
    /** Default for {@code maxTurns}. */
    public static final int DEFAULT_MAX_TURNS = 10;

    private static final Pattern ID = Pattern.compile("^[a-z]+\\.[a-zA-Z]+$");
    private static final int PROMPT_OVERHEAD_CHARS = 15_000;

    /** A feature with the default input limits. */
    public AiFeature(String id, Set<Role> allowedRoles, ModelTier tier, String systemPrompt, int maxTokens,
                     double temperature, int priority, int perUserPerMinute, OutputSpec<T> output,
                     Function<LlmCall, T> fallback, String mockResponse) {
        this(id, allowedRoles, tier, systemPrompt, maxTokens, temperature, priority, perUserPerMinute,
            DEFAULT_MAX_INPUT_CHARS, DEFAULT_MAX_TURNS, output, fallback, mockResponse);
    }

    /**
     * Checks every registration rule of the spec's api.md § AiFeature.
     *
     * @param properties the gateway settings, for the prompt-size bound
     * @throws IllegalArgumentException naming the feature id and the field that breaks a rule
     */
    public void validate(LlmProperties properties) {
        if (id == null || id.length() > 40 || !ID.matcher(id).matches()) {
            throw bad("id", "must match ^[a-z]+\\.[a-zA-Z]+$ and be at most 40 characters");
        }
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            throw bad("allowedRoles", "must not be empty");
        }
        for (Role role : allowedRoles) {
            if (role != Role.GUEST && role != Role.HOST && role != Role.AGENT) {
                throw bad("allowedRoles", "may contain only GUEST, HOST and AGENT");
            }
        }
        if (tier == null) {
            throw bad("tier", "must not be null");
        }
        if (systemPrompt == null || systemPrompt.isBlank() || systemPrompt.length() > 6_000) {
            throw bad("systemPrompt", "must be 1 to 6000 characters");
        }
        range("maxTokens", maxTokens, 1, 4_000);
        if (!(temperature >= 0.0 && temperature <= 1.0)) {
            throw bad("temperature", "must be 0.0 to 1.0");
        }
        range("priority", priority, 0, 2);
        range("perUserPerMinute", perUserPerMinute, 1, 60);
        range("maxInputChars", maxInputChars, 1, 8_000);
        range("maxTurns", maxTurns, 1, 30);
        if ((long) maxInputChars * (maxTurns + 1) + PROMPT_OVERHEAD_CHARS > properties.maxPromptChars()) {
            throw bad("maxInputChars", "x (maxTurns + 1) + 15000 must not exceed snoozeshare.ai.max-prompt-chars");
        }
        validateOutput();
        if (fallback == null) {
            throw bad("fallback", "must not be null");
        }
        if (mockResponse == null) {
            throw bad("mockResponse", "must not be null");
        }
    }

    private void validateOutput() {
        if (output == null) {
            throw bad("output", "must not be null");
        }
        if (output.isJson()) {
            if (!output.jsonType().isRecord()) {
                throw bad("output", "JSON type must be a record");
            }
        } else {
            range("output.maxOutputChars", output.maxOutputChars(), 1, 4_000);
        }
    }

    private void range(String field, long value, long min, long max) {
        if (value < min || value > max) {
            throw bad(field, "must be " + min + " to " + max);
        }
    }

    private IllegalArgumentException bad(String field, String rule) {
        return new IllegalArgumentException("AI feature '" + id + "': " + field + " " + rule);
    }
}
