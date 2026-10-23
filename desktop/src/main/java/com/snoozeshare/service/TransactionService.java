package com.snoozeshare.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.model.WalletTransaction;

/**
 * Booking-level money movements: escrow hold, refund and the payout when a stay completes.
 *
 * All amounts go through {@code LedgerWriter}, so every call leaves wallet rows and audit rows that
 * agree. Dispute resolutions are settled by {@code DisputeSettlementService}, not here.
 */
public interface TransactionService {
    /**
     * Takes the full booking total out of the guest's wallet into escrow.
     *
     * @param bookingId the booking being paid for
     * @return the {@code ESCROW_HOLD} ledger row (negative amount)
     * @throws IllegalArgumentException if the booking or the guest's wallet does not exist
     */
    WalletTransaction holdEscrow(UUID bookingId);

    /**
     * Returns part or all of the escrowed amount to the guest.
     *
     * @param bookingId the booking being refunded
     * @param refundAmount the amount to give back
     * @return the {@code ESCROW_REFUND} ledger row
     */
    WalletTransaction refundEscrow(UUID bookingId, BigDecimal refundAmount);

    /**
     * Completes a confirmed booking: pays the host the net amount, records the platform fee against
     * the System wallet, and marks the booking completed. Idempotent: calling it again returns the
     * existing payout.
     *
     * @return the {@code BOOKING_PAYOUT} ledger row
     * @throws IllegalStateException if the booking is neither confirmed nor already completed
     */
    WalletTransaction settleBookingCompletion(UUID bookingId);

    /**
     * @param bookingId the booking
     * @return every ledger row recorded against the booking
     */
    List<WalletTransaction> historyFor(UUID bookingId);
}
