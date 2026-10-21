package com.snoozeshare.service;

import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.domain.model.Message;

/**
 * Ticket chat. Each ticket has a GUEST thread and a HOST thread.
 */
public interface MessageService {

    List<Message> thread(UUID ticketId, ThreadChannel channel, UUID viewerId, Role viewerRole);

    List<Message> thread(UUID ticketId, ThreadChannel channel, UUID viewerId, Role viewerRole,
                         int page, int size);

    Message post(UUID ticketId, ThreadChannel channel, UUID authorId, Role authorRole, String body);

    List<ConversationSummary> conversationsFor(UUID userId, Role role);

    List<ConversationSummary> conversationsFor(UUID userId, Role role, int page, int size);

    int unreadCount(UUID userId, Role role);

    void markRead(UUID ticketId, ThreadChannel channel, UUID userId, Role role);
}
