package com.snoozeshare.ai.guard;

import java.util.regex.Pattern;

/** Replaces personal data with fixed placeholders before text leaves the process (F3). */
public final class PiiRedactor {

    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
    private static final Pattern NRIC = Pattern.compile("\\b[STFGM]\\d{7}[A-Z]\\b");
    private static final Pattern PHONE = Pattern.compile("(?:\\+?65[\\s-]?)?\\b[689]\\d{3}[\\s-]?\\d{4}\\b");
    private static final Pattern EMAIL =
        Pattern.compile("[\\w.+-]{1,64}@[\\w-]{1,63}(?:\\.[\\w-]{1,63}){1,5}");

    private PiiRedactor() { }

    /**
     * Replaces, in this order, card numbers, NRIC or FIN numbers, Singapore phone numbers and e-mail addresses.
     *
     * @param text the text to clean, never null
     * @return the text with {@code [card]}, {@code [id-number]}, {@code [phone]} and {@code [email]} in place of
     *     personal data
     */
    public static String redact(String text) {
        String t = CARD.matcher(text).replaceAll("[card]");
        t = NRIC.matcher(t).replaceAll("[id-number]");
        t = PHONE.matcher(t).replaceAll("[phone]");
        return EMAIL.matcher(t).replaceAll("[email]");
    }
}
