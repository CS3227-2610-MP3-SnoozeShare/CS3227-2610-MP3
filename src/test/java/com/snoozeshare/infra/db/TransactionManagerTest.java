package com.snoozeshare.infra.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TransactionManagerTest {

    private static final class Recorder implements TransactionListener {
        final List<String> calls = new ArrayList<>();

        @Override
        public void onBegin() {
            calls.add("begin");
        }

        @Override
        public void afterCommit() {
            calls.add("commit");
        }

        @Override
        public void afterRollback() {
            calls.add("rollback");
        }
    }

    @Test
    void notifiesListenerAfterCommit() throws Exception {
        try (Connection connection = ConnectionFactory.open("jdbc:sqlite::memory:")) {
            Recorder recorder = new Recorder();
            String result = new TransactionManager(connection, recorder).inTransaction(conn -> "ok");
            assertEquals("ok", result);
            assertEquals(List.of("begin", "commit"), recorder.calls);
            assertEquals(true, connection.getAutoCommit(), "autocommit is restored before the commit callback runs");
        }
    }

    @Test
    void notifiesListenerAfterRollback() throws Exception {
        try (Connection connection = ConnectionFactory.open("jdbc:sqlite::memory:")) {
            Recorder recorder = new Recorder();
            assertThrows(IllegalArgumentException.class, () -> new TransactionManager(connection, recorder)
                    .inTransaction(conn -> {
                        throw new IllegalArgumentException("boom");
                    }));
            assertEquals(List.of("begin", "rollback"), recorder.calls);
        }
    }

    @Test
    void commitCallbackMayStartAnotherTransaction() throws Exception {
        try (Connection connection = ConnectionFactory.open("jdbc:sqlite::memory:")) {
            TransactionManager[] holder = new TransactionManager[1];
            List<String> order = new ArrayList<>();
            TransactionListener listener = new TransactionListener() {
                private boolean first = true;

                @Override
                public void onBegin() {
                    order.add("begin");
                }

                @Override
                public void afterCommit() {
                    order.add("commit");
                    if (first) {
                        first = false;
                        try {
                            holder[0].inTransaction(conn -> "second");
                        } catch (java.sql.SQLException exception) {
                            throw new IllegalStateException(exception);
                        }
                    }
                }

                @Override
                public void afterRollback() {
                    order.add("rollback");
                }
            };
            holder[0] = new TransactionManager(connection, listener);
            holder[0].inTransaction(conn -> "first");
            assertEquals(List.of("begin", "commit", "begin", "commit"), order);
        }
    }

    @Test
    void nestedTransactionsAreRejected() throws Exception {
        try (Connection connection = ConnectionFactory.open("jdbc:sqlite::memory:")) {
            TransactionManager manager = new TransactionManager(connection);
            assertThrows(IllegalStateException.class, () -> manager.inTransaction(outer ->
                    manager.inTransaction(inner -> "nested")));
        }
    }

    @Test
    void uncheckedVariantWrapsSqlFailuresAndKeepsRuntimeExceptions() throws Exception {
        try (Connection connection = ConnectionFactory.open("jdbc:sqlite::memory:")) {
            TransactionManager manager = new TransactionManager(connection);
            assertThrows(IllegalArgumentException.class, () -> manager.inTransactionUnchecked(() -> {
                throw new IllegalArgumentException("domain failure");
            }));
            assertEquals("done", manager.inTransactionUnchecked(() -> "done"));
        }
    }
}
