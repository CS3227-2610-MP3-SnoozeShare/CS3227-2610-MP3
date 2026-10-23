package com.snoozeshare.ai;

import static com.snoozeshare.ai.testsupport.StubSoclaasServer.completion;
import static com.snoozeshare.ai.testsupport.StubSoclaasServer.reply;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.ai.audit.AiAuditWriter;
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.validation.Validation;

/** E35 and E44: across failure scenarios no log line holds the key, the prompt or an upstream body. */
class AiLogLeakTest {

    private static final String KEY = "clsk_abcdefgh_fakekey";
    private static final String PROMPT_MARKER = "PROMPTMARKER-9137";
    private static final String UPSTREAM_MARKER = "UPSTREAMMARKER-5521";

    private StubSoclaasServer stub;
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;
    private AiAuditWriter writer;

    @BeforeEach
    void setUp() throws IOException {
        stub = new StubSoclaasServer();
        writer = mock(AiAuditWriter.class);
        when(writer.reserve(any(), anyInt())).thenReturn(AiAuditWriter.CapResult.RESERVED);
        when(writer.write(any(), any(), any(), anyBoolean(), any(), any())).thenReturn(false);
        logger = (Logger) LoggerFactory.getLogger("com.snoozeshare.ai");
        logger.setLevel(Level.DEBUG);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        logger.setLevel(null);
        stub.close();
    }

    private LlmGateway gateway() {
        LlmProperties p = new LlmProperties(true, false, stub.baseUrl(), KEY, "fast-m", "std-m", "strong-m", 55, 4,
            20, 10_000, 30_000, 45_000, 1, 5, 30_000, 900_000, 2, 200, 3_000, 20, 40_000, false, "");
        Clock clock = Clock.systemUTC();
        return new LlmGateway(p, writer, new PromptBuilder(),
            new OutputValidator(new ObjectMapper(), Validation.buildDefaultValidatorFactory().getValidator()),
            new SoclaasClient(p), new LlmScheduler(p, clock), new UserThrottle(clock, 2),
            new CircuitBreaker(clock, 5, 30_000), new BudgetBlock(clock, 900_000), new Backoff(() -> 1.0),
            millis -> { }, clock);
    }

    private void run(StubSoclaasServer.Reply reply) {
        stub.script(reply);
        AiFeature<String> feature = new AiFeature<>("guest.helper", Set.of(Role.GUEST), ModelTier.STANDARD,
            "You help.", 200, 0.2, 1, 60, OutputSpec.text(500, false), call -> "FALLBACK", "mock");
        LlmCall call = new LlmCall(UUID.randomUUID(), Role.GUEST,
            List.of(new Turn(TurnRole.USER, PROMPT_MARKER + " help me")), null, null, null);
        gateway().run(feature, call);
    }

    @Test
    void noLogLineHoldsTheKeyThePromptOrAnUpstreamBody() {
        run(reply(401, "{\"error\":\"" + UPSTREAM_MARKER + " " + KEY + "\"}"));
        run(reply(500, "{\"error\":\"" + UPSTREAM_MARKER + "\"}"));
        run(reply(429, "{\"error\":\"budget " + UPSTREAM_MARKER + "\"}"));
        run(reply(400, "{\"error\":\"" + PROMPT_MARKER + " " + UPSTREAM_MARKER + "\"}"));
        run(reply(200, "not json " + UPSTREAM_MARKER));
        run(completion("see https://evil.test/" + UPSTREAM_MARKER + " " + KEY, "stop"));
        run(completion("fine " + UPSTREAM_MARKER, "stop"));
        assertThat(logs.list).isNotEmpty();
        for (ILoggingEvent event : logs.list) {
            String text = event.getFormattedMessage() + " " + event.getThrowableProxy();
            assertThat(text).doesNotContain(KEY).doesNotContain(PROMPT_MARKER).doesNotContain(UPSTREAM_MARKER);
        }
    }

    @Test
    void toStringsOfConfigurationAndRequestsHideSecrets() {
        LlmProperties p = new LlmProperties(true, false, stub.baseUrl(), KEY, "fast-m", "std-m", "strong-m", 55, 4,
            20, 10_000, 30_000, 45_000, 1, 5, 30_000, 900_000, 2, 200, 3_000, 20, 40_000, false, "");
        assertThat(p.toString()).doesNotContain(KEY);
        SoclaasClient.Request request = new SoclaasClient.Request("m",
            List.of(new SoclaasClient.Message("user", PROMPT_MARKER)), 10, 0.1, false);
        assertThat(request.toString()).doesNotContain(PROMPT_MARKER);
    }
}
