package com.snoozeshare.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** E34 (ranges, URL rules, placeholders) and E35 (the key never prints) for {@link LlmProperties}. */
class LlmPropertiesTest {

    private static final String KEY = "clsk_ab_supersecretvalue";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(Config.class);

    @EnableConfigurationProperties(LlmProperties.class)
    static class Config { }

    private String failureOf(String... properties) {
        String[] holder = new String[1];
        runner.withPropertyValues(properties).run(context -> {
            assertThat(context).hasFailed();
            StringBuilder chain = new StringBuilder();
            for (Throwable t = context.getStartupFailure(); t != null; t = t.getCause()) {
                chain.append(t.getMessage()).append('\n');
            }
            holder[0] = chain.toString();
        });
        return holder[0];
    }

    @Test
    void defaultsBindWhenNothingIsSet() {
        runner.run(context -> {
            LlmProperties p = context.getBean(LlmProperties.class);
            assertThat(p.enabled()).isFalse();
            assertThat(p.mock()).isFalse();
            assertThat(p.maxRpm()).isEqualTo(55);
            assertThat(p.maxConcurrent()).isEqualTo(4);
            assertThat(p.maxQueue()).isEqualTo(20);
            assertThat(p.queueWaitMs()).isEqualTo(10_000);
            assertThat(p.requestTimeoutMs()).isEqualTo(30_000);
            assertThat(p.deadlineMs()).isEqualTo(45_000);
            assertThat(p.maxRetries()).isEqualTo(2);
            assertThat(p.dailyCapPerUser()).isEqualTo(200);
            assertThat(p.globalDailyCap()).isEqualTo(3_000);
            assertThat(p.priorityZeroReservePercent()).isEqualTo(20);
            assertThat(p.modelStandard()).isEqualTo("qwen3.5:9b");
            assertThat(p.reasoningEffort()).isEmpty();
            assertThat(p.responseFormatJson()).isFalse();
        });
    }

    @Test
    void someKeysOverrideTheRestKeepDefaults() {
        runner.withPropertyValues("snoozeshare.ai.max-rpm=10").run(context -> {
            assertThat(context.getBean(LlmProperties.class).maxRpm()).isEqualTo(10);
            assertThat(context.getBean(LlmProperties.class).maxConcurrent()).isEqualTo(4);
        });
    }

    @ParameterizedTest
    @CsvSource({
        "max-rpm,0", "max-rpm,91", "max-concurrent,1", "max-queue,0", "queue-wait-ms,99",
        "request-timeout-ms,999", "max-retries,6", "breaker-failures,2", "breaker-open-ms,999",
        "budget-block-ms,59999", "per-user-max-in-flight,0", "daily-cap-per-user,0", "global-daily-cap,100001",
        "priority-zero-reserve-percent,51", "max-prompt-chars,1999", "reasoning-effort,extreme",
        "model-fast,' '"
    })
    void outOfRangeValueNamesItsKey(String key, String value) {
        assertThat(failureOf("snoozeshare.ai." + key + "=" + value)).contains("snoozeshare.ai." + key);
    }

    @Test
    void deadlineBelowRequestTimeoutFails() {
        assertThat(failureOf("snoozeshare.ai.request-timeout-ms=30000", "snoozeshare.ai.deadline-ms=29999"))
            .contains("snoozeshare.ai.deadline-ms");
    }

    @ParameterizedTest
    @CsvSource({
        "http://example.com/v1", "https://example.com/v1/", "https://example.com", "https://example.com/api",
        "ftp://example.com/v1"
    })
    void badBaseUrlFailsWhenEnabled(String url) {
        assertThat(failureOf("snoozeshare.ai.enabled=true", "snoozeshare.ai.api-key=" + KEY,
            "snoozeshare.ai.base-url=" + url)).contains("snoozeshare.ai.base-url");
    }

    @ParameterizedTest
    @CsvSource({"https://soclaas.example/v1", "http://localhost:8089/v1", "http://127.0.0.1:9000/v1"})
    void goodBaseUrlPasses(String url) {
        runner.withPropertyValues("snoozeshare.ai.enabled=true", "snoozeshare.ai.api-key=" + KEY,
            "snoozeshare.ai.base-url=" + url).run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void blankKeyOrUrlFailsWhenEnabledAndNotMock() {
        assertThat(failureOf("snoozeshare.ai.enabled=true", "snoozeshare.ai.base-url=https://x.example/v1"))
            .contains("snoozeshare.ai.api-key");
        assertThat(failureOf("snoozeshare.ai.enabled=true", "snoozeshare.ai.api-key=" + KEY))
            .contains("snoozeshare.ai.base-url");
    }

    @Test
    void literalPlaceholderKeyCountsAsBlank() {
        assertThat(failureOf("snoozeshare.ai.enabled=true", "snoozeshare.ai.base-url=https://x.example/v1",
            "snoozeshare.ai.api-key=$" + "{SOCLAAS_API_KEY}")).contains("snoozeshare.ai.api-key");
    }

    @Test
    void disabledOrMockNeedsNoKeyOrUrl() {
        runner.withPropertyValues("snoozeshare.ai.api-key=$" + "{SOCLAAS_API_KEY}")
            .run(context -> assertThat(context).hasNotFailed());
        runner.withPropertyValues("snoozeshare.ai.enabled=true", "snoozeshare.ai.mock=true")
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void toStringHidesTheKey() {
        runner.withPropertyValues("snoozeshare.ai.enabled=true", "snoozeshare.ai.api-key=" + KEY,
            "snoozeshare.ai.base-url=https://x.example/v1").run(context -> {
                String text = context.getBean(LlmProperties.class).toString();
                assertThat(text).contains("apiKey=***").doesNotContain(KEY).doesNotContain("supersecret");
            });
    }
}
