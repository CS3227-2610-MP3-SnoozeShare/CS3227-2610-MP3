package com.snoozeshare.api.response;

import java.util.List;

import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.service.ListingMetrics;

public record PropertyDetailResponse(
        PropertyResponse property,
        String hostDisplayName,
        int hostingSinceYear,
        List<ListingMetricsResponse.ListingReviewResponse> reviews,
        double averageRating,
        int reviewCount) {

    public static PropertyDetailResponse from(
            Property property, User host, ListingMetrics metrics) {
        return new PropertyDetailResponse(
                PropertyResponse.from(property),
                host.getDisplayName(),
                host.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).getYear(),
                metrics.reviews().stream()
                        .map(ListingMetricsResponse.ListingReviewResponse::from)
                        .toList(),
                metrics.averageRating(),
                metrics.reviews().size());
    }
}
