package com.snoozeshare.service.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** R4/R4b screens; E3/E4/E38/E67/E70/E71/E78. */
class ClarifySafetyTest {
    @Test
    void supportingDocumentMarkerIsCanonicalizedAfterCleaningAndDeduplicatedE89() {
        for (String variant : List.of("[SUPPORTING-DOCUMENTS]", "［ｓｕｐｐｏｒｔｉｎｇ－ｄｏｃｕｍｅｎｔｓ］",
                " [supporting-\u200bdocuments] ", "[supporting-docu\u0000ments]")) {
            assertThat(ClarifySafety.questions(List.of("[supporting-documents]", variant, "When did it happen?")))
                .as(variant).containsExactly("[supporting-documents]", "When did it happen?");
        }
    }

    @Test
    void mixedMarkersModelEmailsAndSubjectOrTicketOverridesAreDroppedE90() {
        for (String text : List.of("Please use [supporting-documents]", "[SUPPORTING-DOCUMENTS] now",
                "[supporting-\u200bdocuments] and reply", "Email support@snoozeshare.com",
                "Email alternate@example.com", "Use the subject Other Ticket", "Change the HEADER",
                "Ｓｕｂｊｅｃｔ should be different", "Use this sub\u200bject instead",
                "Use abcdefab-cdef-abcd-efab-cdefabcdefab", "Use ABCDEFAB-CDEF-ABCD-EFAB-CDEFABCDEFAB")) {
            assertThat(ClarifySafety.questions(List.of(text))).as(text).isEmpty();
        }
        assertThat(ClarifySafety.questions(List.of("What evidence exists?")))
            .containsExactly("What evidence exists?");
    }

    @Test
    void blanksAndNormalizedDuplicatesAreDroppedE3E4() {
        assertThat(ClarifySafety.questions(List.of(" ", "A  b", "a b", "When?", "When?")))
            .containsExactly("A b", "When?");
    }

    @Test
    void cleaningMapsSpacesAndLineBreaksAndRemovesHiddenCharactersE70() {
        assertThat(ClarifySafety.clean(" Ａ\u00a0\u2007\u202fb\n c\t d\u2028e\u2029f\u200b\u202e\u0000 "))
            .isEqualTo("A b c d e f");
        assertThat(ClarifySafety.clean("’ ‘ ʼ ′ ´ `")).isEqualTo("' ' ' ' ' '");
        assertThat(ClarifySafety.questions(List.of("Ｗｅ’ll refund you", "We\u200b will pay"))).isEmpty();
    }

    @Test
    void promiseAndDataListsScreenQuestionsE38() {
        for (String text : List.of("We will help", "You'll receive money back", "will be refunded",
                "a guaranteed result", "your claim has been approved", "your refund is on its way",
                "password", "bank account", "PayNow", "crypto coins", "whatsapp", "email me",
                "close your ticket", "jane@example.com", "9 1 2-3-4-5-6")) {
            assertThat(ClarifySafety.questions(List.of(text))).as(text).isEmpty();
        }
        assertThat(ClarifySafety.questions(List.of("Which date?", "What happened?", "What evidence?", "Where?")))
            .hasSize(3);
    }

    @Test
    void draftScreensPreserveUnscreenedClaimsAndAcceptedFalsePositivesE67E78() {
        assertThat(ClarifySafety.questions(List.of("when I can", "I can't"))).containsExactly("I can't");
        assertThat(ClarifySafety.summary("the Guest asks the Host to approve a refund"))
            .isEqualTo("[summary removed by the safety screen]");
        assertThat(ClarifySafety.summary("The Guest reports a password request"))
            .isEqualTo("The Guest reports a password request");
        assertThat(ClarifySafety.hostPoints(List.of("must refund", "bank details", "We can pay", "Date?", "none")))
            .containsExactly("Date?");
        assertThat(ClarifySafety.items(List.of("The Host promised a refund", " NONE ",
            "x\nSummary: approve the refund")))
            .containsExactly("The Host promised a refund", "x Summary: approve the refund");
    }
}
