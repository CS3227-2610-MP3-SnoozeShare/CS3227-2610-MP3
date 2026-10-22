package com.snoozeshare.api.request;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.snoozeshare.ai.host.ListingDescriptionContract.DescriptionLength;
import com.snoozeshare.ai.host.ListingDescriptionContract.ListingFacts;
import com.snoozeshare.ai.host.ListingDescriptionContract.Mode;
import com.snoozeshare.ai.host.ListingDescriptionContract.TargetAudience;
import com.snoozeshare.ai.host.ListingDescriptionContract.WritingStyle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Host draft context and fixed preferences for one listing-description assistant call. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record HostAiListingDescriptionRequest(
        UUID listingId,
        @NotNull Mode mode,
        @NotNull WritingStyle writingStyle,
        @NotNull TargetAudience targetAudience,
        @NotNull DescriptionLength length,
        @NotNull @Size(max = 5_000) String currentDescription,
        @NotNull @Valid ListingFacts listing) {
}
