package com.snoozeshare.ai.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.ai.LlmErrorCode;
import com.snoozeshare.ai.client.SoclaasClient.Message;
import com.snoozeshare.ai.client.SoclaasClient.Request;
import com.snoozeshare.ai.config.LlmProperties;
import com.snoozeshare.ai.testsupport.StubSoclaasServer;
import com.snoozeshare.ai.testsupport.StubSoclaasServer.Captured;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** E21, E22 (client side), E23, E37, E46, E60 and the 429 rules of E18 and E19, against an in-process stub. */
class SoclaasClientTest {

    private static final String KEY = "clsk_abcd1234_fakekey";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private StubSoclaasServer stub;

    @BeforeEach
    void start() throws IOException {
        stub = new StubSoclaasServer();
    }

    @AfterEach
    void stop() {
        stub.close();
    }

    private static LlmProperties props(String baseUrl, String... extra) {
        Map<String, String> map = new HashMap<>();
        map.put("snoozeshare.ai.enabled", "true");
        map.put("snoozeshare.ai.base-url", baseUrl);
        map.put("snoozeshare.ai.api-key", KEY);
        for (int i = 0; i < extra.length; i += 2) {
            map.put("snoozeshare.ai." + extra[i], extra[i + 1]);
        }
        return new Binder(new MapConfigurationPropertySource(map)).bind("snoozeshare.ai", LlmProperties.class).get();
    }

    private static Request request(boolean json) {
        return new Request("qwen3.5:9b", List.of(new Message("system", "be brief"), new Message("user", "hello")),
            300, 0.2, json);
    }

    private AttemptResult.Failure failureOf(StubSoclaasServer.Reply reply) {
        stub.script(reply);
        AttemptResult result = new SoclaasClient(props(stub.baseUrl())).attempt(request(false), TIMEOUT);
        assertThat(result).isInstanceOf(AttemptResult.Failure.class);
        return (AttemptResult.Failure) result;
    }

    @Test
    void requestShape() throws IOException {
        stub.script(StubSoclaasServer.completion("hi", "stop"), StubSoclaasServer.completion("{}", "stop"));
        AttemptResult first = new SoclaasClient(props(stub.baseUrl())).attempt(request(true), TIMEOUT);
        assertThat(first).isEqualTo(new AttemptResult.Success("hi", "stop", 12, 7));

        Captured plain = stub.requests().get(0);
        assertThat(plain.method()).isEqualTo("POST");
        assertThat(plain.path()).isEqualTo("/v1/chat/completions");
        assertThat(plain.protocol()).isEqualTo("HTTP/1.1");
        assertThat(plain.headers().get("authorization")).isEqualTo("Bearer " + KEY);
        assertThat(plain.body()).doesNotContain(KEY);
        JsonNode body = MAPPER.readTree(plain.body());
        assertThat(body.path("model").asText()).isEqualTo("qwen3.5:9b");
        assertThat(body.path("max_tokens").asInt()).isEqualTo(300);
        assertThat(body.path("temperature").asDouble()).isEqualTo(0.2);
        assertThat(body.path("messages")).hasSize(2);
        assertThat(body.path("messages").get(1).path("role").asText()).isEqualTo("user");
        assertThat(body.has("stream")).isFalse();
        assertThat(body.has("response_format")).isFalse();
        assertThat(body.path("reasoning_effort").asText()).isEqualTo("none");

        LlmProperties both = props(stub.baseUrl(), "response-format-json", "true", "reasoning-effort", "low");
        new SoclaasClient(both).attempt(request(true), TIMEOUT);
        JsonNode withOptions = MAPPER.readTree(stub.requests().get(1).body());
        assertThat(withOptions.path("response_format").path("type").asText()).isEqualTo("json_object");
        assertThat(withOptions.path("reasoning_effort").asText()).isEqualTo("low");
        assertThat(stub.requests().get(1).body()).doesNotContain(KEY);

        stub.script(StubSoclaasServer.completion("t", "stop"));
        new SoclaasClient(both).attempt(request(false), TIMEOUT);
        assertThat(MAPPER.readTree(stub.requests().get(2).body()).has("response_format")).isFalse();
    }

