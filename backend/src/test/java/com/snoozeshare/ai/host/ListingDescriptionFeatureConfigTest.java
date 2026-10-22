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
    void promptTargetsSafeWordCountsInsideEachRequestedBand() {
        assertThat(feature.systemPrompt())
            .contains("exactly 60 words", "exactly 100 words", "exactly 160 words")
            .contains("Count the words before returning")
            .contains("targetWordCount", "numeric targetWordCount");
    }
}
