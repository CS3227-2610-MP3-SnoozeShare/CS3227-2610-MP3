package com.snoozeshare.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.snoozeshare.domain.model.AuditLogEntry;

/** Spring Data JPA repository for audit log entries. */
public interface AuditLogRepository
        extends JpaRepository<AuditLogEntry, UUID>,
        AuditLogSearchRepository {
}
