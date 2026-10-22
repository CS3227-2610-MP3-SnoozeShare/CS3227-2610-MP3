package com.snoozeshare.api.response;

import java.util.List;

public record GuestConversationListResponse(
        List<ConversationSummaryResponse> ticketConversations,
        List<BookingConversationSummaryResponse> bookingConversations) {
}
