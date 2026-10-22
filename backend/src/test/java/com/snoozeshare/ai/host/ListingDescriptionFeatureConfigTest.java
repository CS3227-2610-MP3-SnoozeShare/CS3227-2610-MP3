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
    void promptDefinesFixedLengthGuidanceAndFocus() {
        assertThat(feature.systemPrompt())
            .contains("50-150 words")
            .contains("Balanced", "Comfort", "Practicality")
            .contains("living space, comfort, and available facilities")
            .contains("functional features and suitability for everyday needs")
            .contains("Do not add filler just to reach")
            .doesNotContain("SHORT is 50-75 words", "MEDIUM is 75-125 words", "selected band");
    }

    @Test
    void promptMakesTheThreeCriteriaGenerationTargets() {
        assertThat(feature.systemPrompt())
            .contains("self-review and revise the generated description")
            .contains("Try to make all three criteria PASS")
            .contains("Do not sacrifice accuracy to")
            .contains("generatedAnalysis must evaluate");
    }
}
