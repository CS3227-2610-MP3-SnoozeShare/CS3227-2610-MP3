package com.snoozeshare.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.auth.security.ProtectedRequestPath;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;
import com.snoozeshare.testsupport.FoundationTestController;

/** Exercises authentication and path handling at the API namespace boundary. */
class ApiFoundationIntegrationTest extends AgentApiIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FoundationTestController foundationController;

    @Test
    void guestAndHostTokensAreForbidden() throws Exception {
        for (String token : List.of(guest.token(), host.token())) {
            mockMvc.perform(get("/api/agent/_test/ping")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ROLE_MISMATCH"));
        }
    }

    @Test
    void missingOrBadTokenIs401() throws Exception {
        mockMvc.perform(get("/api/agent/_test/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        mockMvc.perform(get("/api/agent/_test/ping")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void suspendedAgentIsForbidden() throws Exception {
        jdbc.update("update users set status = 'SUSPENDED' where id = ?::uuid", agent.id());

        mockMvc.perform(get("/api/agent/_test/ping")
                        .header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROFILE_INACTIVE"));
    }

    @Test
    void unregisteredProfileIsNotFound() throws Exception {
        String token = "unregistered-" + UUID.randomUUID();
        tokenVerifier.add(token, "no-profile-" + UUID.randomUUID(), "missing@api.test");

        mockMvc.perform(get("/api/agent/_test/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFILE_NOT_REGISTERED"));
    }

    @Test
    void pathVariantsRemainProtected() throws Exception {
        List<String> paths = List.of(
                "/api/%61gent/disputes",
                "//api/agent/disputes",
                "/api/agent/disputes/",
                "/api/agent/disputes;x=1",
                "/api/agent/%2e%2e/guest/x");
        for (String path : paths) {
            assertRejected(path, "Bearer " + guest.token());
            assertRejected(path, null);
        }
    }

    @Test
    void noStoreAppliesToSuccessAndAuthenticationErrors() throws Exception {
        mockMvc.perform(get("/api/agent/_test/ping")
                        .header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
        mockMvc.perform(get("/api/agent/_test/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", "no-store"));
        mockMvc.perform(get("/api/agent/not-found")
                        .header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void anonymousOversizeIs401() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/agent/_test/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("x".repeat(70_000)))
                .andExpect(status().isUnauthorized())
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void oversizedAndChunkedBodiesAreRejectedButBodylessMutationPasses() throws Exception {
        mockMvc.perform(post("/api/agent/_test/ping")
                        .header("Authorization", "Bearer " + agent.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("x".repeat(70_000)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"));
        mockMvc.perform(post("/api/agent/_test/ping")
                        .header("Authorization", "Bearer " + agent.token())
                        .header("Transfer-Encoding", "chunked"))
                .andExpect(status().isLengthRequired())
                .andExpect(jsonPath("$.code").value("LENGTH_REQUIRED"));
        mockMvc.perform(put("/api/agent/_test/ping")
                        .header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isMethodNotAllowed());
    }

    private void assertRejected(String path, String authorization) throws Exception {
        var requestBuilder = path.startsWith("//")
                ? get("/api/agent/disputes").with(servletRequest -> {
                    servletRequest.setRequestURI(path);
                    servletRequest.setServletPath(path);
                    return servletRequest;
                })
                : get(path);
        if (authorization != null) {
            requestBuilder.header("Authorization", authorization);
        }
        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        int statusCode = result.getResponse().getStatus();
        String withinApplication = ProtectedRequestPath.withinApplication(result.getRequest());
        assertThat(statusCode).as("protected URL variant %s resolved as %s", path,
                withinApplication).isIn(400, 401, 403);
    }
}
