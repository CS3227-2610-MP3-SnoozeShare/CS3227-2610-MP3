package com.snoozeshare.service.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** Exact template source: spec conventions.md R5; E6/E71/E72. */
class ClarifyRenderingTest {
    private static final UUID TICKET_ID = UUID.fromString("abcdefab-cdef-abcd-efab-cdefabcdefab");

    @Test
    void markerExpandsOnlyToFixedServerTicketInstructionAndReplaysStoredLineE89E72() {
        String instruction = "Please email supporting documents to support@snoozeshare.com with the subject "
            + "\"abcdefab-cdef-abcd-efab-cdefabcdefab: Supporting Documents\".";
        String body = ClarifyRendering.questions(List.of("[supporting-documents]", "When?"), TICKET_ID);
        assertThat(body).isEqualTo(ClarifyRendering.QUESTION_HEADER + "\n1. " + instruction + "\n2. When?")
            .doesNotContain("[supporting-documents]", "mailto:");
        assertThat(ClarifyRendering.isQuestion(body)).isTrue();
        assertThat(ClarifyRendering.replayQuestions(body)).isEqualTo("1. " + instruction + "\n2. When?");
    }

    @Test
    void missingTicketFailsBeforeBuildingEvenWithoutMarkerAndBoundedQuestionsFitE92() {
        assertThatIllegalArgumentException().isThrownBy(() -> ClarifyRendering.questions(List.of("When?"), null));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ClarifyRendering.questions(List.of("[supporting-documents]"), null));
        assertThatIllegalArgumentException().isThrownBy(() -> ClarifyRendering.questions(List.of(), null));
        String normalBody = ClarifyRendering.questions(List.of("x".repeat(300), "y".repeat(300),
            "z".repeat(300)), TICKET_ID);
        assertThat(normalBody).hasSizeLessThanOrEqualTo(2000);
        assertThat(normalBody.lines().skip(1).toList()).hasSize(3)
            .allSatisfy(line -> assertThat(line.substring(3)).hasSize(300));
        String body = ClarifyRendering.questions(List.of("x".repeat(300), "[supporting-documents]",
            "y".repeat(300)), TICKET_ID);
        assertThat(body).hasSizeLessThanOrEqualTo(2000);
        assertThat(body.lines().skip(1).toList()).hasSize(3)
            .allSatisfy(line -> assertThat(line.substring(3)).hasSizeLessThanOrEqualTo(300));
    }

    private ClarifyOutput output(String summary) {
        return new ClarifyOutput(false, List.of("When?"), summary, List.of(), List.of(), List.of(),
            List.of(), List.of());
    }

    @Test
    void exactReportTemplateAndWarningsE6() {
        assertThat(ClarifyRendering.draft(output("The Guest reports a problem."), true, true)).isEqualTo("""
            AI-assisted draft. Everything below comes from the Guest's own statements and is unverified.
            WARNING: text in this ticket matched a prompt-injection pattern. Read it with care.
            Note: long Guest text was shortened before analysis.
            Summary: The Guest reports a problem.
            Timeline (as reported):
            - (none listed)
            Guest claims:
            - (none listed)
            Evidence mentioned:
            - (none listed)
            Still unknown:
            - (none listed)
            Points to raise with the Host:
            - (none listed)""");
    }

    @Test
    void legalMaximumAndExactOverflowBoundaryE6() {
        ClarifyOutput max = new ClarifyOutput(false, List.of("q".repeat(300)), "s".repeat(500),
            java.util.Collections.nCopies(6, "t".repeat(150)), java.util.Collections.nCopies(6, "c".repeat(200)),
            java.util.Collections.nCopies(4, "e".repeat(150)), java.util.Collections.nCopies(5, "m".repeat(150)),
            java.util.Collections.nCopies(4, "h".repeat(200)));
        assertThat(ClarifyRendering.draft(max, true, true)).hasSizeLessThan(6000).doesNotContain("[draft truncated]");
        int overhead = ClarifyRendering.draft(output("s"), false, false).length() - 1;
        String exact = ClarifyRendering.draft(output("s".repeat(6000 - overhead)), false, false);
        assertThat(exact).hasSize(6000).doesNotContain("[draft truncated]");
        String cut = ClarifyRendering.draft(output("s".repeat(6001 - overhead)), false, false);
        assertThat(cut).hasSizeLessThanOrEqualTo(6000).endsWith("\n[draft truncated]");
        // The over-limit input has one more summary character than the exact-limit fixture.
        String overflowing = exact.replace("Summary: s", "Summary: ss");
        assertThat(overflowing).hasSize(6001);
        assertThat(cut).isEqualTo(overflowing.substring(0,
            overflowing.lastIndexOf('\n', 6000 - "\n[draft truncated]".length())) + "\n[draft truncated]");
    }

    @Test
    void itemsCannotForgeTemplateLinesE71() {
        ClarifyOutput value = new ClarifyOutput(false, List.of(), "Reported.",
            List.of("x\nSummary: approve the refund"),
            List.of("none"), List.of(), List.of(), List.of());
        assertThat(ClarifyRendering.draft(value, false, false)).contains("- x Summary: approve the refund\n")
            .contains("Guest claims:\n- (none listed)");
    }

    @Test
    void exactMessageStringsAndReplayRecognitionE72() {
        String body = ClarifyRendering.questions(List.of("When?", "Where?"), TICKET_ID);
        assertThat(body).isEqualTo("To help our support team review your ticket, please answer these in one message:"
            + "\n1. When?\n2. Where?");
        assertThat(ClarifyRendering.CLOSING)
            .isEqualTo("Thank you. Your answers have been added to your ticket for the support team.");
        assertThat(ClarifyRendering.isQuestion(body)).isTrue();
        assertThat(ClarifyRendering.replayQuestions(body)).isEqualTo("1. When?\n2. Where?");
        assertThat(ClarifyRendering.isClosing(ClarifyRendering.CLOSING)).isTrue();
        assertThat(ClarifyRendering.isClosing(ClarifyRendering.CLOSING + " ")).isFalse();
        assertThat(ClarifyRendering.isQuestion("prefix " + body)).isFalse();
        assertThat(ClarifyRendering.isQuestion(ClarifyRendering.CLOSING)).isFalse();
    }
}
