package com.snoozeshare.infra.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Runs work in one database transaction and reports its outcome to a {@link TransactionListener}. Transactions
 * do not nest: a second {@code inTransaction} while one is open throws.
 */
public final class TransactionManager {

    private final Connection connection;
    private final TransactionListener listener;

    public TransactionManager(Connection connection) {
        this(connection, TransactionListener.NONE);
    }

    public TransactionManager(Connection connection, TransactionListener listener) {
        this.connection = connection;
        this.listener = listener;
    }

    public <T> T inTransaction(SqlWork<T> work) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        if (!originalAutoCommit) {
            throw new IllegalStateException("Nested transactions are not supported");
        }
        listener.onBegin();
        connection.setAutoCommit(false);
        T result;
        try {
            result = work.apply(connection);
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            try {
                connection.rollback();
            } finally {
                connection.setAutoCommit(originalAutoCommit);
                listener.afterRollback();
            }
            throw exception;
        }
        try {
            connection.setAutoCommit(originalAutoCommit);
        } finally {
            listener.afterCommit();
        }
        return result;
    }

    /** Like {@link #inTransaction} for work that throws no checked exception; SQL failures become unchecked. */
    public <T> T inTransactionUnchecked(Supplier<T> work) {
        try {
            return inTransaction(conn -> work.get());
        } catch (SQLException exception) {
            throw new IllegalStateException("Database transaction failed", exception);
        }
    }
}
