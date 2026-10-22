package com.snoozeshare.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.snoozeshare.domain.model.WalletTransaction;

/**
 * JDBC implementation of the ledger read side. Wallet transactions
 * are projections from audit_log rows that have a non-null
 * wallet_adjustment and subject_user_id.
 */
@Repository

public class JdbcLedgerRepository implements LedgerRepository {

    private static final String BASE_SELECT =
            "SELECT a.id, a.action_type, a.subject_user_id, "
            + "a.wallet_adjustment, a.balance_after, a.reason, "
            + "a.booking_id, a.ticket_id, a.timestamp "
            + "FROM audit_log a "
            + "WHERE a.wallet_adjustment IS NOT NULL "
            + "AND a.subject_user_id IS NOT NULL";

    private final JdbcTemplate jdbc;

    public JdbcLedgerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<WalletTransaction> entriesForWallet(UUID walletId) {
        return jdbc.query(
                BASE_SELECT
                + " AND a.subject_user_id = ("
                + "SELECT w.user_id FROM wallets w WHERE w.id = ?)"
                + " ORDER BY a.timestamp ASC, a.id ASC",
                this::mapRow, walletId);
    }

    @Override
    public List<WalletTransaction> entriesForBooking(UUID bookingId) {
        return jdbc.query(
                BASE_SELECT
                + " AND a.booking_id = ?"
                + " ORDER BY a.timestamp ASC, a.id ASC",
                this::mapRow, bookingId);
    }

    private WalletTransaction mapRow(java.sql.ResultSet rs, int row)
            throws java.sql.SQLException {
        return new WalletTransaction(
                rs.getObject("id", UUID.class),
                rs.getString("action_type"),
                rs.getObject("subject_user_id", UUID.class),
                rs.getObject("wallet_adjustment", BigDecimal.class),
                rs.getObject("balance_after", BigDecimal.class),
                rs.getString("reason"),
                rs.getObject("booking_id", UUID.class),
                rs.getObject("ticket_id", UUID.class),
                rs.getObject("timestamp", Instant.class));
    }
}
