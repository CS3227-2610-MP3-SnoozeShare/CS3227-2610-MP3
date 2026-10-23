package com.snoozeshare.api.response;

import java.util.UUID;

import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.service.ConversationSummary;

public record ConversationSummaryResponse(
        UUID ticketId,
        ThreadChannel channel,
        String ticketLabel,
        String ticketTitle,
        String listingTitle,
        String counterpartName,
        MessageResponse lastMessage,
        int unreadCount,
        boolean open) {

    public static ConversationSummaryResponse from(ConversationSummary c) {
        return new ConversationSummaryResponse(c.ticketId(), c.channel(),
                c.ticketLabel(), c.ticketTitle(), c.listingTitle(),
                c.counterpartName(),
                c.lastMessage() != null ? MessageResponse.from(c.lastMessage()) : null,
                c.unreadCount(), c.open());
    }
}
