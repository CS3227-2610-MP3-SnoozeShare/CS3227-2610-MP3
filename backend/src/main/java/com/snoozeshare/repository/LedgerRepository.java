package com.snoozeshare.repository;

import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.model.WalletTransaction;

/**
 * Read side of the ledger: the money rows of the audit log,
 * oldest first (insertion order). Not a JPA repository because
 * {@link WalletTransaction} has no table.
 */
public interface LedgerRepository {

    List<WalletTransaction> entriesForWallet(UUID walletId);

    List<WalletTransaction> entriesForBooking(UUID bookingId);
}
