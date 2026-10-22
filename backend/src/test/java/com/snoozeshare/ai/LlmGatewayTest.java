package com.snoozeshare.ai;

import static com.snoozeshare.ai.testsupport.StubSoclaasServer.completion;
import static com.snoozeshare.ai.testsupport.StubSoclaasServer.delayed;
import static com.snoozeshare.ai.testsupport.StubSoclaasServer.reply;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.ai.audit.AiAuditWriter;
import com.snoozeshare.ai.audit.AiAuditWriter.CapResult;
import com.snoozeshare.ai.client.SoclaasClient;
import com.snoozeshare.ai.config.LlmProperties;
import com.snoozeshare.ai.guard.OutputValidator;
import com.snoozeshare.ai.guard.PromptBuilder;
import com.snoozeshare.ai.limit.Backoff;
import com.snoozeshare.ai.limit.BudgetBlock;
import com.snoozeshare.ai.limit.CircuitBreaker;
import com.snoozeshare.ai.limit.LlmScheduler;
import com.snoozeshare.ai.limit.UserThrottle;
import com.snoozeshare.ai.testsupport.StubSoclaasServer;
import com.snoozeshare.domain.enums.Role;

import jakarta.validation.Validation;

/** The pipeline of {@code LlmGateway.run} with a faked audit writer, real limit classes and a stub server. */
class LlmGatewayTest {

    private static final String KEY = "clsk_abcdefgh_0123456789abcdef0123";

    private StubSoclaasServer stub;
    private AiAuditWriter writer;
    private final List<Long> sleeps = new ArrayList<>();
    private final ExecutorService pool = Executors.newCachedThreadPool();

