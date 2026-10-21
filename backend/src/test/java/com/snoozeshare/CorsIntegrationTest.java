package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.web.server.LocalServerPort;


/** Exact-origin CORS on application endpoints and the actuator (E13, E14, E15). */
class CorsIntegrationTest extends PostgresIntegrationTest {

    private static final String ALLOWED = "http://localhost:3000";
    private static final String ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    private final HttpClient client = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    static Stream<String> paths() {
        return Stream.of("/actuator/health", "/test/cors");
    }

    private HttpResponse<String> preflight(String path, String origin, String method) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .header("Origin", origin)
            .header("Access-Control-Request-Method", method)
            .timeout(Duration.ofSeconds(8))
            .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @ParameterizedTest
    @MethodSource("paths")
    void configuredOriginIsAllowed(String path) throws Exception {
        HttpResponse<String> response = preflight(path, ALLOWED, "GET");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue(ALLOW_ORIGIN)).contains(ALLOWED);
    }

    @ParameterizedTest
    @MethodSource("paths")
    void otherOriginsAreRejected(String path) throws Exception {
        for (String origin : new String[] {"https://localhost:3000", "http://localhost:3001", "http://evil.example"}) {
            HttpResponse<String> response = preflight(path, origin, "GET");
            assertThat(response.statusCode()).as(origin).isEqualTo(403);
            assertThat(response.headers().firstValue(ALLOW_ORIGIN)).as(origin).isEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("paths")
    void putIsAllowedOnApplicationEndpointsOnly(String path) throws Exception {
        HttpResponse<String> response = preflight(path, ALLOWED, "PUT");
        if (path.startsWith("/test")) {
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue(ALLOW_ORIGIN)).contains(ALLOWED);
        } else {
            assertThat(response.statusCode()).isEqualTo(403);
        }
    }

    @org.junit.jupiter.api.Test
    void allowedOriginIsReturnedOnActualApplicationResponse() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/test/cors"))
            .header("Origin", ALLOWED)
            .timeout(Duration.ofSeconds(8))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("ok");
        assertThat(response.headers().firstValue(ALLOW_ORIGIN)).contains(ALLOWED);
    }
}
