package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.snoozeshare.domain.enums.ListingStatus;

import jakarta.validation.constraints.NotNull;

/** Change listing status (activate/deactivate). */
@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateListingStatusRequest(@NotNull ListingStatus status) {
}
