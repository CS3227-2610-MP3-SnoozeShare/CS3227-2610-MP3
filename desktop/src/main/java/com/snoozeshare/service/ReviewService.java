package com.snoozeshare.service;

import java.util.UUID;

import com.snoozeshare.domain.model.Review;

/**
 * Guest reviews of completed stays. One review per booking.
 */
public interface ReviewService {
    /**
     * @param rating whole stars, 1 to 5
     * @throws IllegalArgumentException if the booking does not exist, the user is not its guest, or
     *         the rating is out of range
     * @throws IllegalStateException if the stay is not completed or a review already exists
     */
    Review submit(UUID bookingId, UUID guestId, int rating, String comment);

    /**
     * @return whether the booking already has a review
     */
    boolean hasReview(UUID bookingId);
}
