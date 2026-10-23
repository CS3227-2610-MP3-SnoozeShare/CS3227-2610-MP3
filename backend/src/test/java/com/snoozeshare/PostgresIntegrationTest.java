package com.snoozeshare;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for tests that need the full application on a real Postgres 17 container.
 *
 * <p>One container is started for the whole JVM (singleton pattern). A per-class {@code @Container}
 * restarts on a new port for each subclass while Spring reuses the cached context, which then points at
 * a dead port.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "snoozeshare.assistant.enabled=false")
public abstract class PostgresIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }
}
