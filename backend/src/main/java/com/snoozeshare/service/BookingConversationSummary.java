package com.snoozeshare.service;

import java.time.LocalDate;
import java.util.UUID;

import com.snoozeshare.domain.model.BookingMessage;

public record BookingConversationSummary(
        UUID bookingId,
        String listingTitle,
        LocalDate startDate,
        LocalDate endDate,
        String counterpartName,
        BookingMessage lastMessage,
        int unreadCount,
        boolean open
) {
}
