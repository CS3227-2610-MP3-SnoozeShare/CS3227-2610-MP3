package com.snoozeshare.ai.host;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class ListingDescriptionContractTest {

    @Test
    void exposesFixedOptionsAndWordBands() {
        assertThat(ListingDescriptionContract.WritingStyle.values())
            .containsExactly(ListingDescriptionContract.WritingStyle.FRIENDLY,
                ListingDescriptionContract.WritingStyle.PROFESSIONAL,
                ListingDescriptionContract.WritingStyle.PERSUASIVE);
        assertThat(ListingDescriptionContract.TargetAudience.values())
            .containsExactly(ListingDescriptionContract.TargetAudience.FAMILIES,
                ListingDescriptionContract.TargetAudience.COUPLES,
                ListingDescriptionContract.TargetAudience.BUSINESS_TRAVELLERS,
                ListingDescriptionContract.TargetAudience.ALL_TRAVELLERS);
        assertThat(ListingDescriptionContract.DescriptionLength.SHORT.range()).isEqualTo("50-75");
        assertThat(ListingDescriptionContract.DescriptionLength.MEDIUM.range()).isEqualTo("75-125");
        assertThat(ListingDescriptionContract.DescriptionLength.LONG.range()).isEqualTo("125-200");
        assertThat(ListingDescriptionContract.DescriptionLength.SHORT.targetWordCount()).isEqualTo(60);
        assertThat(ListingDescriptionContract.DescriptionLength.MEDIUM.targetWordCount()).isEqualTo(100);
        assertThat(ListingDescriptionContract.DescriptionLength.LONG.targetWordCount()).isEqualTo(160);
    }

    @Test
    void countsUnicodeWordsWithInternalJoins() {
        assertThat(ListingDescriptionContract.wordCount("A lakeside café's quiet, family-friendly stay."))
            .isEqualTo(6);
        assertThat(ListingDescriptionContract.wordCount("   ")).isZero();
    }

    @Test
    void rejectsOversizedContextBeforeGateway() {
        assertThatThrownBy(() -> ListingDescriptionContract.requireContextWithinLimit("x".repeat(8_001)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesFeedbackAndSelectedWordBand() {
        ListingDescriptionContract.requireFeedback("Clear and specific.");
        assertThatThrownBy(() -> ListingDescriptionContract.requireFeedback("x".repeat(241)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ListingDescriptionContract.requireWordBand(
            "one two", ListingDescriptionContract.DescriptionLength.SHORT))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsTheRequestedInclusiveGenerateBoundaries() {
        String seventyFiveWords = "word ".repeat(75).trim();
        String oneHundredTwentyFiveWords = "word ".repeat(125).trim();

        ListingDescriptionContract.requireWordBand(
            seventyFiveWords, ListingDescriptionContract.DescriptionLength.SHORT);
        ListingDescriptionContract.requireWordBand(
            seventyFiveWords, ListingDescriptionContract.DescriptionLength.MEDIUM);
        ListingDescriptionContract.requireWordBand(
            oneHundredTwentyFiveWords, ListingDescriptionContract.DescriptionLength.MEDIUM);
        ListingDescriptionContract.requireWordBand(
            oneHundredTwentyFiveWords, ListingDescriptionContract.DescriptionLength.LONG);
    }

    @Test
    void serializesOnlyAddressFreeFactsAndNormalizesAmenities() {
        String context = ListingDescriptionContract.serializeContext(
            "GENERATE", "FRIENDLY", "ALL_TRAVELLERS", "MEDIUM", "A description",
            new ListingDescriptionContract.ListingFacts("Cabin", "HOUSE", "Reno", "NV", 2, 1, 1,
                List.of("WIFI", "WIFI"), "15:00", "11:00"));

        assertThat(context).contains("Cabin", "Reno", "WIFI", "\"targetWordCount\":100");
        assertThat(context).doesNotContain("streetAddress", "postalCode");
    }
}
