package com.snoozeshare.repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.snoozeshare.domain.model.AuditLogEntry;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/** Implements dynamic audit log search with text, action-type and date filters. */
public class AuditLogSearchRepositoryImpl
        implements AuditLogSearchRepository {

    private final EntityManager em;

    public AuditLogSearchRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<AuditLogEntry> search(AuditCriteria criteria,
                                      int limit, int offset) {
        var jpql = new StringBuilder("SELECT a FROM AuditLogEntry a");
        var wheres = new ArrayList<String>();
        var params = new ArrayList<Object[]>();

        if (criteria.textGiven()) {
            if (criteria.userIds().isEmpty()
                    && criteria.idFragment() == null) {
                return List.of();
            }
            var textParts = new ArrayList<String>();
            if (!criteria.userIds().isEmpty()) {
                textParts.add("a.actorUserId IN :userIds");
                textParts.add("a.subjectUserId IN :userIds");
                params.add(new Object[]{"userIds", criteria.userIds()});
            }
            if (criteria.idFragment() != null) {
                textParts.add(
                        "CAST(a.entityId AS string) LIKE :idFrag");
                textParts.add(
                        "CAST(a.bookingId AS string) LIKE :idFrag");
                textParts.add(
                        "CAST(a.ticketId AS string) LIKE :idFrag");
                String idPattern = "%" + criteria.idFragment() + "%";
                params.add(new Object[]{"idFrag", idPattern});
            }
            wheres.add("(" + String.join(" OR ", textParts) + ")");
        }

        if (!criteria.actionTypes().isEmpty()) {
            wheres.add("a.actionType IN :actionTypes");
            params.add(new Object[]{"actionTypes",
                    criteria.actionTypes()});
        }
        if (criteria.from() != null) {
            wheres.add("a.timestamp >= :fromTs");
            params.add(new Object[]{"fromTs", criteria.from()});
        }
        if (criteria.toExclusive() != null) {
            wheres.add("a.timestamp < :toTs");
            params.add(new Object[]{"toTs", criteria.toExclusive()});
        }

        if (!wheres.isEmpty()) {
            jpql.append(" WHERE ");
            jpql.append(String.join(" AND ", wheres));
        }
        jpql.append(" ORDER BY a.timestamp DESC, a.id DESC");

        TypedQuery<AuditLogEntry> query = em.createQuery(
                jpql.toString(), AuditLogEntry.class);
        for (Object[] param : params) {
            query.setParameter((String) param[0], param[1]);
        }
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    @Override
    public Set<UUID> findUserIdsByName(String fragment) {
        String pattern = "%" + fragment.toLowerCase() + "%";

        List<UUID> fromUsers = em.createQuery(
                "SELECT u.id FROM User u "
                        + "WHERE LOWER(u.displayName) LIKE :p "
                        + "OR LOWER(u.email) LIKE :p",
                UUID.class)
                .setParameter("p", pattern)
                .getResultList();

        List<UUID> fromActors = em.createQuery(
                "SELECT DISTINCT a.actorUserId FROM AuditLogEntry a "
                        + "WHERE LOWER(a.actorName) LIKE :p",
                UUID.class)
                .setParameter("p", pattern)
                .getResultList();

        List<UUID> fromSubjects = em.createQuery(
                "SELECT DISTINCT a.subjectUserId "
                        + "FROM AuditLogEntry a "
                        + "WHERE LOWER(a.subjectName) LIKE :p "
                        + "AND a.subjectUserId IS NOT NULL",
                UUID.class)
                .setParameter("p", pattern)
                .getResultList();

        var result = new HashSet<>(fromUsers);
        result.addAll(fromActors);
        result.addAll(fromSubjects);
        return result;
    }
}
