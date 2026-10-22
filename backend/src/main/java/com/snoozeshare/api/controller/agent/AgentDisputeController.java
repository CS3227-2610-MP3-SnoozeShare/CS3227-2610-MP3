package com.snoozeshare.api.controller.agent;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.CurrentUserId;
import com.snoozeshare.api.request.SaveNotesRequest;
import com.snoozeshare.api.request.SettleDisputeRequest;
import com.snoozeshare.api.response.DisputeDetailResponse;
import com.snoozeshare.api.response.DisputeQueueResponse;
import com.snoozeshare.api.response.DisputeSummaryResponse;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.AssigneeFilter;
import com.snoozeshare.service.DisputeQueryService;
import com.snoozeshare.service.TicketService;
import com.snoozeshare.service.requests.ResolutionRequest;

import jakarta.validation.Valid;

/** Dispute queue, detail and the agent's actions on one ticket. */
@RestController
@RequestMapping("/api/agent/disputes")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
@ConditionalOnDatabase
public class AgentDisputeController {

    private final DisputeQueryService queries;
    private final TicketService tickets;

    public AgentDisputeController(DisputeQueryService queries, TicketService tickets) {
        this.queries = queries;
        this.tickets = tickets;
    }

    @GetMapping
    public DisputeQueueResponse queue(
            @RequestParam(defaultValue = "ALL") AssigneeFilter assignee,
            @CurrentUserId UUID agentId) {
        var items = queries.queue(null, assignee, agentId).stream()
                .map(DisputeSummaryResponse::from).toList();
        return new DisputeQueueResponse(items, queries.unassignedCount());
    }

    @GetMapping("/{ticketId}")
    public DisputeDetailResponse detail(@PathVariable UUID ticketId) {
        return DisputeDetailResponse.from(queries.detail(ticketId));
    }

    @PutMapping("/{ticketId}/assign")
    public DisputeDetailResponse assign(@PathVariable UUID ticketId, @CurrentUserId UUID agentId) {
        tickets.assignToMe(ticketId, agentId);
        return DisputeDetailResponse.from(queries.detail(ticketId));
    }

    @PutMapping("/{ticketId}/resolve")
    public DisputeDetailResponse resolve(@PathVariable UUID ticketId, @Valid @RequestBody SettleDisputeRequest request,
                                         @CurrentUserId UUID agentId) {
        tickets.resolve(ticketId, new ResolutionRequest(request.mode(), request.guestRefund(), request.reason()),
                agentId);
        return DisputeDetailResponse.from(queries.detail(ticketId));
    }

    @PutMapping("/{ticketId}/notes")
    public DisputeDetailResponse notes(@PathVariable UUID ticketId, @Valid @RequestBody SaveNotesRequest request,
                                       @CurrentUserId UUID agentId) {
        tickets.saveNotes(ticketId, request.notes(), agentId);
        return DisputeDetailResponse.from(queries.detail(ticketId));
    }
}
