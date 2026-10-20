package com.snoozeshare.domain.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A guest review of a completed stay. */
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @Column(name = "review_id")
    private UUID reviewId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "guest_id", nullable = false)
    private UUID guestId;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** JPA requires a no-arg constructor. */
    protected Review() {}

    /** Creates a new review. */
    public Review(UUID reviewId, UUID bookingId, UUID guestId,
                  int rating, String comment, Instant createdAt) {
        this.reviewId = reviewId;
        this.bookingId = bookingId;
        this.guestId = guestId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public UUID getReviewId() {
        return reviewId;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getGuestId() {
        return guestId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
