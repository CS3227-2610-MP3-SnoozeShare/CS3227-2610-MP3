package com.snoozeshare.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.snoozeshare.domain.model.AuditLogEntry;

/** Spring Data JPA repository for audit log entries. */
public interface AuditLogRepository
        extends JpaRepository<AuditLogEntry, UUID>,
        AuditLogSearchRepository {

    /** Counts one actor's rows of an action type at or after an instant (the per-user AI daily cap). */
    long countByActorUserIdAndActionTypeAndTimestampGreaterThanEqual(
            UUID actorUserId, String actionType, Instant from);

    /** Counts every actor's rows of an action type at or after an instant (the global AI daily cap). */
    long countByActionTypeAndTimestampGreaterThanEqual(String actionType, Instant from);
}
