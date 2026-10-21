package com.snoozeshare.service;

import java.util.UUID;

/**
 * Read-only statistics shown on the host's listing detail page.
 */
public interface ListingMetricsService {

    ListingMetrics metricsFor(UUID propertyId);
}
