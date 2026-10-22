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
        assertThat(ListingDescriptionContract.DescriptionLength.SHORT.range()).isEqualTo("50-74");
        assertThat(ListingDescriptionContract.DescriptionLength.MEDIUM.range()).isEqualTo("75-124");
        assertThat(ListingDescriptionContract.DescriptionLength.LONG.range()).isEqualTo("125-200");
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
    void serializesOnlyAddressFreeFactsAndNormalizesAmenities() {
        String context = ListingDescriptionContract.serializeContext(
            "GENERATE", "FRIENDLY", "ALL_TRAVELLERS", "MEDIUM", "A description",
            new ListingDescriptionContract.ListingFacts("Cabin", "HOUSE", "Reno", "NV", 2, 1, 1,
                List.of("WIFI", "WIFI"), "15:00", "11:00"));

        assertThat(context).contains("Cabin", "Reno", "WIFI");
        assertThat(context).doesNotContain("streetAddress", "postalCode");
    }
}
