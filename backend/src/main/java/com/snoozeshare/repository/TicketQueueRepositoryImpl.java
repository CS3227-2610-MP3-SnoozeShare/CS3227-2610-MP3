package com.snoozeshare.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.enums.AssigneeFilter;
import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.model.Ticket;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/** Implements the dynamic agent queue filter for tickets. */
public class TicketQueueRepositoryImpl implements TicketQueueRepository {

    private final EntityManager em;

    public TicketQueueRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<Ticket> findQueue(TicketStatus status,
                                  AssigneeFilter assignee,
                                  UUID agentId) {
        var jpql = new StringBuilder("SELECT t FROM Ticket t WHERE 1=1");
        var params = new ArrayList<Object[]>();

        if (status != null) {
            jpql.append(" AND t.status = :status");
            params.add(new Object[]{"status", status});
        }

        if (assignee != null) {
            switch (assignee) {
                case MINE -> {
                    jpql.append(" AND t.assignedAgentId = :agentId");
                    params.add(new Object[]{"agentId", agentId});
                }
                case UNASSIGNED -> jpql.append(
                        " AND t.assignedAgentId IS NULL");
                case ALL -> { }
                default -> { }
            }
        }

        jpql.append(" ORDER BY t.createdAt ASC");

        TypedQuery<Ticket> query = em.createQuery(
                jpql.toString(), Ticket.class);
        for (Object[] param : params) {
            query.setParameter((String) param[0], param[1]);
        }
        return query.getResultList();
    }
}
