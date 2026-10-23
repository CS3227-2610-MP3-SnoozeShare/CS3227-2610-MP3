package com.snoozeshare.repository;

import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.enums.AssigneeFilter;
import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.model.Ticket;

/** Custom fragment for the agent ticket queue with dynamic filters. */
public interface TicketQueueRepository {

    List<Ticket> findQueue(TicketStatus status,
                           AssigneeFilter assignee,
                           UUID agentId);
}
