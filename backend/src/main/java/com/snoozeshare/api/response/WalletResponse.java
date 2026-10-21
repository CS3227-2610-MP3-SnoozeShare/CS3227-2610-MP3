package com.snoozeshare.api.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.model.Wallet;

public record WalletResponse(
        UUID id,
        UUID userId,
        BigDecimal balance,
        String currency,
        Instant updatedAt) {

    public static WalletResponse from(Wallet w) {
        return new WalletResponse(w.getId(), w.getUserId(), w.getBalance(),
                w.getCurrency(), w.getUpdatedAt());
    }
}
