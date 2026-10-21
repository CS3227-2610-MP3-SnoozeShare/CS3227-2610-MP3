package com.snoozeshare.repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.snoozeshare.domain.model.AuditLogEntry;

/** Custom fragment for dynamic audit log search queries. */
public interface AuditLogSearchRepository {

    List<AuditLogEntry> search(AuditCriteria criteria,
                               int limit, int offset);

    Set<UUID> findUserIdsByName(String fragment);
}
