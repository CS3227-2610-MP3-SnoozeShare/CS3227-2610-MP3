package com.snoozeshare.ai.guard;

import java.text.Normalizer;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.ai.LlmErrorCode;
import com.snoozeshare.ai.OutputSpec;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Checks model output before any user sees it, in the five ordered steps of the spec's conventions.md § Validating
 * model output (F10). Holds no state; the mapper and validator are supplied by the caller.
 */
public final class OutputValidator {

    private static final Pattern FENCE_OPEN = Pattern.compile("^```[A-Za-z0-9_+.-]*[ \\t]*(?:\\R|$)");
    private static final Pattern REFUSAL = Pattern.compile("\\{\\s*\"refusal\"\\s*:\\s*true\\s*\\}");
    private static final Pattern KEY_SHAPE = Pattern.compile("clsk_[A-Za-z0-9]+_[A-Za-z0-9]+");
    private static final Pattern FORMAT_CHARS = Pattern.compile("\\p{Cf}");
    private static final Pattern LINK = Pattern.compile(
        "https?://|//[A-Za-z0-9]|www\\.|\\bjavascript:|\\bdata:|<[A-Za-z!/]|!\\[|\\]\\(", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper mapper;
    private final Validator validator;

    /**
     * Creates a validator.
     *
     * @param mapper the application mapper; copied and made strict (unknown properties, duplicate keys and trailing
     *     tokens all fail)
     * @param validator the Jakarta Bean Validation validator for JSON records
     */
    public OutputValidator(ObjectMapper mapper, Validator validator) {
        ObjectMapper strict = mapper.copy();
        strict.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        strict.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        strict.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        this.mapper = strict;
        this.validator = validator;
    }

    /**
     * The verdict on one reply.
     *
     * @param value the parsed output; null unless {@code code} is null
     * @param code null when the reply is usable, otherwise INVALID_OUTPUT or REFUSED
     * @param <T> the output type
     */
    public record Outcome<T>(T value, LlmErrorCode code) {
        /**
         * Whether the reply passed.
         *
         * @return true when {@code code} is null
         */
        public boolean ok() {
            return code == null;
        }
    }

    /**
     * Validates one reply.
     *
     * @param content {@code choices[0].message.content}; may be null
     * @param finishReason {@code choices[0].finish_reason}; may be null
     * @param spec what the feature expects
     * @param apiKey the configured key, searched for in the reply; may be blank
     * @param <T> the output type
     * @return the parsed value, or INVALID_OUTPUT or REFUSED
     */
    public <T> Outcome<T> validate(String content, String finishReason, OutputSpec<T> spec, String apiKey) {
        if (content == null || content.isBlank() || !"stop".equals(finishReason)) {
            return invalid();
        }
        String text = stripFences(content.strip());
        if (REFUSAL.matcher(text).matches()) {
            return new Outcome<>(null, LlmErrorCode.REFUSED);
        }
        if (leaks(text, spec.allowLinks(), apiKey)) {
            return invalid();
        }
        if (spec.isJson()) {
            return parseJson(text, spec, apiKey);
        }
        if (text.length() > spec.maxOutputChars()) {
            return invalid();
        }
        @SuppressWarnings("unchecked")
        T value = (T) text;
        return new Outcome<>(value, null);
    }

    private <T> Outcome<T> parseJson(String text, OutputSpec<T> spec, String apiKey) {
        try {
            JsonNode tree = mapper.readTree(text);
            if (tree == null || !tree.isObject() || stringLeaks(tree, spec.allowLinks(), apiKey)) {
                return invalid();
            }
            T value = mapper.treeToValue(tree, spec.jsonType());
            Set<ConstraintViolation<T>> violations = validator.validate(value);
            return violations.isEmpty() ? new Outcome<>(value, null) : invalid();
        } catch (Exception e) {
            return invalid();
        }
    }

    private boolean stringLeaks(JsonNode node, boolean allowLinks, String apiKey) {
        if (node.isTextual()) {
            return leaks(node.textValue(), allowLinks, apiKey);
        }
        if (node.isObject()) {
            for (Map.Entry<String, JsonNode> field : node.properties()) {
                if (leaks(field.getKey(), allowLinks, apiKey) || stringLeaks(field.getValue(), allowLinks, apiKey)) {
                    return true;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                if (stringLeaks(element, allowLinks, apiKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean leaks(String text, boolean allowLinks, String apiKey) {
        String t = FORMAT_CHARS.matcher(Normalizer.normalize(text, Normalizer.Form.NFKC)).replaceAll("");
        if (apiKey != null && !apiKey.isBlank() && t.contains(apiKey)) {
            return true;
        }
        if (KEY_SHAPE.matcher(t).find() || InputSanitizer.DELIMITER_TAG.matcher(t).find()) {
            return true;
        }
        return !allowLinks && LINK.matcher(t).find();
    }

    private static String stripFences(String text) {
        String t = text;
        Matcher open = FENCE_OPEN.matcher(t);
        if (open.find()) {
            t = t.substring(open.end());
        }
        t = t.strip();
        if (t.endsWith("```")) {
            t = t.substring(0, t.length() - 3);
        }
        return t.strip();
    }

    private static <T> Outcome<T> invalid() {
        return new Outcome<>(null, LlmErrorCode.INVALID_OUTPUT);
    }
}
