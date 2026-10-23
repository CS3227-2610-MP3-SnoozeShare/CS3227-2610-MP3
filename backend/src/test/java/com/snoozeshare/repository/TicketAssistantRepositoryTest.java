package com.snoozeshare.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.model.TicketAssistant;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;

/** Assistant persistence constraints and compare-and-set semantics (F5, E26, E64). */
class TicketAssistantRepositoryTest extends AgentApiIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-23T01:00:00Z");

    @Autowired
    private TicketAssistantRepository assistants;

    @Autowired
    private PlatformTransactionManager transactions;

    private UUID ticketId() {
        return scenarios.openDispute(RemedyType.FULL_REFUND).ticket().getId();
    }

    @Test
    void pendingInsertIsConflictSafeAndPreservesDefaults() { // E64
        UUID id = ticketId();
        TransactionTemplate tx = new TransactionTemplate(transactions);
        assertThat(tx.<Integer>execute(status -> assistants.insertPending(id, NOW))).isEqualTo(1);
        assertThat(tx.<Integer>execute(status -> assistants.insertPending(id, NOW.plusSeconds(1)))).isZero();
        TicketAssistant row = tx.execute(status -> assistants.findByTicketIdForUpdate(id).orElseThrow());
        assertThat(row.getTicketId()).isEqualTo(id);
        assertThat(row.getState()).isEqualTo(TicketAssistant.State.PENDING);
        assertThat(row.getRounds()).isZero();
        assertThat(row.isFlagged()).isFalse();
        assertThat(row.getDraft()).isNull();
        assertThat(row.getLastMessageId()).isNull();
        assertThat(row.getCreatedAt()).isEqualTo(NOW);
        assertThat(row.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void oneConcurrentClaimWinsAndTerminalOrResolvedRowsAreNotClaimed() throws Exception { // E26
        UUID id = ticketId();
        new TransactionTemplate(transactions).executeWithoutResult(status -> assistants.insertPending(id, NOW));
        jdbc.update("update ticket_assistants set state = 'WAITING_GUEST', rounds = 1 where ticket_id = ?", id);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.<Integer>submit(() -> new TransactionTemplate(transactions)
                    .execute(status -> assistants.claimWaiting(id, NOW.plusSeconds(1))));
            var second = pool.<Integer>submit(() -> new TransactionTemplate(transactions)
                    .execute(status -> assistants.claimWaiting(id, NOW.plusSeconds(1))));
            assertThat(first.get() + second.get()).isEqualTo(1);
        }
        for (String state : new String[]{"PENDING", "DONE", "STOPPED", "UNAVAILABLE"}) {
            jdbc.update("update ticket_assistants set state = ? where ticket_id = ?", state, id);
            assertThat(new TransactionTemplate(transactions)
                    .<Integer>execute(status -> assistants.claimWaiting(id, NOW))).isZero();
        }
        jdbc.update("update ticket_assistants set state = 'WAITING_GUEST' where ticket_id = ?", id);
        jdbc.update("update tickets set status = 'RESOLVED_REJECTED' where id = ?", id);
        assertThat(new TransactionTemplate(transactions)
                .<Integer>execute(status -> assistants.claimWaiting(id, NOW))).isZero();
    }

    @Test
    void lockedReadExcludesASecondWriter() throws Exception { // E27
        UUID id = ticketId();
        new TransactionTemplate(transactions).executeWithoutResult(status -> assistants.insertPending(id, NOW));
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var first = pool.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                assistants.findByTicketIdForUpdate(id).orElseThrow();
                held.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }));
            try {
                assertThat(held.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
                    jdbc.execute("set local lock_timeout = '200ms'");
                    assistants.findByTicketIdForUpdate(id).orElseThrow();
                })).isInstanceOf(PessimisticLockingFailureException.class);
            } finally {
                release.countDown();
            }
            first.get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    void databaseRejectsInvalidStateRoundsDraftAndReferences() {
        UUID id = ticketId();
        new TransactionTemplate(transactions).executeWithoutResult(status -> assistants.insertPending(id, NOW));
        assertThatThrownBy(() -> jdbc.update("update ticket_assistants set state = 'BOGUS' where ticket_id = ?", id))
                .isInstanceOf(DataIntegrityViolationException.class);
        for (int rounds : new int[]{-1, 6}) {
            assertThatThrownBy(() -> jdbc.update("update ticket_assistants set rounds = ? where ticket_id = ?",
                    rounds, id)).isInstanceOf(DataIntegrityViolationException.class);
        }
        jdbc.update("update ticket_assistants set draft = ? where ticket_id = ?", "x".repeat(6000), id);
        assertThatThrownBy(() -> jdbc.update("update ticket_assistants set draft = ? where ticket_id = ?",
                "x".repeat(6001), id)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update ticket_assistants set last_message_id = ? where ticket_id = ?",
                UUID.randomUUID(), id)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> new TransactionTemplate(transactions)
                .executeWithoutResult(status -> assistants.insertPending(UUID.randomUUID(), NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
