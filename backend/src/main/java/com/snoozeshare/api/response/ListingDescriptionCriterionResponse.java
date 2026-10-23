package com.snoozeshare.api.response;

import com.snoozeshare.ai.host.ListingDescriptionContract.Status;

/** One concise analysis result for a listing-description criterion. */
public record ListingDescriptionCriterionResponse(Status status, String feedback) {
}
