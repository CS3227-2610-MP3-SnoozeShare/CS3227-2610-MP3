package com.snoozeshare.auth.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/** Persistence boundary for registration-time wallets. */
public interface WalletRepository {

    Optional<Wallet> findByUserId(UUID userId);

    Wallet insert(Wallet wallet);

    record Wallet(UUID id, UUID userId, String currency, BigDecimal balance) { }
}
