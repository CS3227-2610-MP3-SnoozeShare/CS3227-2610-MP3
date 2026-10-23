package com.snoozeshare.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import com.snoozeshare.PostgresIntegrationTest;

/** E39: with AI enabled and an unreachable service, the health body stays exactly {"status":"UP"}. */
@TestPropertySource(properties = {
    "snoozeshare.ai.enabled=true",
    "snoozeshare.ai.base-url=http://127.0.0.1:1/v1",
    "snoozeshare.ai.api-key=clsk_abcdefgh_fakekey"})
class AiHealthTest extends PostgresIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void healthIsUpWhenTheUpstreamIsUnreachable() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health"))
            .timeout(Duration.ofSeconds(8)).build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"status\":\"UP\"}");
    }
}