    @BeforeEach
    void setUp() throws IOException {
        stub = new StubSoclaasServer();
        writer = mock(AiAuditWriter.class);
        when(writer.reserve(any(), anyInt())).thenReturn(CapResult.RESERVED);
        when(writer.write(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        stub.close();
        pool.shutdownNow();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private LlmProperties props(boolean enabled, boolean mock, int maxRetries, long requestTimeout, long deadline,
                                int breakerFailures, int inFlight) {
        return new LlmProperties(enabled, mock, stub.baseUrl(), KEY, "fast-m", "std-m", "strong-m", 55, 4, 20,
            10_000, requestTimeout, deadline, maxRetries, breakerFailures, 30_000, 900_000, inFlight, 200, 3_000, 20,
            40_000, false, "");
    }

    private LlmProperties props() {
        return props(true, false, 2, 30_000, 45_000, 5, 2);
    }

    private LlmGateway gateway(LlmProperties p) {
        Clock clock = Clock.systemUTC();
        return new LlmGateway(p, writer, new PromptBuilder(),
            new OutputValidator(new ObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator()),
            new SoclaasClient(p), new LlmScheduler(p, clock), new UserThrottle(clock, p.perUserMaxInFlight()),
            new CircuitBreaker(clock, p.breakerFailures(), p.breakerOpenMs()),
            new BudgetBlock(clock, p.budgetBlockMs()), new Backoff(() -> 1.0), sleeps::add, clock);
    }

    private static AiFeature<String> feature(int perMinute) {
        return new AiFeature<>("guest.helper", Set.of(Role.GUEST), ModelTier.STANDARD, "You help guests.", 200, 0.2,
            1, perMinute, OutputSpec.text(500, false), call -> "FALLBACK", "mock reply");
    }

    private static LlmCall call(UUID user, Role role, String text) {
        return new LlmCall(user, role, List.of(new Turn(TurnRole.USER, text)), null, null, null);
    }

    private static LlmCall guestCall(String text) {
        return call(UUID.randomUUID(), Role.GUEST, text);
    }

    private AiAuditWriter.Details lastDetails() {
        ArgumentCaptor<AiAuditWriter.Details> details = ArgumentCaptor.forClass(AiAuditWriter.Details.class);
        verify(writer, org.mockito.Mockito.atLeastOnce())
            .write(any(), any(), any(), anyBoolean(), any(), details.capture());
        return details.getValue();
    }

    private void verifyRow(LlmErrorCode code, boolean asCall) {
        verify(writer).write(any(), any(), eq(code), eq(asCall), any(), any());
    }

    @Test
    void okCallReturnsValueAndWritesACallRow() {
        stub.script(completion("Hello guest", "stop"));
        LlmResult<String> result = gateway(props()).run(feature(10), guestCall("Where is my booking?"));
        assertThat(result.ok()).isTrue();
        assertThat(result.value()).isEqualTo("Hello guest");
        assertThat(result.code()).isNull();
        assertThat(result.callId()).isNotNull();
        verifyRow(null, true);
        AiAuditWriter.Details d = lastDetails();
        assertThat(d.model()).isEqualTo("std-m");
        assertThat(d.attempts()).isEqualTo(1);
        assertThat(d.tokensIn()).isEqualTo(12);
        verify(writer).release(any(), eq(false));
    }

    @Test
    void malformedCallThrows() {
        LlmCall bad = new LlmCall(null, Role.GUEST, List.of(), null, null, null);
        assertThatThrownBy(() -> gateway(props()).run(feature(10), bad))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gateway(props()).run(null, guestCall("x")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesInsideATransactionEvenWhenDisabled() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        assertThatThrownBy(() -> gateway(props(false, false, 2, 30_000, 45_000, 5, 2))
            .run(feature(10), guestCall("x"))).isInstanceOf(IllegalStateException.class);
        assertThat(stub.requests()).isEmpty();
    }

    @Test
    void disabledReturnsFallbackWithoutRowOrRequest() {
        LlmResult<String> result = gateway(props(false, false, 2, 30_000, 45_000, 5, 2))
            .run(feature(10), guestCall("hi"));
        assertThat(result.code()).isEqualTo(LlmErrorCode.DISABLED);
        assertThat(result.value()).isEqualTo("FALLBACK");
        assertThat(stub.requests()).isEmpty();
        verify(writer, never()).write(any(), any(), any(), anyBoolean(), any(), any());
        verify(writer, never()).reserve(any(), anyInt());
    }

    @Test
    void roleDenied() {
        LlmResult<String> result = gateway(props()).run(feature(10), call(UUID.randomUUID(), Role.HOST, "hi"));
        assertThat(result.code()).isEqualTo(LlmErrorCode.ROLE_DENIED);
        assertThat(stub.requests()).isEmpty();
        verify(writer, never()).write(any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void mockModeSkipsTheUpstream() {
        LlmResult<String> result = gateway(props(true, true, 2, 30_000, 45_000, 5, 2))
            .run(feature(10), guestCall("hi"));
        assertThat(result.ok()).isTrue();
        assertThat(result.value()).isEqualTo("mock reply");
        assertThat(stub.requests()).isEmpty();
        verifyRow(null, true);
        assertThat(lastDetails().model()).isEqualTo("mock");
        assertThat(lastDetails().attempts()).isZero();
    }

    @Test
    void perUserPerMinuteLimit() {
        stub.script(completion("one", "stop"));
        LlmGateway gw = gateway(props());
        UUID user = UUID.randomUUID();
        assertThat(gw.run(feature(1), call(user, Role.GUEST, "a")).ok()).isTrue();
        assertThat(gw.run(feature(1), call(user, Role.GUEST, "b")).code()).isEqualTo(LlmErrorCode.USER_THROTTLED);
        assertThat(stub.requests()).hasSize(1);
    }

    @Test
    void inFlightLimitIsPerUser() throws Exception {
        stub.script(delayed(200, completionBody("slow"), 700));
        LlmGateway gw = gateway(props(true, false, 0, 30_000, 45_000, 5, 1));
        UUID user = UUID.randomUUID();
        CompletableFuture<LlmResult<String>> first =
            CompletableFuture.supplyAsync(() -> gw.run(feature(10), call(user, Role.GUEST, "a")), pool);
        Thread.sleep(300);
        assertThat(gw.run(feature(10), call(user, Role.GUEST, "b")).code()).isEqualTo(LlmErrorCode.USER_THROTTLED);
        assertThat(gw.run(feature(10), guestCall("other user")).code()).isNotEqualTo(LlmErrorCode.USER_THROTTLED);
        first.get();
    }

    private static String completionBody(String text) {
        return "{\"choices\":[{\"message\":{\"content\":\"" + text + "\"},\"finish_reason\":\"stop\"}]}";
    }

    @Test
    void emptyInputIsBlockedWithARejectedRow() {
        LlmResult<String> result = gateway(props()).run(feature(10), guestCall("\u0000\u0007  "));
        assertThat(result.code()).isEqualTo(LlmErrorCode.BLOCKED_INPUT);
        assertThat(stub.requests()).isEmpty();
        verifyRow(LlmErrorCode.BLOCKED_INPUT, false);
        verify(writer, never()).reserve(any(), anyInt());
    }

    @Test
    void dailyCapIsARejectedRow() {
        when(writer.reserve(any(), anyInt())).thenReturn(CapResult.DAILY_CAP);
        LlmResult<String> result = gateway(props()).run(feature(10), guestCall("hi"));
        assertThat(result.code()).isEqualTo(LlmErrorCode.DAILY_CAP);
        verifyRow(LlmErrorCode.DAILY_CAP, false);
        assertThat(stub.requests()).isEmpty();
    }

    @Test
    void failedCapCountFailsClosedWithoutARow() {
        when(writer.reserve(any(), anyInt())).thenReturn(CapResult.COUNT_FAILED);
        LlmResult<String> result = gateway(props()).run(feature(10), guestCall("hi"));
        assertThat(result.code()).isEqualTo(LlmErrorCode.AUDIT_FAILED);
        assertThat(stub.requests()).isEmpty();
        verify(writer, never()).write(any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void serverErrorsRetryTwiceWithBackoffThenReturnServer() {
        stub.script(reply(503, "{}"));
        LlmResult<String> result = gateway(props()).run(feature(10), guestCall("hi"));
        assertThat(result.code()).isEqualTo(LlmErrorCode.SERVER);
        assertThat(stub.requests()).hasSize(3);
        assertThat(sleeps).containsExactly(500L, 1000L);
        verifyRow(LlmErrorCode.SERVER, true);
        assertThat(lastDetails().attempts()).isEqualTo(3);
    }

    @Test
    void rateLimitThenSuccessReportsTwoAttempts() {
        stub.script(reply(429, "{\"error\":\"slow down, rate limit\"}"), completion("fine", "stop"));
        LlmResult<String> result = gateway(props()).run(feature(10), guestCall("hi"));
        assertThat(result.ok()).isTrue();
        assertThat(lastDetails().attempts()).isEqualTo(2);
        assertThat(sleeps).containsExactly(500L);
    }

    @Test
    void clientErrorsAreNotRetried() {
        stub.script(reply(401, "{}"));
        assertThat(gateway(props()).run(feature(10), guestCall("hi")).code()).isEqualTo(LlmErrorCode.AUTH);
        assertThat(stub.requests()).hasSize(1);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void budgetExhaustionBlocksLaterCallsWithoutRequests() {
        stub.script(reply(429, "{\"error\":\"monthly budget exhausted\"}"));
        LlmGateway gw = gateway(props());
        LlmResult<String> first = gw.run(feature(10), guestCall("a"));
        assertThat(first.code()).isEqualTo(LlmErrorCode.BUDGET_EXHAUSTED);
        verifyRow(LlmErrorCode.BUDGET_EXHAUSTED, true);
        LlmResult<String> second = gw.run(feature(10), guestCall("b"));
        assertThat(second.code()).isEqualTo(LlmErrorCode.BUDGET_EXHAUSTED);
        assertThat(stub.requests()).hasSize(1);
        verify(writer, times(1)).write(any(), any(), eq(LlmErrorCode.BUDGET_EXHAUSTED), eq(false), any(), any());
    }

    @Test
    void breakerOpensAfterFailuresFromThreeUsers() {
        stub.script(reply(500, "{}"));
        LlmGateway gw = gateway(props(true, false, 0, 30_000, 45_000, 3, 2));
        for (int i = 0; i < 3; i++) {
            assertThat(gw.run(feature(10), guestCall("x")).code()).isEqualTo(LlmErrorCode.SERVER);
        }
        assertThat(gw.run(feature(10), guestCall("y")).code()).isEqualTo(LlmErrorCode.CIRCUIT_OPEN);
        assertThat(stub.requests()).hasSize(3);
        verifyRow(LlmErrorCode.CIRCUIT_OPEN, false);
    }

    @Test
    void breakerIsRecheckedBeforeEveryRetry() {
        stub.script(reply(500, "{}"));
        LlmGateway gw = gateway(props(true, false, 5, 30_000, 45_000, 3, 2));
        for (int i = 0; i < 2; i++) {
            gw.run(feature(10), guestCall("x"));
        }
        long before = stub.requests().size();
        LlmResult<String> third = gw.run(feature(10), guestCall("z"));
        assertThat(third.code()).isEqualTo(LlmErrorCode.SERVER);
        assertThat(stub.requests().size() - before).isLessThan(6);
    }

    @Test
    void refusalAndUnsafeOutputAreInvalid() {
        stub.script(completion("{\\\"refusal\\\":true}", "stop"));
        assertThat(gateway(props()).run(feature(10), guestCall("hi")).code()).isEqualTo(LlmErrorCode.REFUSED);
        stub.script(completion("see https://evil.test/x now", "stop"));
        assertThat(gateway(props()).run(feature(10), guestCall("hi")).code()).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        stub.script(completion("cut off", "length"));
        assertThat(gateway(props()).run(feature(10), guestCall("hi")).code()).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
    }

    @Test
    void unwritableRowOfAnOkCallBecomesAuditFailedAndStaysCounted() {
        stub.script(completion("fine", "stop"));
        when(writer.write(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(false);
        LlmResult<String> result = gateway(props()).run(feature(10), guestCall("hi"));
        assertThat(result.ok()).isFalse();
        assertThat(result.code()).isEqualTo(LlmErrorCode.AUDIT_FAILED);
        assertThat(result.value()).isEqualTo("FALLBACK");
        verify(writer).release(any(), eq(true));
    }

    @Test
    void deadlineEndsASlowAttemptAsTimeout() {
        stub.script(delayed(200, completionBody("late"), 3_000));
        LlmResult<String> result = gateway(props(true, false, 2, 1_000, 1_000, 5, 2))
            .run(feature(10), guestCall("hi"));
        assertThat(result.code()).isEqualTo(LlmErrorCode.TIMEOUT);
        assertThat(sleeps).isEmpty();
    }

    @Test
    void keyNeverAppearsInTheRequestBody() {
        stub.script(completion("fine", "stop"));
        gateway(props()).run(feature(10), guestCall("hello"));
        assertThat(stub.requests().get(0).body()).doesNotContain(KEY).doesNotContain("clsk_");
        assertThat(stub.requests().get(0).headers().get("authorization")).isEqualTo("Bearer " + KEY);
    }

    @Test
    void throwingFallbackPropagatesAfterReleasingTheReservation() {
        AiFeature<String> bad = new AiFeature<>("guest.helper", Set.of(Role.GUEST), ModelTier.STANDARD, "p", 200, 0.2,
            1, 10, OutputSpec.text(500, false), call -> {
                throw new IllegalStateException("feature bug");
            }, "m");
        stub.script(reply(401, "{}"));
        assertThatThrownBy(() -> gateway(props()).run(bad, guestCall("hi")))
            .isInstanceOf(IllegalStateException.class).hasMessage("feature bug");
        verify(writer).release(any(), eq(false));
    }
}
