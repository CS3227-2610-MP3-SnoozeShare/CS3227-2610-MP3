package com.snoozeshare.api.response;

import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.service.DisputeSummary;

public record DisputeSummaryResponse(
        UUID ticketId,
        String ticketLabel,
        String title,
        String category,
        String listingTitle,
        String guestName,
        String hostName,
        String assignedAgentName,
        TicketStatus status,
        Instant createdAt) {

    public static DisputeSummaryResponse from(DisputeSummary d) {
        return new DisputeSummaryResponse(d.ticketId(), d.ticketLabel(), d.title(),
                d.category(), d.listingTitle(), d.guestName(), d.hostName(),
                d.assignedAgentName(), d.status(), d.createdAt());
    }
}
