package com.snoozeshare.api.response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.service.AuditEntry;

public record AuditEntryResponse(
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

    public static AuditEntryResponse from(AuditEntry e) {
        return new AuditEntryResponse(e.id(), e.timestamp(), e.actorName(), e.actionType(), e.entityType(),
                e.entityId(), e.beforeState(), e.afterState(), money(e.walletAdjustment()),
                money(e.balanceAfter()), e.reason(), e.subjectName(), e.bookingId(), e.ticketId());
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
