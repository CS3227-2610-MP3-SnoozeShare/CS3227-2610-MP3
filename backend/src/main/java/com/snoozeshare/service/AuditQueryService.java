package com.snoozeshare.service;

import java.util.List;

/** Read side of the audit log, so controllers never touch the write-side service or the entity. */
public interface AuditQueryService {

    /** Newest first; a page shorter than {@code limit} is the last one. */
    List<AuditEntry> search(AuditFilter filter, int limit, int offset);
}
