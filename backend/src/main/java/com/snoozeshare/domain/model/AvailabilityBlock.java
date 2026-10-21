package com.snoozeshare.domain.model;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


/** A blocked date range on a property (either a manual host block or a booking hold). */
@Entity
@Table(name = "availability_blocks")
public class AvailabilityBlock {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String source;

    @Column(name = "booking_id")
    private UUID bookingId;

    private String reason;

    /** JPA requires a no-arg constructor. */
    protected AvailabilityBlock() {}

    /** Creates a new availability block. */
    public AvailabilityBlock(UUID id, UUID propertyId,
                             LocalDate startDate, LocalDate endDate,
                             String source, UUID bookingId,
                             String reason) {
        this.id = id;
        this.propertyId = propertyId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.source = source;
        this.bookingId = bookingId;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPropertyId() {
        return propertyId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getSource() {
        return source;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public String getReason() {
        return reason;
    }
}
