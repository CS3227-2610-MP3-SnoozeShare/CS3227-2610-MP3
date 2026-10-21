package com.snoozeshare.domain.settlement;

import java.util.List;
import java.util.Set;

import com.snoozeshare.domain.model.WalletTransaction;

public final class EscrowPolicy {

    private static final Set<String> RELEASING = Set.of(
            "ESCROW_REFUND", "BOOKING_PAYOUT",
            "TICKET_REMEDY", "AGENT_OVERRIDE");

    private EscrowPolicy() {
    }

    /**
     * Escrow is held when a booking has an ESCROW_HOLD row and nothing has released it yet.
     */
    public static boolean isHeld(List<WalletTransaction> bookingTransactions) {
        boolean held = false;
        for (WalletTransaction transaction : bookingTransactions) {
            if (RELEASING.contains(transaction.actionType())) {
                return false;
            }
            if ("ESCROW_HOLD".equals(transaction.actionType())) {
                held = true;
            }
        }
        return held;
    }
}
