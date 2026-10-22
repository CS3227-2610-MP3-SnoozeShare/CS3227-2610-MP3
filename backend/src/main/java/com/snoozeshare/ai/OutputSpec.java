package com.snoozeshare.ai;

/**
 * What a feature expects back: plain text, or a JSON object bound to a record.
 *
 * @param jsonType the record class for a JSON reply; null for text
 * @param maxOutputChars the longest accepted text reply in UTF-16 characters; 0 for JSON, where the response
 *     size cap and {@code maxTokens} bound the reply
 * @param allowLinks true lets URLs through output validation; role UIs still render output as plain text
 * @param <T> the output type
 */
public record OutputSpec<T>(Class<T> jsonType, int maxOutputChars, boolean allowLinks) {

    /**
     * A plain-text reply.
     *
     * @param maxOutputChars 1 to 4,000
     * @param allowLinks whether URLs may appear
     * @return the spec
     */
    public static OutputSpec<String> text(int maxOutputChars, boolean allowLinks) {
        return new OutputSpec<>(null, maxOutputChars, allowLinks);
    }

    /**
     * A JSON reply bound to a record validated with Jakarta Bean Validation.
     *
     * @param type the record class
     * @param allowLinks whether URLs may appear in string values
     * @param <T> the record type
     * @return the spec
     */
    public static <T> OutputSpec<T> json(Class<T> type, boolean allowLinks) {
        return new OutputSpec<>(type, 0, allowLinks);
    }

    /**
     * Whether the reply is JSON.
     *
     * @return true for a JSON spec
     */
    public boolean isJson() {
        return jsonType != null;
    }
}
