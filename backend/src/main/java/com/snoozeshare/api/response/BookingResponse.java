package com.snoozeshare.api.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.model.Booking;

public record BookingResponse(
        UUID id,
        UUID listingId,
        UUID guestId,
        LocalDate startDate,
        LocalDate endDate,
        BookingStatus status,
        BigDecimal nightlyRateSnapshot,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant decidedAt,
        Instant completedAt,
        String hostDecisionMessage) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getListingId(), b.getGuestId(),
                b.getStartDate(), b.getEndDate(), b.getStatus(),
                b.getNightlyRateSnapshot(), b.getTotalAmount(),
                b.getCreatedAt(), b.getDecidedAt(), b.getCompletedAt(),
                b.getHostDecisionMessage());
    }
}
