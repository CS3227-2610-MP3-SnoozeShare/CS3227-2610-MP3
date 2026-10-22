package com.snoozeshare.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
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

}
