package com.snoozeshare.service.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** E82 validates property boundaries and names the offending key. */
class AssistantPropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Config.class);

    @EnableConfigurationProperties(AssistantProperties.class)
    static class Config { }

    @Test
    void defaultsAndDisabledBindingE82() {
        runner.run(context -> assertThat(context.getBean(AssistantProperties.class))
            .isEqualTo(new AssistantProperties(true, 3, 2, 50, 10)));
        runner.withPropertyValues("snoozeshare.assistant.enabled=false").run(context ->
            assertThat(context.getBean(AssistantProperties.class).enabled()).isFalse());
    }

    @ParameterizedTest
    @CsvSource({"max-rounds,0,1 to 5", "max-rounds,6,1 to 5", "threads,0,1 to 4", "threads,5,1 to 4",
        "queue,0,1 to 500", "queue,501,1 to 500", "stale-after-minutes,1,2 to 120",
        "stale-after-minutes,121,2 to 120"})
    void invalidStartupNamesKeyAndRangeE82(String key, String value, String range) {
        runner.withPropertyValues("snoozeshare.assistant." + key + "=" + value).run(context -> {
            assertThat(context).hasFailed();
            StringBuilder messages = new StringBuilder();
            for (Throwable error = context.getStartupFailure(); error != null; error = error.getCause()) {
                messages.append(error.getMessage());
            }
            assertThat(messages.toString()).contains("snoozeshare.assistant." + key, range);
        });
    }

    @Test
    void boundaryValuesBindE82() {
        for (Map<String, String> settings : java.util.List.of(
                Map.of("max-rounds", "1", "threads", "1", "queue", "1", "stale-after-minutes", "2"),
                Map.of("max-rounds", "5", "threads", "4", "queue", "500", "stale-after-minutes", "120"))) {
            runner.withPropertyValues(settings.entrySet().stream()
                .map(entry -> "snoozeshare.assistant." + entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new)).run(context -> assertThat(context).hasNotFailed());
        }
    }
}
