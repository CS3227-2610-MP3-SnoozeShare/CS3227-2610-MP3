package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

/** The prod profile fails fast when a required variable is missing (E17, E18). */
class ProdProfileTest {

    @ParameterizedTest
    @ValueSource(strings = {"SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD"})
    void missingDatasourceVariable(String omitted) {
        assumeTrue(System.getenv(omitted) == null, omitted + " is set in this machine's environment");
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")) {
            postgres.start();
            List<String> args = new ArrayList<>(List.of(
                "--SPRING_DATASOURCE_URL=" + postgres.getJdbcUrl(),
                "--SPRING_DATASOURCE_USERNAME=" + postgres.getUsername(),
                "--SPRING_DATASOURCE_PASSWORD=" + postgres.getPassword(),
                "--SNOOZESHARE_CORS_ALLOWED_ORIGIN=http://localhost:3000",
                "--SNOOZESHARE_SECURITY_ENABLED=true",
                "--FIREBASE_ENABLED=true",
                "--FIREBASE_PROJECT_ID=test-project",
                "--SNOOZESHARE_HOST_REGISTRATION_CODE=host-code",
                "--SNOOZESHARE_AGENT_REGISTRATION_CODE=agent-code",
                "--GOOGLE_APPLICATION_CREDENTIALS=/tmp/test-service-account.json"));
            args.removeIf(arg -> arg.startsWith("--" + omitted + "="));
            assertThatThrownBy(() -> new SpringApplicationBuilder(SnoozeShareApplication.class)
                .profiles("prod")
                .web(WebApplicationType.NONE)
                .run(args.toArray(String[]::new)))
                .satisfies(error -> assertThat(messageChain(error)).contains(omitted));
        }
    }

    @Test
    void missingCorsOrigin() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")) {
            postgres.start();
            assumeTrue(System.getenv("SNOOZESHARE_CORS_ALLOWED_ORIGIN") == null,
                "SNOOZESHARE_CORS_ALLOWED_ORIGIN is set in this machine's environment");
            assertThatThrownBy(() -> new SpringApplicationBuilder(SnoozeShareApplication.class)
                .profiles("prod")
                .web(WebApplicationType.NONE)
                .run("--SPRING_DATASOURCE_URL=" + postgres.getJdbcUrl(),
                    "--SPRING_DATASOURCE_USERNAME=" + postgres.getUsername(),
                    "--SPRING_DATASOURCE_PASSWORD=" + postgres.getPassword(),
                    "--SNOOZESHARE_SECURITY_ENABLED=true",
                    "--FIREBASE_ENABLED=true",
                    "--FIREBASE_PROJECT_ID=test-project",
                    "--SNOOZESHARE_HOST_REGISTRATION_CODE=host-code",
                    "--SNOOZESHARE_AGENT_REGISTRATION_CODE=agent-code",
                    "--GOOGLE_APPLICATION_CREDENTIALS=/tmp/test-service-account.json"))
                .satisfies(error -> assertThat(messageChain(error)).contains("SNOOZESHARE_CORS_ALLOWED_ORIGIN"));
        }
    }

    private static String messageChain(Throwable error) {
        StringBuilder chain = new StringBuilder();
        for (Throwable t = error; t != null; t = t.getCause()) {
            chain.append(t.getMessage()).append('\n');
        }
        return chain.toString();
    }
}
