package com.snoozeshare.service;

import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.model.AuditLogEntry;

public interface AuditService {

    /** The seeded SYSTEM user that owns system-initiated rows. */
    UUID SYSTEM_ACTOR_ID =
            UUID.fromString("a0000000-0000-0000-0000-0000000000ff");

    void record(AuditRecord record);

    List<AuditLogEntry> search(AuditFilter filter,
                               int limit, int offset);
}
