package com.snoozeshare.service;

import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.BookingMessage;

/**
 * Private host and guest chat on a booking.
 */
public interface BookingConversationService {

    List<BookingMessage> thread(UUID bookingId, UUID viewerId, Role viewerRole);

    List<BookingMessage> thread(UUID bookingId, UUID viewerId, Role viewerRole,
                                int page, int size);

    BookingMessage post(UUID bookingId, UUID authorId, Role authorRole, String body);

    List<BookingConversationSummary> conversationsFor(UUID userId, Role role);

    List<BookingConversationSummary> conversationsFor(UUID userId, Role role,
                                                       int page, int size);

    int unreadCount(UUID userId, Role role);

    void markRead(UUID bookingId, UUID userId, Role role);
}
