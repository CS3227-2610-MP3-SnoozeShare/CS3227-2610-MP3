package com.snoozeshare.ai.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import com.snoozeshare.ai.AuditContext;
import com.snoozeshare.ai.LlmErrorCode;
import com.snoozeshare.ai.audit.AiAuditWriter.CapResult;
import com.snoozeshare.ai.audit.AiAuditWriter.Details;
import com.snoozeshare.ai.config.LlmProperties;
import com.snoozeshare.ai.testsupport.AiTestUsers;
import com.snoozeshare.domain.enums.AuditAction;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.AuditLogEntry;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.repository.AuditLogRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.AuditService;
import com.snoozeshare.service.impl.AuditServiceImpl;

/**
 * E13, E30, E31, E47, E55 (global reserve), E56 and E59 on a real Postgres container. Seed rows are committed (no
 * test transaction) so worker threads see them; every user and row is removed afterwards.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AiAuditWriterTest {

    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    private static final Instant NOON = Instant.parse("2026-10-22T12:00:00Z");
    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");
    private static final Details DETAILS = new Details("guest.chat", "llama3.1:8b", 12, 34, 250, 1, false, false,
            "a".repeat(64));

    @Autowired
    private AuditLogRepository entries;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private AiTestUsers users;
    private MutableClock clock;
    private AuditService realAudit;

    @BeforeEach
    void setUp() {
        users = new AiTestUsers(userRepository, jdbc);
        clock = new MutableClock(NOON);
        realAudit = new AuditServiceImpl(entries, userRepository, clock);
    }

    @AfterEach
    void tearDown() {
        users.cleanup();
    }

    private static LlmProperties props(int userCap, int globalCap, int reservePercent) {
        return new LlmProperties(false, false, "", "", "m1", "m2", "m3", 55, 4, 20, 10_000, 30_000, 45_000, 2, 5,
                30_000, 900_000, 2, userCap, globalCap, reservePercent, 40_000, false, "");
    }

    private AiAuditWriter writer(LlmProperties props) {
        return new AiAuditWriter(realAudit, entries, props, clock);
    }

    private void seed(UUID actor, AuditAction action, String reason, Instant at, int count) {
        List<AuditLogEntry> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(new AuditLogEntry(UUID.randomUUID(), actor, "n", action.name(), "AI_CALL", UUID.randomUUID(),
                    null, "feature=x", null, reason, null, null, null, null, at, null));
        }
        entries.saveAll(rows);
    }

    private long rows(UUID actor, AuditAction action, String reason) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE actor_user_id = ? AND action_type = ?"
                + " AND reason = ?", Long.class, actor, action.name(), reason);
    }

    @Test
    void capsUnderParallelBurst() throws Exception {
        UUID user = users.create(Role.GUEST).getId();
        seed(user, AuditAction.AI_CALL, "OK", Instant.parse("2026-10-22T01:00:00Z"), 195);
        seed(user, AuditAction.AI_CALL, "OK", Instant.parse("2026-10-21T23:00:00Z"), 50);
        seed(user, AuditAction.AI_CALL_REJECTED, "BUDGET_EXHAUSTED", Instant.parse("2026-10-22T02:00:00Z"), 30);
        AiAuditWriter writer = writer(props(200, 3_000, 20));

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<CapResult>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                CapResult r = writer.reserve(user, 1);
                if (r == CapResult.DAILY_CAP) {
                    writer.write(UUID.randomUUID(), user, LlmErrorCode.DAILY_CAP, false, AuditContext.NONE, DETAILS);
                }
                return r;
            }));
        }
        start.countDown();
        int reserved = 0;
        int capped = 0;
        for (Future<CapResult> f : results) {
            CapResult r = f.get(30, TimeUnit.SECONDS);
            if (r == CapResult.RESERVED) {
                reserved++;
            } else if (r == CapResult.DAILY_CAP) {
                capped++;
            }
        }
        pool.shutdownNow();

        assertThat(reserved).isEqualTo(5);
        assertThat(capped).isEqualTo(5);
        assertThat(rows(user, AuditAction.AI_CALL_REJECTED, "DAILY_CAP")).isEqualTo(1);
    }

    @Test
    void releaseFreesTheSlot() {
        UUID user = users.create(Role.GUEST).getId();
        AiAuditWriter writer = writer(props(1, 3_000, 20));

        assertThat(writer.reserve(user, 1)).isEqualTo(CapResult.RESERVED);
        assertThat(writer.reserve(user, 1)).isEqualTo(CapResult.DAILY_CAP);
        writer.release(user, false);
        assertThat(writer.reserve(user, 1)).isEqualTo(CapResult.RESERVED);
    }

    @Test
    void utcDay() {
        UUID user = users.create(Role.GUEST).getId();
        seed(user, AuditAction.AI_CALL, "OK", Instant.parse("2026-10-22T23:00:00Z"), 2);
        AiAuditWriter writer = writer(props(2, 3_000, 20));

        clock.set(Instant.parse("2026-10-22T23:59:59Z"));
        assertThat(writer.reserve(user, 1)).isEqualTo(CapResult.DAILY_CAP);

        clock.set(Instant.parse("2026-10-23T00:00:01Z"));
        assertThat(writer.reserve(user, 1)).isEqualTo(CapResult.RESERVED);
    }

    @Test
    void globalReserve() {
        UUID a = users.create(Role.GUEST).getId();
        UUID b = users.create(Role.HOST).getId();
        UUID c = users.create(Role.AGENT).getId();
        seed(a, AuditAction.AI_CALL, "OK", Instant.parse("2026-10-22T01:00:00Z"), 4);
        seed(b, AuditAction.AI_CALL, "OK", Instant.parse("2026-10-22T01:00:00Z"), 4);
        AiAuditWriter writer = writer(props(100, 10, 20));

        assertThat(writer.reserve(c, 2)).isEqualTo(CapResult.DAILY_CAP);
        assertThat(writer.reserve(c, 0)).isEqualTo(CapResult.RESERVED);
        assertThat(writer.reserve(c, 0)).isEqualTo(CapResult.RESERVED);
        assertThat(writer.reserve(c, 0)).isEqualTo(CapResult.DAILY_CAP);
    }

    @Test
    void countFailureFailsClosed() {
        UUID user = UUID.randomUUID();
        AuditLogRepository broken = mock(AuditLogRepository.class);
        when(broken.countByActorUserIdAndActionTypeAndTimestampGreaterThanEqual(any(), anyString(), any()))
                .thenThrow(new IllegalStateException("db down"));
        AiAuditWriter failing = new AiAuditWriter(realAudit, broken, props(1, 10, 20), clock);

        assertThat(failing.reserve(user, 1)).isEqualTo(CapResult.COUNT_FAILED);
        assertThat(failing.reserve(user, 0)).isEqualTo(CapResult.COUNT_FAILED);

        UUID real = users.create(Role.GUEST).getId();
        AiAuditWriter working = writer(props(1, 10, 20));
        assertThat(working.reserve(real, 1)).isEqualTo(CapResult.RESERVED);
    }

    @Test
    void failedAuditStaysCounted() {
        UUID a = users.create(Role.GUEST).getId();
        UUID b = users.create(Role.GUEST).getId();
        AuditService broken = mock(AuditService.class);
        doThrow(new IllegalStateException("audit down")).when(broken).record(any());
        AiAuditWriter writer = new AiAuditWriter(broken, entries, props(2, 2, 0), clock);

        for (int i = 0; i < 2; i++) {
            assertThat(writer.reserve(a, 1)).isEqualTo(CapResult.RESERVED);
            assertThat(writer.write(UUID.randomUUID(), a, null, true, AuditContext.NONE, DETAILS)).isFalse();
            writer.release(a, true);
        }

        assertThat(writer.reserve(a, 1)).isEqualTo(CapResult.DAILY_CAP);
        assertThat(writer.reserve(b, 1)).isEqualTo(CapResult.DAILY_CAP);

        clock.set(Instant.parse("2026-10-23T00:00:01Z"));
        assertThat(writer.reserve(b, 1)).isEqualTo(CapResult.RESERVED);
    }

    @Test
    void writerFailures() {
        UUID user = users.create(Role.GUEST).getId();
        AuditService throwing = mock(AuditService.class);
        doThrow(new IllegalStateException("boom")).when(throwing).record(any());

        assertThat(new AiAuditWriter(throwing, entries, props(5, 50, 20), clock)
                .write(UUID.randomUUID(), user, null, true, AuditContext.NONE, DETAILS)).isFalse();

        AuditContext missingTicket = new AuditContext(UUID.randomUUID(), null, null);
        assertThat(writer(props(5, 50, 20))
                .write(UUID.randomUUID(), user, null, true, missingTicket, DETAILS)).isFalse();
        assertThat(rows(user, AuditAction.AI_CALL, "OK")).isZero();
    }

    @Test
    void failedRejectedWriteDoesNotBlockTheNextOne() {
        UUID user = users.create(Role.GUEST).getId();
        AuditService throwing = mock(AuditService.class);
        doThrow(new IllegalStateException("boom")).when(throwing).record(any());

        assertThat(new AiAuditWriter(throwing, entries, props(5, 50, 20), clock)
                .write(UUID.randomUUID(), user, LlmErrorCode.DAILY_CAP, false, AuditContext.NONE, DETAILS)).isFalse();
        AiAuditWriter writer = writer(props(5, 50, 20));
        assertThat(writer.write(UUID.randomUUID(), user, LlmErrorCode.DAILY_CAP, false, AuditContext.NONE, DETAILS))
                .isTrue();
        assertThat(rows(user, AuditAction.AI_CALL_REJECTED, "DAILY_CAP")).isEqualTo(1);
    }

    @Test
    void coalescing() {
        UUID user = users.create(Role.GUEST).getId();
        UUID other = users.create(Role.GUEST).getId();
        AiAuditWriter writer = writer(props(5, 50, 20));

        for (int i = 0; i < 20; i++) {
            assertThat(writer.write(UUID.randomUUID(), user, LlmErrorCode.DAILY_CAP, false, AuditContext.NONE,
                    DETAILS)).isTrue();
        }
        assertThat(rows(user, AuditAction.AI_CALL_REJECTED, "DAILY_CAP")).isEqualTo(1);

        writer.write(UUID.randomUUID(), user, LlmErrorCode.QUEUE_FULL, false, AuditContext.NONE, DETAILS);
        writer.write(UUID.randomUUID(), other, LlmErrorCode.DAILY_CAP, false, AuditContext.NONE, DETAILS);
        assertThat(rows(user, AuditAction.AI_CALL_REJECTED, "QUEUE_FULL")).isEqualTo(1);
        assertThat(rows(other, AuditAction.AI_CALL_REJECTED, "DAILY_CAP")).isEqualTo(1);

        clock.set(NOON.plus(Duration.ofMinutes(10)));
        writer.write(UUID.randomUUID(), user, LlmErrorCode.DAILY_CAP, false, AuditContext.NONE, DETAILS);
        assertThat(rows(user, AuditAction.AI_CALL_REJECTED, "DAILY_CAP")).isEqualTo(2);
    }

    @Test
    void ordinaryCallRowsAreNeverCoalesced() {
        UUID user = users.create(Role.GUEST).getId();
        AiAuditWriter writer = writer(props(5, 50, 20));

        writer.write(UUID.randomUUID(), user, LlmErrorCode.SERVER, true, AuditContext.NONE, DETAILS);
        writer.write(UUID.randomUUID(), user, LlmErrorCode.SERVER, true, AuditContext.NONE, DETAILS);

        assertThat(rows(user, AuditAction.AI_CALL, "SERVER")).isEqualTo(2);
    }

    @Test
    void noPromptText() {
        User user = users.create(Role.GUEST);
        User subject = users.create(Role.HOST);
        UUID callId = UUID.randomUUID();
        Details details = new Details("guest.chat", null, 0, 0, 7, 0, true, true, null);

        assertThat(writer(props(5, 50, 20)).write(callId, user.getId(), LlmErrorCode.BLOCKED_INPUT, false,
                new AuditContext(null, null, subject.getId()), details)).isTrue();

        AuditLogEntry row = entries.findAll().stream().filter(e -> callId.equals(e.getEntityId())).findFirst()
                .orElseThrow();
        assertThat(row.getActionType()).isEqualTo("AI_CALL_REJECTED");
        assertThat(row.getEntityType()).isEqualTo("AI_CALL");
        assertThat(row.getActorUserId()).isEqualTo(user.getId());
        assertThat(row.getSubjectUserId()).isEqualTo(subject.getId());
        assertThat(row.getBeforeState()).isNull();
        assertThat(row.getWalletAdjustment()).isNull();
        assertThat(row.getReason()).isEqualTo("BLOCKED_INPUT");
        assertThat(row.getTimestamp()).isEqualTo(NOON);
        assertThat(row.getAfterState()).isEqualTo("feature=guest.chat;model=-;tokensIn=0;tokensOut=0;latencyMs=7;"
                + "attempts=0;flagged=true;truncated=true;promptHash=-").hasSizeLessThan(400);
        String everything = String.join("|", row.getAfterState(), row.getReason(), row.getActionType(),
                String.valueOf(row.getActorName()), String.valueOf(row.getSubjectName()));
        assertThat(everything).doesNotContain("ZZ-MARKER-PROMPT").doesNotContain("ZZ-MARKER-REPLY");
    }

    @Test
    void okRowCarriesModelAndHash() {
        User user = users.create(Role.GUEST);
        UUID callId = UUID.randomUUID();

        assertThat(writer(props(5, 50, 20)).write(callId, user.getId(), null, true, null, DETAILS)).isTrue();

        AuditLogEntry row = entries.findAll().stream().filter(e -> callId.equals(e.getEntityId())).findFirst()
                .orElseThrow();
        assertThat(row.getActionType()).isEqualTo("AI_CALL");
        assertThat(row.getReason()).isEqualTo("OK");
        assertThat(row.getAfterState()).isEqualTo("feature=guest.chat;model=llama3.1:8b;tokensIn=12;tokensOut=34;"
                + "latencyMs=250;attempts=1;flagged=false;truncated=false;promptHash=" + "a".repeat(64));
    }

    /** A clock whose instant a test moves; the zone is Singapore, as the application clock is not UTC. */
    private static final class MutableClock extends Clock {
        private volatile Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void set(Instant instant) {
            this.now = instant;
        }

        @Override
        public ZoneId getZone() {
            return SINGAPORE;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
