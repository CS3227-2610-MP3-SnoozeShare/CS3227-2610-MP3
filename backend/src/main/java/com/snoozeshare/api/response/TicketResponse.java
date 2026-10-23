package com.snoozeshare.api.response;

import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.model.Ticket;

public record TicketResponse(
        UUID id,
        UUID bookingId,
        UUID raisedByUserId,
        Role raisedByRole,
        String category,
        String title,
        String description,
        RemedyType requestedRemedy,
        String supportingText,
        TicketStatus status,
        UUID assignedAgentId,
        String resolutionReason,
        Instant createdAt,
        Instant resolvedAt) {

    public static TicketResponse from(Ticket t) {
        return new TicketResponse(t.getId(), t.getBookingId(), t.getRaisedByUserId(),
                t.getRaisedByRole(), t.getCategory(), t.getTitle(), t.getDescription(),
                t.getRequestedRemedy(), t.getSupportingText(), t.getStatus(),
                t.getAssignedAgentId(), t.getResolutionReason(),
                t.getCreatedAt(), t.getResolvedAt());
    }
}
