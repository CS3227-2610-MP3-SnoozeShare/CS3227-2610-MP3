package com.snoozeshare.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.snoozeshare.domain.enums.BookingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A booking for a property stay. */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "guest_id", nullable = false)
    private UUID guestId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "nightly_rate_snapshot", nullable = false,
            precision = 19, scale = 4)
    private BigDecimal nightlyRateSnapshot;

    @Column(name = "total_amount", nullable = false,
            precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "host_decision_message")
    private String hostDecisionMessage;

    /** JPA requires a no-arg constructor. */
    protected Booking() {}

    /** Creates a new booking. */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public Booking(UUID id, UUID listingId, UUID guestId,
                   LocalDate startDate, LocalDate endDate,
                   BookingStatus status,
                   BigDecimal nightlyRateSnapshot,
                   BigDecimal totalAmount, Instant createdAt,
                   Instant decidedAt, Instant completedAt,
                   String hostDecisionMessage) {
        this.id = id;
        this.listingId = listingId;
        this.guestId = guestId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.nightlyRateSnapshot = nightlyRateSnapshot;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.decidedAt = decidedAt;
        this.completedAt = completedAt;
        this.hostDecisionMessage = hostDecisionMessage;
    }

    public UUID getId() {
        return id;
    }

    public UUID getListingId() {
        return listingId;
    }

    public UUID getGuestId() {
        return guestId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public BigDecimal getNightlyRateSnapshot() {
        return nightlyRateSnapshot;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getHostDecisionMessage() {
        return hostDecisionMessage;
    }

    public void setHostDecisionMessage(String hostDecisionMessage) {
        this.hostDecisionMessage = hostDecisionMessage;
    }
}
