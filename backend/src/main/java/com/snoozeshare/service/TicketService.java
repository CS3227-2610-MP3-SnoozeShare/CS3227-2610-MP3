package com.snoozeshare.service;

import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.enums.AssigneeFilter;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.model.TicketCategory;
import com.snoozeshare.service.requests.NewTicketRequest;
import com.snoozeshare.service.requests.ResolutionRequest;

public interface TicketService {

    Ticket fileTicket(NewTicketRequest request, UUID raisedByUserId, Role raisedByRole);

    List<Ticket> myTickets(UUID userId);

    List<TicketCategory> listCategories();

    List<TicketCategory> listAllCategories();

    TicketCategory createCategory(String label, UUID agentId);

    TicketCategory renameCategory(UUID categoryId, String label, UUID agentId);

    TicketCategory setCategoryActive(UUID categoryId, boolean active, UUID agentId);

    void deleteCategory(UUID categoryId, UUID agentId);

    List<Ticket> queueForAgent(TicketStatus statusFilter, AssigneeFilter assignee, UUID agentId);

    Ticket assignToMe(UUID ticketId, UUID agentId);

    Ticket unassign(UUID ticketId, UUID agentId);

    Ticket saveNotes(UUID ticketId, String notes, UUID agentId);

    Ticket resolve(UUID ticketId, ResolutionRequest request, UUID agentId);
}
