package com.snoozeshare.api.response;

import java.time.LocalDate;
import java.util.UUID;

import com.snoozeshare.service.BookingConversationSummary;

public record BookingConversationSummaryResponse(
        UUID bookingId,
        String listingTitle,
        LocalDate startDate,
        LocalDate endDate,
        String counterpartName,
        MessageResponse lastMessage,
        int unreadCount,
        boolean open) {

    public static BookingConversationSummaryResponse from(BookingConversationSummary c) {
        return new BookingConversationSummaryResponse(c.bookingId(), c.listingTitle(),
                c.startDate(), c.endDate(), c.counterpartName(),
                c.lastMessage() != null ? MessageResponse.from(c.lastMessage()) : null,
                c.unreadCount(), c.open());
    }
}
