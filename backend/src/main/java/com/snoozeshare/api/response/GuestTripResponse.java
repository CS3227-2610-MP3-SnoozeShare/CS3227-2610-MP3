package com.snoozeshare.api.response;

public record GuestTripResponse(
        BookingResponse booking,
        String listingTitle,
        String hostDisplayName,
        boolean canCancel,
        boolean canFileDispute,
        boolean hasReview) {
}
