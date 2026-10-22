package com.snoozeshare.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import com.snoozeshare.testsupport.AgentApiIntegrationTest;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** Pins the API error contract and ensures failures never disclose framework or SQL text. */
class ApiErrorMappingIntegrationTest extends AgentApiIntegrationTest {

    @Test
    void malformedUuidIs400() throws Exception {
        mockMvc.perform(get("/api/agent/_test/uuid/not-a-uuid")
                        .header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request is not valid"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void repositoryFailureIs500() throws Exception {
        mockMvc.perform(get("/api/agent/_test/repository-error")
                        .header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Something went wrong"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void wrongMethodIs405() throws Exception {
        mockMvc.perform(put("/api/agent/_test/ping")
                        .header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("Method not allowed"));
    }

    @Test
    void wrongMediaTypeIs415() throws Exception {
        mockMvc.perform(post("/api/agent/_test/notes")
                        .header("Authorization", "Bearer " + agent.token())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("notes"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value("Unsupported media type"));
    }

    @Test
    void serviceAndFrameworkMessagesAreSeparated() throws Exception {
        mockMvc.perform(get("/api/agent/_test/service-invalid")
                        .header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Service validation message"));
        mockMvc.perform(get("/api/agent/_test/plain-invalid")
                        .header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request is not valid"));
    }

    @Test
    void logsHoldNoSecretsAndContainRequestId() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger("com.snoozeshare.api.ApiExceptionHandler");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            String secret = "secret request body and password";
            mockMvc.perform(get("/api/agent/_test/plain-secret")
                            .header("Authorization", "Bearer " + agent.token()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Something went wrong"));
            assertThat(appender.list).isNotEmpty();
            assertThat(appender.list).anySatisfy(event -> {
                assertThat(event.getFormattedMessage()).contains("requestId=");
                assertThat(event.getFormattedMessage()).doesNotContain(secret, "SQL");
                if (event.getThrowableProxy() != null) {
                    assertThat(event.getThrowableProxy().getMessage()).doesNotContain(secret, "SQL");
                    assertThat(event.getThrowableProxy().getClassName()).isNotBlank();
                }
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void unknownRouteUsesNotFoundErrorBodyAndNoStore() throws Exception {
        mockMvc.perform(get("/api/agent/nope")
                        .header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Not found"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void authResponsesKeepTheirExistingBody() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.message").value("Request cannot be authorized"));
    }
}
