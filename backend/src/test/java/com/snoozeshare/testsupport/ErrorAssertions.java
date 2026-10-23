package com.snoozeshare.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Assertions for stable, user-safe API error messages. */
public final class ErrorAssertions {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ErrorAssertions() { }

    public static JsonNode assertCleanMessage(MvcResult result, String expectedMessage)
            throws Exception {
        JsonNode body = MAPPER.readTree(result.getResponse().getContentAsString());
        String message = body.path("message").asText();
        assertThat(message).isEqualTo(expectedMessage);
        assertThat(message).doesNotContain("com.snoozeshare", "org.springframework", " at ");
        assertThat(body.path("requestId").asText()).isNotBlank();
        assertThat(body.path("code").asText()).isNotBlank();
        return body;
    }
}
