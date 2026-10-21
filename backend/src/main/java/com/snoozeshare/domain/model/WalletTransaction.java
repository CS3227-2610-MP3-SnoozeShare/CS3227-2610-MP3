package com.snoozeshare.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A wallet transaction projected from an audit_log row that has
 * a non-null wallet_adjustment. Not a JPA entity.
 */
public record WalletTransaction(
        UUID id,
        String actionType,
        UUID subjectUserId,
        BigDecimal walletAdjustment,
        BigDecimal balanceAfter,
        String reason,
        UUID bookingId,
        UUID ticketId,
        Instant timestamp
) {
}
