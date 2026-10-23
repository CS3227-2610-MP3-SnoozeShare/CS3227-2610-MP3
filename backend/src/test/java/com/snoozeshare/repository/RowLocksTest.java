package com.snoozeshare.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.snoozeshare.PostgresIntegrationTest;

/** Proves the transaction-local timeout bounds a competing PostgreSQL lock wait. */
class RowLocksTest extends PostgresIntegrationTest {

    @Autowired
    private RowLocks rowLocks;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void competingTransactionTimesOutAfterConfiguredLockWait() throws Exception {
        CountDownLatch firstLockHeld = new CountDownLatch(1);
        CountDownLatch releaseFirstLock = new CountDownLatch(1);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        long started;
        try {
            executor.submit(() -> {
                try {
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(314159)");
                        firstLockHeld.countDown();
                        try {
                            releaseFirstLock.await(10, TimeUnit.SECONDS);
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(exception);
                        }
                    });
                } catch (Throwable exception) {
                    firstFailure.set(exception);
                }
            });
            assertThat(firstLockHeld.await(5, TimeUnit.SECONDS)).isTrue();
            started = System.nanoTime();
            assertThatThrownBy(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        rowLocks.timeout();
                        jdbcTemplate.execute("SELECT pg_advisory_xact_lock(314159)");
                    }))
                    .isInstanceOf(UncategorizedSQLException.class)
                    .satisfies(exception -> assertThat(((UncategorizedSQLException) exception)
                            .getSQLException().getSQLState()).isEqualTo("55P03"));
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            assertThat(elapsedMillis).isBetween(4_000L, 8_000L);
        } finally {
            releaseFirstLock.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(firstFailure.get()).isNull();
    }
}
