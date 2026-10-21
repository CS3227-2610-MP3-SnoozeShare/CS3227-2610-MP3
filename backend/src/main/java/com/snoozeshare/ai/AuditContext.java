package com.snoozeshare.ai;

import java.util.UUID;

/**
 * Optional references copied into the audit row of a call; any field may be null.
 *
 * @param ticketId the ticket the call concerns
 * @param bookingId the booking the call concerns
 * @param subjectUserId the user the call is about, when that is not the caller
 */
public record AuditContext(UUID ticketId, UUID bookingId, UUID subjectUserId) {

    /** No references. */
    public static final AuditContext NONE = new AuditContext(null, null, null);
}
