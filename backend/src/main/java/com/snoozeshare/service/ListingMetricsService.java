package com.snoozeshare.service;

import java.util.Map;
import java.util.UUID;

/**
 * Read-only statistics shown on the host's listing detail page.
 */
public interface ListingMetricsService {

    ListingMetrics metricsFor(UUID propertyId);

    /** Returns batched card metrics for every property owned by the authenticated Host. */
    Map<UUID, ListingSummaryMetrics> summaryMetricsForHost(UUID hostId);
}
