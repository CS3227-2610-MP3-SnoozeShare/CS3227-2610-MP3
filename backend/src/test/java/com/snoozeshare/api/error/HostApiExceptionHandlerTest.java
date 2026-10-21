package com.snoozeshare.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.snoozeshare.auth.api.AuthErrorResponse;

class HostApiExceptionHandlerTest {

    private HostApiExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new HostApiExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/host/test");
    }

    @Test
    void mapsTypedConflictToStableSafeEnvelope() throws Exception {
        ResponseEntity<AuthErrorResponse> response = handler.handleHostApiException(
            new HostApiException(HttpStatus.CONFLICT, "STATE_CONFLICT"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getFirst("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getBody()).satisfies(body -> {
            assertThat(body.code()).isEqualTo("STATE_CONFLICT");
            assertThat(body.message()).isEqualTo("Request could not be completed");
            assertThat(body.requestId()).isNotBlank();
        });
    }

    @Test
    void mapsMalformedJsonToValidationError() throws Exception {
        ResponseEntity<AuthErrorResponse> response = handler.handleValidationException(
            new IllegalArgumentException("invalid input"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().requestId()).isNotBlank();
    }

    @Test
    void hidesUnexpectedExceptionDetails() throws Exception {
        ResponseEntity<AuthErrorResponse> response = handler.handleUnexpected(
            new RuntimeException("sensitive backend detail"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Request could not be completed");
        assertThat(response.getBody().message()).doesNotContain("sensitive backend detail");
        assertThat(response.getBody().requestId()).isNotBlank();
    }
}
