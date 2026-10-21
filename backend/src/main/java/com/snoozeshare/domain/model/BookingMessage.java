package com.snoozeshare.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.enums.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A private message between guest and host on a booking. */
@Entity
@Table(name = "booking_messages")
public class BookingMessage {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_role", nullable = false)
    private Role authorRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    /** JPA requires a no-arg constructor. */
    protected BookingMessage() {}

    /** Creates a new booking message. */
    public BookingMessage(UUID id, UUID bookingId,
                          UUID authorId, Role authorRole,
                          String body, Instant sentAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.authorId = authorId;
        this.authorRole = authorRole;
        this.body = body;
        this.sentAt = sentAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public Role getAuthorRole() {
        return authorRole;
    }

    public String getBody() {
        return body;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
