package com.snoozeshare.api.response;

import com.snoozeshare.ai.host.ListingDescriptionContract.Mode;
import com.snoozeshare.ai.host.ListingDescriptionContract.Outcome;

/** HTTP response for one Host listing-description assistant operation. */
public record HostAiListingDescriptionResponse(
        Outcome outcome,
        Mode mode,
        ListingDescriptionAnalysisResponse currentAnalysis,
        String generatedDescription,
        ListingDescriptionAnalysisResponse generatedAnalysis,
        String gatewayCode,
        String requestId) {

    public static HostAiListingDescriptionResponse from(
            ListingDescriptionModelResponse model, String gatewayCode, String requestId) {
        return new HostAiListingDescriptionResponse(model.outcome(), model.mode(), model.currentAnalysis(),
            model.generatedDescription(), model.generatedAnalysis(), gatewayCode, requestId);
    }

    public static HostAiListingDescriptionResponse degraded(
            Mode mode, String gatewayCode, String requestId) {
        return new HostAiListingDescriptionResponse(Outcome.DEGRADED, mode, null, null, null,
            gatewayCode, requestId);
    }
}
