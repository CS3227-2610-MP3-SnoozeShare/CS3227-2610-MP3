package com.snoozeshare.api.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.model.WalletTransaction;

public record TransactionResponse(
        UUID id,
        String actionType,
        BigDecimal walletAdjustment,
        BigDecimal balanceAfter,
        String reason,
        UUID bookingId,
        UUID ticketId,
        Instant timestamp) {

    public static TransactionResponse from(WalletTransaction t) {
        return new TransactionResponse(t.id(), t.actionType(),
                t.walletAdjustment(), t.balanceAfter(), t.reason(),
                t.bookingId(), t.ticketId(), t.timestamp());
    }
}
