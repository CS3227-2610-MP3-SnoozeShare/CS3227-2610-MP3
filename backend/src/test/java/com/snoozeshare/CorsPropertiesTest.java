package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import com.snoozeshare.config.CorsProperties;

/** Validation of the allowed origin (E16). */
class CorsPropertiesTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(Config.class);

    @ParameterizedTest
    @ValueSource(strings = {"", "*", "http://localhost:3000/", "localhost:3000", "http://*.example.com"})
    void invalidOriginsFailStartup(String origin) {
        runner.withPropertyValues("snoozeshare.cors.allowed-origin=" + origin).run(context -> {
            assertThat(context).hasFailed();
            Throwable failure = context.getStartupFailure();
            StringBuilder chain = new StringBuilder();
            for (Throwable t = failure; t != null; t = t.getCause()) {
                chain.append(t.getMessage()).append('\n');
            }
            assertThat(chain).contains("snoozeshare.cors.allowed-origin");
        });
    }

    @Test
    void missingOriginFailsStartup() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:3000", "https://snoozeshare.vercel.app"})
    void validOriginsPass(String origin) {
        runner.withPropertyValues("snoozeshare.cors.allowed-origin=" + origin).run(context ->
            assertThat(context.getBean(CorsProperties.class).allowedOrigin()).isEqualTo(origin));
    }

    @Configuration
    @EnableConfigurationProperties(CorsProperties.class)
    static class Config { }
}
