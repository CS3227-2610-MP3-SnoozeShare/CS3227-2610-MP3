package com.snoozeshare.ai.host;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class ListingDescriptionContractTest {

    @Test
    void exposesFixedOptionsAndDescriptionFocus() {
        assertThat(ListingDescriptionContract.WritingStyle.values())
            .containsExactly(ListingDescriptionContract.WritingStyle.FRIENDLY,
                ListingDescriptionContract.WritingStyle.PROFESSIONAL,
                ListingDescriptionContract.WritingStyle.PERSUASIVE);
        assertThat(ListingDescriptionContract.TargetAudience.values())
            .containsExactly(ListingDescriptionContract.TargetAudience.ALL_TRAVELLERS,
                ListingDescriptionContract.TargetAudience.FAMILIES,
                ListingDescriptionContract.TargetAudience.COUPLES,
                ListingDescriptionContract.TargetAudience.BUSINESS_TRAVELLERS);
        assertThat(ListingDescriptionContract.DescriptionFocus.values())
            .containsExactly(ListingDescriptionContract.DescriptionFocus.BALANCED,
                ListingDescriptionContract.DescriptionFocus.COMFORT,
                ListingDescriptionContract.DescriptionFocus.PRACTICALITY);
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
    void validatesFeedback() {
        ListingDescriptionContract.requireFeedback("Clear and specific.");
        assertThatThrownBy(() -> ListingDescriptionContract.requireFeedback("x".repeat(241)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void serializesOnlyAddressFreeFactsAndNormalizesAmenities() {
        String context = ListingDescriptionContract.serializeContext(
            "GENERATE", "FRIENDLY", "ALL_TRAVELLERS", "BALANCED", "A description",
            new ListingDescriptionContract.ListingFacts("Cabin", "HOUSE", "Reno", "NV", 2, 1, 1,
                List.of("WIFI", "WIFI"), "15:00", "11:00"));

        assertThat(context).contains("Cabin", "Reno", "WIFI");
        assertThat(context).contains("descriptionLength\":\"50-150\"");
        assertThat(context).contains("descriptionFocus\":\"BALANCED\"");
        assertThat(context).doesNotContain("targetWordCount", "SHORT", "MEDIUM", "LONG");
        assertThat(context).doesNotContain("streetAddress", "postalCode");
    }
}