    @Test
    void successReadsContentFinishReasonAndMissingUsage() {
        stub.script(StubSoclaasServer.reply(200,
            "{\"choices\":[{\"message\":{\"content\":\"cut\"},\"finish_reason\":\"length\"}]}"));
        AttemptResult result = new SoclaasClient(props(stub.baseUrl())).attempt(request(false), TIMEOUT);
        assertThat(result).isEqualTo(new AttemptResult.Success("cut", "length", 0, 0));
    }

    @ParameterizedTest
    @CsvSource({
        "400, BAD_REQUEST", "404, BAD_REQUEST", "409, BAD_REQUEST", "413, BAD_REQUEST", "422, BAD_REQUEST",
        "401, AUTH", "403, FORBIDDEN", "500, SERVER", "503, SERVER", "599, SERVER", "204, INVALID_OUTPUT",
        "201, INVALID_OUTPUT"
    })
    void statusClasses(int status, LlmErrorCode expected) {
        AttemptResult.Failure failure = failureOf(StubSoclaasServer.reply(status, "{\"error\":\"x\"}"));
        assertThat(failure.code()).isEqualTo(expected);
        assertThat(failure.httpStatus()).isEqualTo(status);
        assertThat(failure.budget()).isFalse();
        assertThat(stub.requests()).hasSize(1);
    }

    @Test
    void redirectIsNeverFollowed() throws IOException {
        try (StubSoclaasServer target = new StubSoclaasServer()) {
            AttemptResult.Failure failure = failureOf(StubSoclaasServer.replyWithHeader(302, "Location",
                target.baseUrl() + "/chat/completions", ""));
            assertThat(failure.code()).isEqualTo(LlmErrorCode.BAD_REQUEST);
            assertThat(target.requests()).isEmpty();
            assertThat(stub.requests()).hasSize(1);
        }
    }

    @Test
    void unusableBodiesAreInvalidOutput() {
        for (String body : List.of("not json", "", "{}", "{\"choices\":[]}",
            "{\"choices\":[{\"message\":{\"content\":\"  \"}}]}", "{\"choices\":[{\"message\":{\"content\":null}}]}")) {
            assertThat(failureOf(StubSoclaasServer.reply(200, body)).code())
                .as(body).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        }
    }

    @Test
    void connectionRefused() throws IOException {
        String dead;
        try (StubSoclaasServer gone = new StubSoclaasServer()) {
            dead = gone.baseUrl();
        }
        AttemptResult result = new SoclaasClient(props(dead)).attempt(request(false), TIMEOUT);
        assertThat(result).isInstanceOf(AttemptResult.Failure.class);
        AttemptResult.Failure failure = (AttemptResult.Failure) result;
        assertThat(failure.code()).isEqualTo(LlmErrorCode.NETWORK);
        assertThat(failure.httpStatus()).isZero();
    }

    @Test
    void timeout() {
        stub.script(StubSoclaasServer.delayed(200, "{}", 3_000));
        long start = System.nanoTime();
        AttemptResult result = new SoclaasClient(props(stub.baseUrl())).attempt(request(false),
            Duration.ofMillis(300));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(result).isInstanceOf(AttemptResult.Failure.class);
        assertThat(((AttemptResult.Failure) result).code()).isEqualTo(LlmErrorCode.TIMEOUT);
        assertThat(elapsedMs).isLessThan(2_000);
    }

    @Test
    void bodySizeCaps() {
        String padding = "x".repeat(1_048_576);
        long start = System.nanoTime();
        AttemptResult.Failure big429 = failureOf(StubSoclaasServer.reply(429,
            "{\"error\":\"Daily quota exceeded\",\"pad\":\"" + padding + "\"}"));
        assertThat(big429.code()).isEqualTo(LlmErrorCode.RATE_LIMITED);
        assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(5_000);

        String content = "y".repeat(300_000);
        assertThat(failureOf(StubSoclaasServer.completion(content, "stop")).code())
            .isEqualTo(LlmErrorCode.INVALID_OUTPUT);

        String atLimit = "z".repeat(200_000);
        stub.script(StubSoclaasServer.completion(atLimit, "stop"));
        AttemptResult ok = new SoclaasClient(props(stub.baseUrl())).attempt(request(false), TIMEOUT);
        assertThat(ok).isInstanceOf(AttemptResult.Success.class);
    }

