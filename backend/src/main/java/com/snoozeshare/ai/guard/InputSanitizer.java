package com.snoozeshare.ai.guard;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Cleans one piece of user-influenced text. The seven steps and their order are the contract of the spec's
 * conventions.md: NFKC, control and format characters, pre-cap, PII, delimiter tags, trim, final cap.
 */
public final class InputSanitizer {

    /** Prompt delimiter tags in any case or spacing, also an unclosed fragment; possessive, so no backtracking. */
    static final Pattern DELIMITER_TAG = Pattern.compile(
        "<\\s*+/?+\\s*+(?:user_input|untrusted_data|context)\\b(?:[^<>]*+>)?", Pattern.CASE_INSENSITIVE);

    private static final Pattern CONTROL_AND_FORMAT =
        Pattern.compile("[\\u0000-\\u0008\\u000B-\\u001F\\u007F-\\u009F\\p{Cf}]");
    private static final int PRE_CAP_FACTOR = 4;

    private InputSanitizer() { }

    /**
     * The outcome of sanitizing.
     *
     * @param text the cleaned text, at most {@code limit} code points
     * @param truncated true when the pre-cap or the final cap removed anything
     * @param flagged true when the injection detector matched after control characters were removed
     */
    public record Sanitized(String text, boolean truncated, boolean flagged) { }

    /**
     * Sanitizes one text.
     *
     * @param raw the text as received, never null
     * @param limit the longest result in code points
     * @param redactPii whether step 4 runs; false for the trusted context
     * @return the cleaned text with its truncation and injection flags
     */
    public static Sanitized sanitize(String raw, int limit, boolean redactPii) {
        String t = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        t = CONTROL_AND_FORMAT.matcher(t).replaceAll("");
        boolean flagged = InjectionDetector.isSuspicious(t);
        boolean cut = false;
        int preCap = limit * PRE_CAP_FACTOR;
        if (t.codePointCount(0, t.length()) > preCap) {
            t = t.substring(0, t.offsetByCodePoints(0, preCap));
            t = dropTrailingDigitRun(t);
            cut = true;
        }
        if (redactPii) {
            t = PiiRedactor.redact(t);
        }
        String previous;
        do {
            previous = t;
            t = DELIMITER_TAG.matcher(t).replaceAll("");
        } while (!t.equals(previous));
        t = t.strip();
        // Removing delimiter tags can join fragments into a phrase ("ignore previous <user_input>instructions").
        flagged |= InjectionDetector.isSuspicious(t);
        if (t.codePointCount(0, t.length()) > limit) {
            t = t.substring(0, t.offsetByCodePoints(0, limit));
            cut = true;
        }
        return new Sanitized(t, cut, flagged);
    }

    private static String dropTrailingDigitRun(String text) {
        int end = text.length();
        while (end > 0) {
            char c = text.charAt(end - 1);
            if ((c >= '0' && c <= '9') || c == ' ' || c == '-') {
                end--;
            } else {
                break;
            }
        }
        return text.substring(0, end);
    }
}
