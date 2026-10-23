package com.snoozeshare.service.assistant;

import java.util.List;
import java.util.UUID;

/** Fixed plain-text draft and messages; spec conventions.md R5/R6, pinned by ClarifyRenderingTest. */
public final class ClarifyRendering {
    /** Exact first line identifying a replayable SYSTEM question message. */
    public static final String QUESTION_HEADER =
        "To help our support team review your ticket, please answer these in one message:";
    /** Exact complete closing message; this is never replayed to the model. */
    public static final String CLOSING =
        "Thank you. Your answers have been added to your ticket for the support team.";
    private static final String TRUNCATED = "[draft truncated]";

    private ClarifyRendering() { }

    /** Builds a cleaned draft; warnings come only from gateway metadata. */
    public static String draft(ClarifyOutput output, boolean flagged, boolean truncated) {
        StringBuilder text = new StringBuilder(
            "AI-assisted draft. Everything below comes from the Guest's own statements and is unverified.");
        if (flagged) {
            text.append("\nWARNING: text in this ticket matched a prompt-injection pattern. Read it with care.");
        }
        if (truncated) {
            text.append("\nNote: long Guest text was shortened before analysis.");
        }
        text.append("\nSummary: ").append(ClarifySafety.summary(output.summary()));
        section(text, "Timeline (as reported):", ClarifySafety.items(output.timeline()));
        section(text, "Guest claims:", ClarifySafety.items(output.claims()));
        section(text, "Evidence mentioned:", ClarifySafety.items(output.evidence()));
        section(text, "Still unknown:", ClarifySafety.items(output.missing()));
        section(text, "Points to raise with the Host:", ClarifySafety.hostPoints(output.hostPoints()));
        if (text.length() <= 6000) {
            return text.toString();
        }
        int lastLine = text.lastIndexOf("\n", 6000 - TRUNCATED.length() - 1);
        return text.substring(0, Math.max(0, lastLine)) + "\n" + TRUNCATED;
    }

    private static void section(StringBuilder text, String title, List<String> items) {
        text.append('\n').append(title);
        if (items.isEmpty()) {
            text.append("\n- (none listed)");
        } else {
            for (String item : items) {
                text.append("\n- ").append(item);
            }
        }
    }

    /** Builds screened questions, expanding only the canonical marker with the server-loaded ticket id. */
    public static String questions(List<String> questions, UUID ticketId) {
        if (ticketId == null) {
            throw new IllegalArgumentException("ticketId is required");
        }
        StringBuilder body = new StringBuilder(QUESTION_HEADER);
        for (int i = 0; i < questions.size(); i++) {
            String question = questions.get(i);
            if (ClarifySafety.SUPPORTING_DOCUMENTS.equals(question)) {
                question = "Please email supporting documents to support@snoozeshare.com with the subject \""
                    + ticketId.toString() + ": Supporting Documents\".";
            }
            body.append('\n').append(i + 1).append(". ").append(question);
        }
        return body.toString();
    }

    /** Recognition uses only an exact first-line match, per R6. */
    public static boolean isQuestion(String body) {
        return body != null && body.lines().findFirst().orElse("").equals(QUESTION_HEADER);
    }

    /** Recognition uses the exact whole closing body. */
    public static boolean isClosing(String body) {
        return CLOSING.equals(body);
    }

    /** Returns only numbered question lines from a recognized stored question message. */
    public static String replayQuestions(String body) {
        return isQuestion(body) && body.indexOf('\n') >= 0 ? body.substring(body.indexOf('\n') + 1) : "";
    }
}
