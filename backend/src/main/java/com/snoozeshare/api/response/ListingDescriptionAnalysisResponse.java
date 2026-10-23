package com.snoozeshare.api.response;

/** The exact three-row analysis shape shared by current and generated descriptions. */
public record ListingDescriptionAnalysisResponse(
        ListingDescriptionCriterionResponse clarity,
        ListingDescriptionCriterionResponse completeness,
        ListingDescriptionCriterionResponse accuracy) {
}
