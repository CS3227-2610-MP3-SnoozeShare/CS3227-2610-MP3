package com.snoozeshare.api.response;

import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.model.Review;

public record ReviewResponse(
        UUID id,
        UUID bookingId,
        UUID guestId,
        int rating,
        String comment,
        Instant createdAt) {

    public static ReviewResponse from(Review r) {
        return new ReviewResponse(r.getId(), r.getBookingId(), r.getGuestId(),
                r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
