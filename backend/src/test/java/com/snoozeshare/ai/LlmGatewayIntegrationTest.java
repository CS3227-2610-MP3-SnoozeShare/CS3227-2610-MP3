package com.snoozeshare.ai;

import static com.snoozeshare.ai.testsupport.StubSoclaasServer.completion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.snoozeshare.PostgresIntegrationTest;
import com.snoozeshare.ai.testsupport.AiTestUsers;
import com.snoozeshare.ai.testsupport.StubSoclaasServer;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.repository.UserRepository;

/** The gateway on the full application: real audit rows, real transaction detection, a stub upstream. */
class LlmGatewayIntegrationTest extends PostgresIntegrationTest {

    private static StubSoclaasServer stub;

    @Autowired
    private LlmGateway gateway;
    @Autowired
    private UserRepository users;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactions;

    private AiTestUsers testUsers;

    @BeforeAll
    static void startStub() throws IOException {
        stub = new StubSoclaasServer();
    }

    @AfterAll
    static void stopStub() {
        stub.close();
    }

    @DynamicPropertySource
    static void ai(DynamicPropertyRegistry registry) throws IOException {
        if (stub == null) {
            stub = new StubSoclaasServer();
        }
        registry.add("snoozeshare.ai.enabled", () -> "true");
        registry.add("snoozeshare.ai.base-url", () -> stub.baseUrl());
        registry.add("snoozeshare.ai.api-key", () -> "clsk_abcdefgh_0123456789abcdef0123");
    }

    @BeforeEach
    void setUp() {
        testUsers = new AiTestUsers(users, jdbc);
    }

    @AfterEach
    void tearDown() {
        testUsers.cleanup();
    }

    private static AiFeature<String> feature() {
        return new AiFeature<>("guest.helper", Set.of(Role.GUEST), ModelTier.STANDARD, "You help guests.", 200, 0.2,
            1, 30, OutputSpec.text(500, false), call -> "FALLBACK", "mock");
    }

    private static LlmCall call(UUID user, String text) {
        return new LlmCall(user, Role.GUEST, List.of(new Turn(TurnRole.USER, text)), null, null, null);
    }

    private int rows(UUID user, String action) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE actor_user_id = ? AND action_type = ?",
            Integer.class, user, action);
    }

    @Test
    void okCallWritesOneCallRowKeyedByTheCallId() {
        stub.script(completion("Hello there", "stop"));
        UUID user = testUsers.create(Role.GUEST).getId();
        LlmResult<String> result = gateway.run(feature(), call(user, "Where is my booking?"));
        assertThat(result.ok()).isTrue();
        assertThat(rows(user, "AI_CALL")).isEqualTo(1);
        assertThat(rows(user, "AI_CALL_REJECTED")).isZero();
        UUID entityId = jdbc.queryForObject("SELECT entity_id FROM audit_log WHERE actor_user_id = ?", UUID.class,
            user);
        assertThat(entityId).isEqualTo(result.callId());
    }

    @Test
    void blockedInputWritesARejectedRow() {
        UUID user = testUsers.create(Role.GUEST).getId();
        LlmResult<String> result = gateway.run(feature(), call(user, "\u0000\u0007"));
        assertThat(result.code()).isEqualTo(LlmErrorCode.BLOCKED_INPUT);
        assertThat(rows(user, "AI_CALL_REJECTED")).isEqualTo(1);
        assertThat(rows(user, "AI_CALL")).isZero();
    }

    @Test
    void neitherPromptNorReplyTextReachesTheAuditRow() {
        stub.script(completion("REPLYMARKER-77 is the answer", "stop"));
        UUID user = testUsers.create(Role.GUEST).getId();
        gateway.run(feature(), call(user, "PROMPTMARKER-42 please help"));
        List<String> text = jdbc.queryForList("SELECT CAST(t AS text) FROM audit_log t WHERE actor_user_id = ?",
            String.class, user);
        assertThat(text).hasSize(1);
        assertThat(text.get(0)).doesNotContain("PROMPTMARKER").doesNotContain("REPLYMARKER");
        assertThat(text.get(0)).contains("feature=guest.helper").contains("promptHash=");
    }

    @Test
    void codesWithoutARowStillCarryACallId() {
        UUID host = testUsers.create(Role.HOST).getId();
        LlmResult<String> result = gateway.run(feature(),
            new LlmCall(host, Role.HOST, List.of(new Turn(TurnRole.USER, "hi")), null, null, null));
        assertThat(result.code()).isEqualTo(LlmErrorCode.ROLE_DENIED);
        assertThat(result.callId()).isNotNull();
        assertThat(rows(host, "AI_CALL") + rows(host, "AI_CALL_REJECTED")).isZero();
    }

    @Test
    void refusesInsideATransaction() {
        UUID user = testUsers.create(Role.GUEST).getId();
        TransactionTemplate tx = new TransactionTemplate(transactions);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> gateway.run(feature(), call(user, "hi"))))
            .isInstanceOf(IllegalStateException.class);
    }
}
