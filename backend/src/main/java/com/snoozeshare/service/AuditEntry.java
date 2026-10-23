package com.snoozeshare.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.model.AuditLogEntry;

/** One audit log row as the agent sees it: names instead of user ids. */
public record AuditEntry(
        UUID id,
        Instant timestamp,
        String actorName,
        String actionType,
        String entityType,
        UUID entityId,
        String beforeState,
        String afterState,
        BigDecimal walletAdjustment,
        BigDecimal balanceAfter,
        String reason,
        String subjectName,
        UUID bookingId,
        UUID ticketId) {

    public static AuditEntry from(AuditLogEntry e) {
        return new AuditEntry(e.getId(), e.getTimestamp(), e.getActorName(), e.getActionType(),
                e.getEntityType(), e.getEntityId(), e.getBeforeState(), e.getAfterState(),
                e.getWalletAdjustment(), e.getBalanceAfter(), e.getReason(), e.getSubjectName(),
                e.getBookingId(), e.getTicketId());
    }
}
