package com.snoozeshare.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.domain.model.WalletTransaction;

/**
 * A user's wallet balance and statement, plus mocked top-ups and withdrawals.
 *
 * No real payment service is involved: top-ups and withdrawals only move numbers in the ledger.
 */
public interface WalletService {
    /**
     * @throws IllegalArgumentException if the user has no wallet
     */
    Wallet getWallet(UUID userId);

    /**
     * @return the current balance
     */
    BigDecimal balanceOf(UUID userId);

    /**
     * @param amount must be positive
     * @return the {@code TOP_UP} ledger row
     */
    WalletTransaction topUp(UUID userId, BigDecimal amount);

    /**
     * @param amount must be positive
     * @return the {@code WITHDRAWAL} ledger row (negative amount)
     */
    WalletTransaction withdraw(UUID userId, BigDecimal amount);

    /**
     * @return the user's ledger rows
     */
    List<WalletTransaction> statementFor(UUID userId);
}
