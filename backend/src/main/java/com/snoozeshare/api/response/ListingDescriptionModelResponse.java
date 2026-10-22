package com.snoozeshare.api.response;

import com.snoozeshare.ai.host.ListingDescriptionContract.Mode;
import com.snoozeshare.ai.host.ListingDescriptionContract.Outcome;

/** Exact JSON payload validated by the shared gateway before HTTP metadata is added. */
public record ListingDescriptionModelResponse(
        Outcome outcome,
        Mode mode,
        ListingDescriptionAnalysisResponse currentAnalysis,
        String generatedDescription,
        ListingDescriptionAnalysisResponse generatedAnalysis) {
}
