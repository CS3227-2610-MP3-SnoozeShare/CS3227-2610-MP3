package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Health body stays exactly {"status":"UP"} on a platform Spring detects as Kubernetes (E6).
 *
 * <p>Render runs on Kubernetes, where Spring Boot enables liveness and readiness probes and adds a
 * "groups" field to the health body; the live dev deployment returned it.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.main.cloud-platform=kubernetes")
class HealthOnKubernetesTest extends PostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void healthBodyHasNoGroups() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health"))
            .timeout(Duration.ofSeconds(8))
            .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"status\":\"UP\"}");
    }
}
