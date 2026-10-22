package com.snoozeshare.api.controller.agent;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.CurrentUserId;
import com.snoozeshare.api.response.DisputeQueueResponse;
import com.snoozeshare.api.response.DisputeSummaryResponse;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.AssigneeFilter;
import com.snoozeshare.service.DisputeQueryService;

/** Dispute queue, detail and the agent's actions on one ticket. */
@RestController
@RequestMapping("/api/agent/disputes")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
@ConditionalOnDatabase
public class AgentDisputeController {

    private final DisputeQueryService queries;

    public AgentDisputeController(DisputeQueryService queries) {
        this.queries = queries;
    }

    @GetMapping
    public DisputeQueueResponse queue(
            @RequestParam(defaultValue = "ALL") AssigneeFilter assignee,
            @CurrentUserId UUID agentId) {
        var items = queries.queue(null, assignee, agentId).stream()
                .map(DisputeSummaryResponse::from).toList();
        return new DisputeQueueResponse(items, queries.unassignedCount());
    }
}
