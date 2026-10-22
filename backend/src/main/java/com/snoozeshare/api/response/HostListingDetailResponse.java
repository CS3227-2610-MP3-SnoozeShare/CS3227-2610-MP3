package com.snoozeshare.api.response;

/** A Host listing detail with the metrics already shown by the Host UI. */
public record HostListingDetailResponse(PropertyResponse listing, ListingMetricsResponse metrics) { }
