package com.snoozeshare.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.AuditAction;
import com.snoozeshare.domain.enums.WalletTransactionType;
import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.domain.model.WalletTransaction;
import com.snoozeshare.domain.validation.DomainValidation;
import com.snoozeshare.infra.events.WalletTransactionRecordedEvent;
import com.snoozeshare.repository.WalletRepository;
import com.snoozeshare.service.AuditRecord;
import com.snoozeshare.service.AuditService;

/**
 * The only code that changes a wallet balance. It runs inside the
 * caller's {@code @Transactional} boundary so the balance, the
 * money row and the state change commit or roll back together.
 * Wallet events are published via Spring's
 * {@link ApplicationEventPublisher}.
 */
@Service
@ConditionalOnDatabase
public class LedgerWriter {

    private final WalletRepository wallets;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    public LedgerWriter(WalletRepository wallets,
                        AuditService audit,
                        ApplicationEventPublisher events) {
        this.wallets = wallets;
        this.audit = audit;
        this.events = events;
    }

    /**
     * Moves {@code amount} (signed) in one wallet and records
     * the money row.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public WalletTransaction post(
            UUID walletId, WalletTransactionType type,
            BigDecimal amount, UUID actorId,
            UUID bookingId, UUID ticketId,
            String reason, Instant at) {

        if (walletId == null || type == null || actorId == null) {
            throw new IllegalArgumentException(
                    "Wallet, type and actor are required");
        }
        if (amount == null || amount.signum() == 0) {
            throw new IllegalArgumentException(
                    "Amount must not be zero");
        }
        Wallet wallet = wallets.findByIdForUpdate(walletId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Wallet does not exist"));
        DomainValidation.requireSgd(wallet.getCurrency());

        BigDecimal balanceAfter = wallet.getBalance().add(amount);
        if (balanceAfter.signum() < 0) {
            throw new IllegalArgumentException(
                    "Insufficient wallet funds");
        }

        wallet.setBalance(balanceAfter);
        wallet.setUpdatedAt(at);
        wallets.save(wallet);

        UUID txId = UUID.randomUUID();
        audit.record(AuditRecord.builder(
                actorId,
                AuditAction.forWallet(type),
                "WalletTransaction", txId)
                .wallet(amount, balanceAfter)
                .reason(reason)
                .subject(wallet.getUserId())
                .booking(bookingId)
                .ticket(ticketId)
                .at(at)
                .build());

        events.publishEvent(
                new WalletTransactionRecordedEvent(
                        txId, walletId, at));

        return new WalletTransaction(txId, type.name(),
                wallet.getUserId(), amount, balanceAfter,
                reason, bookingId, ticketId, at);
    }

    /**
     * Pays a host {@code net} and credits the platform {@code fee}
     * to the System wallet (no fee row for a zero fee).
     * Returns the host's payout row.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public WalletTransaction postPayout(
            UUID hostWalletId, BigDecimal net, BigDecimal fee,
            UUID actorId, UUID bookingId, UUID ticketId,
            Instant at) {

        DomainValidation.requireNonNegative(fee, "fee");
        String reason = fee.signum() > 0
                ? "Payout net of 3% platform fee ("
                + fee.setScale(2, RoundingMode.HALF_UP)
                        .toPlainString() + ")"
                : null;

        WalletTransaction payout = post(
                hostWalletId, WalletTransactionType.BOOKING_PAYOUT,
                net, actorId, bookingId, ticketId, reason, at);

        if (fee.signum() > 0) {
            Wallet system = wallets.findByUserId(
                    AuditService.SYSTEM_ACTOR_ID)
                    .orElseThrow(() -> new IllegalStateException(
                            "The System wallet is missing"));
            post(system.getId(),
                    WalletTransactionType.PLATFORM_FEE,
                    fee, actorId, bookingId, ticketId,
                    "3% platform fee on payout", at);
        }
        return payout;
    }
}
