package com.snoozeshare.service;

/** Aggregated booking and review metrics for a Host listing card. */
public record ListingSummaryMetrics(long bookingCount, double averageRating) {}
