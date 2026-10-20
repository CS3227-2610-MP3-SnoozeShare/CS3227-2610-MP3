package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.web.server.LocalServerPort;

/** Health endpoint behaviour (E6, E7, E8, E9). */
class HealthEndpointTest extends PostgresIntegrationTest {

    private static final String UP_BODY = "{\"status\":\"UP\"}";
    private static final String DOWN_BODY = "{\"status\":\"DOWN\"}";

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    private HttpResponse<String> get(String path, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .timeout(Duration.ofSeconds(8));
        if (headers.length > 0) {
            request.headers(headers);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void awaitHealth(int expectedStatus, String expectedBody) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        HttpResponse<String> last = get("/actuator/health");
        while (last.statusCode() != expectedStatus && System.nanoTime() < deadline) {
            Thread.sleep(500);
            last = get("/actuator/health");
        }
        assertThat(last.statusCode()).isEqualTo(expectedStatus);
        assertThat(last.body()).isEqualTo(expectedBody);
    }

    @Test
    void upWhenDatabaseReachable() throws Exception {
        HttpResponse<String> response = get("/actuator/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(UP_BODY);
    }

    static Stream<String> otherActuatorPaths() {
        return Stream.of("/actuator", "/actuator/info", "/actuator/env", "/actuator/beans");
    }

    @ParameterizedTest
    @MethodSource("otherActuatorPaths")
    void otherActuatorPathsAre404(String path) throws Exception {
        assertThat(get(path).statusCode()).isEqualTo(404);
    }

    @Test
    void queryParametersDoNotAddDetails() throws Exception {
        assertThat(get("/actuator/health?show-details=always").body()).isEqualTo(UP_BODY);
        assertThat(get("/actuator/health?details=true").body()).isEqualTo(UP_BODY);
        assertThat(get("/actuator/health", "Authorization", "Bearer x").body()).isEqualTo(UP_BODY);
    }

    @Test
    void downThenUpWhenDatabaseLoginBlocked() throws Exception {
        try (Connection admin = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
            Statement statement = admin.createStatement()) {
            block(statement);
            try {
                awaitHealth(503, DOWN_BODY);
            } finally {
                statement.execute("ALTER ROLE " + POSTGRES.getUsername() + " LOGIN");
            }
            awaitHealth(200, UP_BODY);
        }
    }

    private static void block(Statement statement) throws SQLException {
        statement.execute("ALTER ROLE " + POSTGRES.getUsername() + " NOLOGIN");
        statement.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity "
            + "WHERE datname = current_database() AND pid <> pg_backend_pid()");
    }
}
