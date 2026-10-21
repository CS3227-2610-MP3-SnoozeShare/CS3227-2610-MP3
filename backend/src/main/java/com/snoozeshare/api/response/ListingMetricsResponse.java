package com.snoozeshare.api.response;

import java.math.BigDecimal;
import java.util.List;

import com.snoozeshare.service.ListingMetrics;
import com.snoozeshare.service.ListingReview;

public record ListingMetricsResponse(
        int bookingCount,
        double averageRating,
        List<ListingReviewResponse> reviews,
        double occupancyPercentage,
        BigDecimal earnings) {

    public record ListingReviewResponse(String guestName, int rating, String comment) {

        public static ListingReviewResponse from(ListingReview r) {
            return new ListingReviewResponse(r.guestName(), r.rating(), r.comment());
        }
    }

    public static ListingMetricsResponse from(ListingMetrics m) {
        return new ListingMetricsResponse(m.bookingCount(), m.averageRating(),
                m.reviews().stream().map(ListingReviewResponse::from).toList(),
                m.occupancyPercentage(), m.earnings());
    }
}
