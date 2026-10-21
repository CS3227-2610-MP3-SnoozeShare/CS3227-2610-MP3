package com.snoozeshare.api.response;

import java.time.LocalDate;
import java.util.UUID;

import com.snoozeshare.domain.model.AvailabilityBlock;

public record AvailabilityBlockResponse(
        UUID id,
        UUID propertyId,
        LocalDate startDate,
        LocalDate endDate,
        String source,
        UUID bookingId,
        String reason) {

    public static AvailabilityBlockResponse from(AvailabilityBlock b) {
        return new AvailabilityBlockResponse(b.getId(), b.getPropertyId(),
                b.getStartDate(), b.getEndDate(), b.getSource(),
                b.getBookingId(), b.getReason());
    }
}
