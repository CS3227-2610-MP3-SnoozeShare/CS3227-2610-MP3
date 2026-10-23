package com.snoozeshare.api.layerfixtures;

import com.snoozeshare.service.AuditService;

/** Deliberately violates the audit-write boundary for the ArchUnit test. */
final class AuditFixture {
    private AuditService auditService;
}
