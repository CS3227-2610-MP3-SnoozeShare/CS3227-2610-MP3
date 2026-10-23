package com.snoozeshare.service.assistant;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** One normalization and deterministic screening implementation for model text (R4/R4b). */
public final class ClarifySafety {
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS;
    private static final Pattern DIGITS = Pattern.compile("\\d(?:[ -]?\\d){6,}", FLAGS);
    private static final List<Pattern> LIST_A = List.of(
        Pattern.compile("\\b(we|i)\\s*('ll|will|shall|can(?!'t)|would|'d|are going to|am going to|"
            + "'re going to|'m going to)\\b", FLAGS),
        Pattern.compile("\\b(you|u)\\s*('ll|will|are going to|'re going to|are entitled to|qualify for|"
            + "are eligible for)\\b.{0,40}\\b(get|receive|refund\\w*|reimburs\\w*|compensat\\w*|paid|credit\\w*|"
            + "money back|full)\\b", FLAGS),
        Pattern.compile("\\b(will|going to)\\s+be\\s+(refund\\w*|reimburs\\w*|compensat\\w*|credit\\w*|approv\\w*|"
            + "given|paid)\\b", FLAGS),
        Pattern.compile("\\b(guarantee\\w*|promis\\w*|assur\\w*)\\b", FLAGS),
        Pattern.compile("\\b(your|the)\\s+(claim|request|ticket|dispute)\\s+(is|has been|will be|was)\\s+"
            + "(approv\\w*|accept\\w*|reject\\w*|den\\w*)\\b", FLAGS),
        Pattern.compile("\\b(your\\s+)?(refund|money|payment|payout)\\s+(is|will be)\\s+"
            + "(on its way|coming|processed|approved|issued)\\b", FLAGS));
    private static final List<Pattern> LIST_B = List.of(
        Pattern.compile("\\b(password|passcode|pin|otp|2fa|verification code|security code|one[- ]time|cvv|cvc|"
            + "expiry|card (number|details)|credit card|debit card|bank (account|details)|account number|iban|swift|"
            + "nric|passport|id (card|document)|identity (card|document))\\b", FLAGS),
        Pattern.compile("\\b(paynow|paylah|grabpay|venmo|paypal|crypto\\w*|bitcoin|gift card|western union|"
            + "wire transfer|"
            + "cash|bank transfer|transfer (the )?(money|funds))\\b", FLAGS),
        Pattern.compile("\\b(whatsapp|telegram|wechat|viber|messenger|instagram|facebook|sms|text me|call me|email me|"
            + "contact (me|the host|them) directly|outside (the )?(platform|app|snoozeshare)|off[- ]platform|"
            + "directly (to|with) the host)\\b", FLAGS),
        Pattern.compile("\\b(withdraw|drop|cancel|close|delete)\\s+(this|the|your)\\s+"
            + "(ticket|dispute|claim|complaint)\\b",
            FLAGS));
    private static final List<Pattern> LIST_C = List.of(
        Pattern.compile("\\b(should|must|ought to|recommend\\w*|advis\\w*|suggest\\w*|propose\\w*)\\b.{0,80}\\b"
            + "(approv\\w*|reject\\w*|den(y|ied|ial)|refund\\w*|pay|paid|payout\\w*|settl\\w*|compensat\\w*|"
            + "reimburs\\w*|resolv\\w*|grant\\w*|award\\w*)\\b", FLAGS),
        Pattern.compile("\\b(approv\\w*|reject\\w*|den(y|ied|ial)|grant\\w*|award\\w*|refus\\w*|giv\\w*)\\b.{0,40}\\b"
            + "(refund\\w*|claim\\w*|request\\w*|ticket\\w*|payout\\w*|dispute\\w*|money back|compensat\\w*|"
            + "reimburs\\w*)\\b", FLAGS),
        Pattern.compile("\\b(entitled|eligible|valid|justified|legitimate)\\b.{0,30}\\b"
            + "(refund\\w*|claim\\w*|compensat\\w*)\\b",
            FLAGS));

    private ClarifySafety() { }

    /** NFKC plus line-safe spaces, hidden-character removal and normalized apostrophes. */
    public static String clean(String value) {
        StringBuilder result = new StringBuilder();
        // Preserve the intended apostrophe before NFKC decomposes the spacing acute accent.
        String apostrophes = value.replace('´', '\'');
        Normalizer.normalize(apostrophes, Normalizer.Form.NFKC).codePoints().forEach(cp -> {
            int type = Character.getType(cp);
            if (Character.isWhitespace(cp) || type == Character.SPACE_SEPARATOR
                    || type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR) {
                result.append(' ');
            } else if (type != Character.CONTROL && type != Character.FORMAT) {
                result.appendCodePoint("’‘ʼ′´`".indexOf(cp) >= 0 ? '\'' : cp);
            }
        });
        return result.toString().replaceAll(" +", " ").trim();
    }

    /** First three distinct, nonblank questions that pass lists A/B and contact checks. */
    public static List<String> questions(List<String> values) {
        List<String> kept = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String value : values) {
            String text = clean(value);
            if (!text.isBlank() && !matches(LIST_A, text) && !matches(LIST_B, text)
                    && !text.contains("@") && !DIGITS.matcher(text).find()
                    && seen.add(text.toLowerCase(Locale.ROOT))) {
                kept.add(text);
                if (kept.size() == 3) {
                    break;
                }
            }
        }
        return List.copyOf(kept);
    }

    /** Summary screens promises and recommendations while retaining Guest-reported sensitive facts. */
    public static String summary(String value) {
        String text = clean(value);
        return matches(LIST_A, text) || matches(LIST_C, text)
            ? "[summary removed by the safety screen]" : text;
    }

    /** Host facts screen all three lists. */
    public static List<String> hostPoints(List<String> values) {
        return items(values).stream().filter(text -> !matches(LIST_A, text)
            && !matches(LIST_B, text) && !matches(LIST_C, text)).toList();
    }

    /** Other draft lists retain claims and evidence, removing only empty/none items. */
    public static List<String> items(List<String> values) {
        return values.stream().map(ClarifySafety::clean)
            .filter(text -> !text.isBlank() && !text.equalsIgnoreCase("none")).toList();
    }

    private static boolean matches(List<Pattern> patterns, String text) {
        return patterns.stream().anyMatch(pattern -> pattern.matcher(text).find());
    }
}
