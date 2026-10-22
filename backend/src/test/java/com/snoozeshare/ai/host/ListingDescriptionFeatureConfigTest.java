package com.snoozeshare.ai.host;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.snoozeshare.ai.AiFeature;
import com.snoozeshare.api.response.ListingDescriptionModelResponse;

class ListingDescriptionFeatureConfigTest {

    private final AiFeature<ListingDescriptionModelResponse> feature =
        new ListingDescriptionFeatureConfig().hostListingDescriptionFeature();

    @Test
    void promptDefinesBothModesAndTheExactJsonContract() {
        assertThat(feature.systemPrompt())
            .contains("EVALUATE", "GENERATE")
            .contains("currentAnalysis", "generatedDescription", "generatedAnalysis")
            .contains("clarity", "completeness", "accuracy")
            .contains("PASS", "IMPROVE")
            .contains("Return only valid JSON");
    }

    @Test
    void promptRequiresEvaluateToAvoidGeneratedContent() {
        assertThat(feature.systemPrompt())
            .contains("generatedDescription and generatedAnalysis as null")
            .contains("For EVALUATE, generatedDescription and generatedAnalysis must be null");
    }

    @Test
    void promptDefinesTheSelectedDescriptionLengthGuidance() {
        assertThat(feature.systemPrompt())
            .contains("SHORT is 50-75 words", "MEDIUM is 75-125 words", "LONG is 125-200 words")
            .contains("descriptionLength is authoritative", "Count the words before returning");
    }
}
