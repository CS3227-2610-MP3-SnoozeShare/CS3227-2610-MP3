package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** The application refuses to start when the database is unreachable (E10). */
class StartupFailureTest {

    @Test
    void failsFastWhenDatabaseUnreachable() {
        // A command-line argument: properties(...) has the lowest precedence and application-dev.yml would win.
        assertTimeoutPreemptively(Duration.ofSeconds(20), () ->
            assertThatThrownBy(() -> new SpringApplicationBuilder(SnoozeShareApplication.class)
                .profiles("dev")
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=jdbc:postgresql://localhost:1/snoozeshare"))
                .isInstanceOf(Exception.class));
    }
}
