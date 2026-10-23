package com.snoozeshare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.snoozeshare.domain.model.Review;

/** Spring Data JPA repository for guest reviews. */
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByBookingId(UUID bookingId);

    List<Review> findByGuestId(UUID guestId);
}
