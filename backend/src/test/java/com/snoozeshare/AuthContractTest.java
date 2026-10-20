package com.snoozeshare;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Cross-endpoint status, error-shape and secret-disclosure contract checks. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "snoozeshare.security.enabled=true",
        "snoozeshare.registration.host-code=host-secret",
        "snoozeshare.registration.agent-code=agent-secret"
    })
@AutoConfigureMockMvc
@Import(AuthSecurityIntegrationTest.TestAuthConfiguration.class)
class AuthContractTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthSecurityIntegrationTest.FakeTokenVerifier verifier;

    private String token;

    @BeforeEach
    void addIdentity() {
        token = "contract-" + UUID.randomUUID();
        verifier.add(token, token, "contract@example.com");
    }

    @Test
    void protectedEndpointsReturnStableErrorsWithoutLeakingSecrets() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
            .andExpect(jsonPath("$.message").value("Request cannot be authorized"))
            .andExpect(jsonPath("$.requestId").isString());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
            .andExpect(jsonPath("$.requestId").isString());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PROFILE_NOT_REGISTERED"))
            .andExpect(jsonPath("$.requestId").isString());
    }

    @Test
    void validationAndRoleCodeErrorsHaveSafeStableContracts() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"role\":\"HOST\",\"registrationCode\":\"wrong\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("INVALID_REGISTRATION_CODE"))
            .andExpect(jsonPath("$.message").value("Request could not be completed"))
            .andExpect(jsonPath("$.requestId").isString());

        mockMvc.perform(post("/api/auth/register")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"role\":\"GUEST\",\"unknown\":true}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("Request could not be completed"))
            .andExpect(jsonPath("$.requestId").isString());
    }

    @Test
    void unknownApiRouteIsNotRewrittenAsAnAuthenticationError() throws Exception {
        mockMvc.perform(get("/api/not-a-route"))
            .andExpect(status().isNotFound());
    }

    @Test
    void registrationCodePreflightIsPublicAndDoesNotExposeTheCode() throws Exception {
        mockMvc.perform(post("/api/auth/registration-code/validate")
                .contentType("application/json")
                .content("{\"role\":\"HOST\",\"registrationCode\":\"host-secret\"}"))
            .andExpect(status().isNoContent())
            .andExpect(header -> org.assertj.core.api.Assertions.assertThat(
                header.getResponse().getHeader("Cache-Control")).isEqualTo("no-store"));

        mockMvc.perform(post("/api/auth/registration-code/validate")
                .contentType("application/json")
                .content("{\"role\":\"HOST\",\"registrationCode\":\"wrong\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("INVALID_REGISTRATION_CODE"))
            .andExpect(jsonPath("$.message").value("Request could not be completed"));

        mockMvc.perform(post("/api/auth/registration-code/validate")
                .contentType("application/json")
                .content("{\"role\":\"GUEST\",\"registrationCode\":\"wrong\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