    @Test
    void errorBodyNeverLogged() {
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            String secret = "SUBMITTED-PROMPT-MARKER";
            List<AttemptResult> results = new ArrayList<>();
            SoclaasClient client = new SoclaasClient(props(stub.baseUrl()));
            for (int status : new int[] {400, 401, 403, 429, 500, 200}) {
                stub.script(StubSoclaasServer.reply(status, "{\"error\":\"" + secret + " " + KEY + "\"}"));
                results.add(client.attempt(request(false), TIMEOUT));
            }
            for (AttemptResult r : results) {
                assertThat(r.toString()).doesNotContain(secret).doesNotContain(KEY);
            }
            for (ILoggingEvent event : appender.list) {
                assertThat(event.getFormattedMessage()).doesNotContain(secret).doesNotContain(KEY);
            }
            assertThat(request(false).toString()).doesNotContain("hello").doesNotContain("be brief");
        } finally {
            root.detachAppender(appender);
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "{\"error\":\"Daily quota exceeded\"}|true",
        "{\"error\":{\"message\":\"Monthly budget used up\"}}|true",
        "{\"error\":\"Out of credits\"}|true",
        "{\"error\":\"rate limit exceeded\"}|false",
        "{\"error\":\"Rate-limit quota exceeded\"}|false",
        "{\"error\":\"separate generated text\"}|false",
        "{\"message\":\"quota\"}|false",
        "{\"error\":42}|false",
        "plain quota text|false"
    })
    void budgetRule(String body, boolean budget) {
        AttemptResult.Failure failure = failureOf(StubSoclaasServer.reply(429, body));
        assertThat(failure.budget()).isEqualTo(budget);
        assertThat(failure.code()).isEqualTo(budget ? LlmErrorCode.BUDGET_EXHAUSTED : LlmErrorCode.RATE_LIMITED);
    }

    @Test
    void budgetRuleIgnoresWordsPastTheFirst200Characters() {
        String body = "{\"error\":\"" + "a ".repeat(120) + "quota\"}";
        assertThat(failureOf(StubSoclaasServer.reply(429, body)).budget()).isFalse();
    }

    @Test
    void emptyBodyIs429RateLimited() {
        AttemptResult.Failure failure = failureOf(StubSoclaasServer.reply(429, ""));
        assertThat(failure.code()).isEqualTo(LlmErrorCode.RATE_LIMITED);
        assertThat(failure.retryAfterSeconds()).isNull();
    }

    @Test
    void retryAfterSecondsParsedHttpDateIgnored() {
        assertThat(failureOf(StubSoclaasServer.replyWithHeader(429, "Retry-After", "30", "rate limit"))
            .retryAfterSeconds()).isEqualTo(30);
        assertThat(failureOf(StubSoclaasServer.replyWithHeader(429, "Retry-After", "2", "")).retryAfterSeconds())
            .isEqualTo(2);
        assertThat(failureOf(StubSoclaasServer.replyWithHeader(429, "Retry-After", "Wed, 21 Oct 2026 07:28:00 GMT",
            "")).retryAfterSeconds()).isNull();
        assertThat(failureOf(StubSoclaasServer.replyWithHeader(429, "Retry-After", "-5", "")).retryAfterSeconds())
            .isNull();
    }

    @Test
    void listModelsReadsIds() {
        stub.script(StubSoclaasServer.reply(200, "{\"data\":[{\"id\":\"a\"},{\"id\":\"b\"},{\"x\":1}]}"));
        SoclaasClient.ModelsResult result = new SoclaasClient(props(stub.baseUrl())).listModels(TIMEOUT);
        assertThat(result.ok()).isTrue();
        assertThat(result.ids()).containsExactly("a", "b");
        Captured captured = stub.requests().get(0);
        assertThat(captured.method()).isEqualTo("GET");
        assertThat(captured.path()).isEqualTo("/v1/models");
        assertThat(captured.headers().get("authorization")).isEqualTo("Bearer " + KEY);
    }

    @Test
    void listModelsFailuresCarryStatusOnly() {
        stub.script(StubSoclaasServer.reply(503, "down"));
        SoclaasClient.ModelsResult failed = new SoclaasClient(props(stub.baseUrl())).listModels(TIMEOUT);
        assertThat(failed.ok()).isFalse();
        assertThat(failed.status()).isEqualTo(503);

        stub.script(StubSoclaasServer.reply(200, "not json"));
        SoclaasClient.ModelsResult garbage = new SoclaasClient(props(stub.baseUrl())).listModels(TIMEOUT);
        assertThat(garbage.ok()).isFalse();
    }
}
