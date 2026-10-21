package com.snoozeshare.api.response;

import java.math.BigDecimal;

import com.snoozeshare.service.HostBookingRow;

public record HostBookingRowResponse(
        BookingResponse booking,
        String guestDisplayName,
        String listingTitle,
        long nights,
        BigDecimal grossAmount,
        BigDecimal projectedNetAmount,
        Double guestAverageRating) {

    public static HostBookingRowResponse from(HostBookingRow r) {
        return new HostBookingRowResponse(
                BookingResponse.from(r.booking()),
                r.guestDisplayName(), r.listingTitle(), r.nights(),
                r.grossAmount(), r.projectedNetAmount(),
                r.guestAverageRating().isPresent()
                        ? r.guestAverageRating().getAsDouble() : null);
    }
}
