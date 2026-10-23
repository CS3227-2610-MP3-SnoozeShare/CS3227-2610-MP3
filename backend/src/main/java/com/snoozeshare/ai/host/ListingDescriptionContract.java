package com.snoozeshare.ai.host;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

/** Shared value and validation rules for the Host listing-description assistant. */
public final class ListingDescriptionContract {

    public static final int MAX_INPUT_CHARS = 8_000;
    public static final int MAX_FEEDBACK_CHARS = 240;
    private static final Pattern WORD = Pattern.compile(
        "[\\p{L}\\p{N}]+(?:['’\\-][\\p{L}\\p{N}]+)*",
        Pattern.UNICODE_CHARACTER_CLASS);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private ListingDescriptionContract() {
    }

    public enum WritingStyle {
        FRIENDLY, PROFESSIONAL, PERSUASIVE
    }

    public enum Mode {
        EVALUATE, GENERATE
    }

    public enum Outcome {
        SUCCESS, DEGRADED
    }

    public enum TargetAudience {
        ALL_TRAVELLERS, FAMILIES, COUPLES, BUSINESS_TRAVELLERS
    }

    public enum Status {
        PASS, IMPROVE
    }

    public enum DescriptionFocus {
        BALANCED, COMFORT, PRACTICALITY
    }

    public record ListingFacts(String title, String propertyType, String city, String region,
                               Integer maxGuests, Integer bedrooms, Integer bathrooms,
                               List<String> amenities, String checkInTime, String checkOutTime) {
        public ListingFacts {
            amenities = amenities == null ? List.of() : amenities.stream().filter(Objects::nonNull)
                .distinct().toList();
        }
    }

    public static int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        Matcher matcher = WORD.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public static void requireContextWithinLimit(String context) {
        if (context == null) {
            throw new IllegalArgumentException("listing-description context must not be null");
        }
        String normalized = Normalizer.normalize(context, Normalizer.Form.NFKC);
        if (normalized.length() > MAX_INPUT_CHARS) {
            throw new IllegalArgumentException("listing-description context exceeds 8000 characters");
        }
    }

    public static void requireFeedback(String feedback) {
        if (feedback == null || feedback.isBlank() || feedback.length() > MAX_FEEDBACK_CHARS
            || feedback.indexOf('\n') >= 0 || feedback.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("analysis feedback must be 1 to 240 single-line characters");
        }
    }

    public static String serializeContext(String mode, String style, String audience, String focus,
                                          String currentDescription, ListingFacts facts) {
        Objects.requireNonNull(facts, "listing facts must not be null");
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("mode", mode);
        root.put("writingStyle", style);
        root.put("targetAudience", audience);
        root.put("descriptionLength", "50-150");
        root.put("descriptionFocus", focus);
        root.put("currentDescription", currentDescription == null ? "" : currentDescription);
        root.put("listing", facts);
        try {
            String context = JSON.writeValueAsString(root);
            requireContextWithinLimit(context);
            return context;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("listing-description context could not be serialized", exception);
        }
    }
}
