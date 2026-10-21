package com.snoozeshare.service;

import java.util.UUID;

import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.domain.model.Message;

public record ConversationSummary(
        UUID ticketId,
        ThreadChannel channel,
        String ticketLabel,
        String ticketTitle,
        String listingTitle,
        String counterpartName,
        Message lastMessage,
        int unreadCount,
        boolean open
) {
}
