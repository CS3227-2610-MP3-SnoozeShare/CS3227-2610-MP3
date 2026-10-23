package com.snoozeshare.ai;

import static com.snoozeshare.ai.testsupport.StubSoclaasServer.completion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import com.snoozeshare.service.assistant.ClarifyFeatureConfiguration;
import com.snoozeshare.service.assistant.ClarifyOutput;

import jakarta.validation.Validation;

/**
 * The real gateway, validator and sanitizer with a stub transport: what the ticket.clarify feature sends to the
 * model and what it accepts back (E1, E2, E7, E9, E37, E72). It lives in {@code ai} to reach the gateway internals.
 */
class ClarifyGatewayContractTest {
    private static final String KEY = "clsk_abcdefgh_fakekey";
    private static final String GOOD = "{\"enough\":false,\"questions\":[\"When did it start?\"],"
        + "\"summary\":\"The Guest reports a problem.\",\"timeline\":[],\"claims\":[],\"evidence\":[],"
        + "\"missing\":[\"Start date\"],\"hostPoints\":[]}";

    private StubSoclaasServer stub;
    private final AiFeature<ClarifyOutput> feature = new ClarifyFeatureConfiguration().clarifyFeature();

    @BeforeEach
    void setUp() throws IOException {
        stub = new StubSoclaasServer();
    }

    @AfterEach
    void tearDown() {
        stub.close();
    }

    private LlmGateway gateway() {
        AiAuditWriter writer = mock(AiAuditWriter.class);
        when(writer.reserve(any(), anyInt())).thenReturn(CapResult.RESERVED);
        when(writer.write(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(true);
        LlmProperties p = new LlmProperties(true, false, stub.baseUrl(), KEY, "fast-m", "std-m", "strong-m", 55, 4,
            20, 10_000, 30_000, 45_000, 0, 5, 30_000, 900_000, 2, 200, 3_000, 20, 40_000, false, "");
        Clock clock = Clock.systemUTC();
        return new LlmGateway(p, writer, new PromptBuilder(),
            new OutputValidator(new ObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator()),
            new SoclaasClient(p), new LlmScheduler(p, clock), new UserThrottle(clock, p.perUserMaxInFlight()),
            new CircuitBreaker(clock, p.breakerFailures(), p.breakerOpenMs()),
            new BudgetBlock(clock, p.budgetBlockMs()), new Backoff(() -> 1.0), ms -> { }, clock);
    }

    private static LlmCall call(Turn... turns) {
        return new LlmCall(UUID.randomUUID(), Role.GUEST, List.of(turns), "Ticket status: OPEN", null, null);
    }

    private LlmResult<ClarifyOutput> run(String modelJson, Turn... turns) {
        stub.script(completion(modelJson.replace("\\", "\\\\").replace("\"", "\\\""), "stop"));
        return gateway().run(feature, call(turns));
    }

    private static Turn user(String text) {
        return new Turn(TurnRole.USER, text);
    }

    @Test
    void validReplyIsAcceptedAndNothingIsTruncatedByDefault() {
        var result = run(GOOD, user("Title: Dirty room\nDescription: It was dirty"));
        assertThat(result.ok()).isTrue();
        assertThat(result.value().questions()).containsExactly("When did it start?");
        assertThat(result.truncated()).isFalse();
    }

    @Test
    void fiveQuestionsAreRejectedE2() {
        String five = GOOD.replace("[\"When did it start?\"]", "[\"a?\",\"b?\",\"c?\",\"d?\",\"e?\"]");
        var result = run(five, user("Title: x\nDescription: y"));
        assertThat(result.ok()).isFalse();
        assertThat(result.code()).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
    }

    @Test
    void missingEnoughIsRejected() {
        var result = run(GOOD.replace("\"enough\":false,", ""), user("Title: x\nDescription: y"));
        assertThat(result.ok()).isFalse();
        assertThat(result.code()).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
    }

    @Test
    void linksInQuestionsOrDraftItemsAreRejectedE37() {
        var inQuestion = run(GOOD.replace("When did it start?", "See https://evil.example/pay"),
            user("Title: x\nDescription: y"));
        assertThat(inQuestion.ok()).isFalse();
        var inDraft = run(GOOD.replace("\"missing\":[\"Start date\"]", "\"missing\":[\"www.evil.example\"]"),
            user("Title: x\nDescription: y"));
        assertThat(inDraft.ok()).isFalse();
    }

    @Test
    void personalDataIsRedactedBeforeTheModelSeesItE7() {
        run(GOOD, user("Title: Hi\nDescription: I am Jane, jane@example.com, +65 9123 4567"));
        String sent = stub.requests().getFirst().body();
        assertThat(sent).doesNotContain("jane@example.com").doesNotContain("9123 4567");
        assertThat(sent).contains("[email]");
    }

    @Test
    void longTicketAndLongJoinedReplyAreTruncatedAndReportedE1E9() {
        var ticket = run(GOOD, user("Title: x\nDescription: " + "a".repeat(5000)));
        assertThat(ticket.ok()).isTrue();
        assertThat(ticket.truncated()).isTrue();
        var reply = run(GOOD, user("Title: x\nDescription: y"), new Turn(TurnRole.ASSISTANT, "1. When?"),
            user("b".repeat(2500)));
        assertThat(reply.truncated()).isTrue();
    }

    @Test
    void storedSupportEmailInAReplayedQuestionIsRedactedE72() {
        String stored = "1. Please email supporting documents to support@snoozeshare.com with the subject \""
            + UUID.randomUUID() + ": Supporting Documents\".";
        run(GOOD, user("Title: x\nDescription: y"), new Turn(TurnRole.ASSISTANT, stored), user("Done"));
        String sent = stub.requests().getFirst().body();
        assertThat(sent).doesNotContain("support@snoozeshare.com");
    }
}
