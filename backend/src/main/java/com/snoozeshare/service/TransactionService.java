package com.snoozeshare.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.model.WalletTransaction;

/**
 * Booking-level money movements: escrow hold, refund and the payout when a stay completes.
 */
public interface TransactionService {

    WalletTransaction holdEscrow(UUID bookingId);

    WalletTransaction refundEscrow(UUID bookingId, BigDecimal refundAmount);

    WalletTransaction settleBookingCompletion(UUID bookingId);

    List<WalletTransaction> historyFor(UUID bookingId);
}
