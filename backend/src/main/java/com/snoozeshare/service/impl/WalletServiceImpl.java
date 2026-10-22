package com.snoozeshare.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.snoozeshare.domain.enums.WalletTransactionType;
import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.domain.model.WalletTransaction;
import com.snoozeshare.domain.validation.DomainValidation;
import com.snoozeshare.repository.LedgerRepository;
import com.snoozeshare.repository.WalletRepository;
import com.snoozeshare.service.WalletService;

@Service

public class WalletServiceImpl implements WalletService {

    private final WalletRepository wallets;
    private final LedgerWriter ledger;
    private final LedgerRepository ledgerEntries;

    public WalletServiceImpl(WalletRepository wallets, LedgerWriter ledger,
                             LedgerRepository ledgerEntries) {
        this.wallets = wallets;
        this.ledger = ledger;
        this.ledgerEntries = ledgerEntries;
    }

    @Override
    public Wallet getWallet(UUID userId) {
        return wallets.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet does not exist"));
    }

    @Override
    public BigDecimal balanceOf(UUID userId) {
        return getWallet(userId).getBalance();
    }

    @Override
    @Transactional
    public WalletTransaction topUp(UUID userId, BigDecimal amount) {
        DomainValidation.requirePositive(amount, "amount");
        return move(userId, WalletTransactionType.TOP_UP, amount);
    }

    @Override
    @Transactional
    public WalletTransaction withdraw(UUID userId, BigDecimal amount) {
        DomainValidation.requirePositive(amount, "amount");
        return move(userId, WalletTransactionType.WITHDRAWAL, amount.negate());
    }

    @Override
    public List<WalletTransaction> statementFor(UUID userId) {
        return ledgerEntries.entriesForWallet(getWallet(userId).getId());
    }

    private WalletTransaction move(UUID userId, WalletTransactionType type, BigDecimal amount) {
        Wallet wallet = getWallet(userId);
        return ledger.post(wallet.getId(), type, amount, userId, null, null, null, Instant.now());
    }
}
