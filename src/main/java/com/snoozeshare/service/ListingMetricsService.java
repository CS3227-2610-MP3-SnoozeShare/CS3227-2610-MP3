package com.snoozeshare.service;

import java.util.UUID;

/**
 * Read-only statistics shown on the host's listing detail page.
 */
public interface ListingMetricsService {
    /**
     * Counts all bookings, averages the ratings, and computes occupancy and host earnings over the last
     * 30 days.
     *
     * @return the metrics; zeroes for a listing with no history
     */
    ListingMetrics metricsFor(UUID propertyId);
}
