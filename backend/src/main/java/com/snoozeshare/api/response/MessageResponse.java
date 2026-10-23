package com.snoozeshare.api.response;

import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.BookingMessage;
import com.snoozeshare.domain.model.Message;

public record MessageResponse(
        UUID id,
        UUID authorId,
        Role authorRole,
        String body,
        Instant sentAt) {

    public static MessageResponse from(Message m) {
        return new MessageResponse(m.getId(), m.getAuthorId(), m.getAuthorRole(),
                m.getBody(), m.getSentAt());
    }

    public static MessageResponse from(BookingMessage m) {
        return new MessageResponse(m.getId(), m.getAuthorId(), m.getAuthorRole(),
                m.getBody(), m.getSentAt());
    }
}
