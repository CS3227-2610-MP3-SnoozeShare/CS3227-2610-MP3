package com.snoozeshare.api.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.snoozeshare.api.mapper.AssistantMapper;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.service.DisputeDetail;

public record DisputeDetailResponse(
        TicketResponse ticket,
        String ticketLabel,
        String listingTitle,
        LocalDate startDate,
        LocalDate endDate,
        String guestName,
        String hostName,
        String raisedByName,
        String assignedAgentName,
        BookingStatus bookingStatus,
        BigDecimal escrowAmount,
        boolean escrowHeld,
        String phaseLabel,
        String agentNotes,
        AssistantResponse assistant) {

    public static DisputeDetailResponse from(DisputeDetail d) {
        return new DisputeDetailResponse(
                TicketResponse.from(d.ticket()), d.ticketLabel(),
                d.listingTitle(), d.startDate(), d.endDate(),
                d.guestName(), d.hostName(), d.raisedByName(),
                d.assignedAgentName(), d.bookingStatus(),
                d.escrowAmount(), d.escrowHeld(), d.phaseLabel(),
                d.ticket().getAgentNotes(), AssistantMapper.from(d.assistant()));
    }
}
