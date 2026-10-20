package com.snoozeshare;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.snoozeshare.auth.security.FirebaseTokenVerifier;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationException;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationFailure;

/** Verifies transactional profile registration, wallet rules and /me. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "snoozeshare.security.enabled=true",
        "snoozeshare.registration.host-code=host-secret",
        "snoozeshare.registration.agent-code=agent-secret"
    })
@AutoConfigureMockMvc
@Import(AuthRegistrationIntegrationTest.TestAuthConfiguration.class)
class AuthRegistrationIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private FakeTokenVerifier verifier;

    private String token;

    @BeforeEach
    void addVerifiedIdentity() {
        token = "registration-" + UUID.randomUUID();
        verifier.add(token, token, "User@Example.com");
        verifier.add(token + "-other", token + "-other", "other@example.com");
    }

    @Test
    void guestRegistrationCreatesOneWalletAndMeReturnsProfile() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"role\":\"GUEST\",\"displayName\":\"  A Guest  \"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.role").value("GUEST"))
            .andExpect(jsonPath("$.displayName").value("A Guest"))
            .andExpect(jsonPath("$.walletId").isNotEmpty());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("GUEST"));
        Integer wallets = jdbc.queryForObject(
            "select count(*) from wallets w join users u on u.id = w.user_id where u.firebase_uid = ?",
            Integer.class,
            token);
        org.assertj.core.api.Assertions.assertThat(wallets).isEqualTo(1);
    }

    @Test
    void sameRoleRetryIsIdempotentAndPrivilegedCodeIsRequired() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"role\":\"HOST\",\"registrationCode\":\"wrong\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("INVALID_REGISTRATION_CODE"));

        mockMvc.perform(post("/api/auth/register")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"role\":\"HOST\",\"registrationCode\":\"host-secret\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.walletId").isNotEmpty());
        mockMvc.perform(post("/api/auth/register")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"role\":\"HOST\",\"registrationCode\":\"host-secret\","
                    + "\"displayName\":\"Changed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").doesNotExist());
    }

    @Test
    void agentRegistrationDoesNotCreateWalletAndUnknownFieldsFail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"role\":\"AGENT\",\"registrationCode\":\"agent-secret\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.walletId").isEmpty());
        mockMvc.perform(post("/api/auth/register")
                .header("Authorization", "Bearer " + token + "-other")
                .contentType("application/json")
                .content("{\"role\":\"GUEST\",\"unknown\":true}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestAuthConfiguration {

        @Bean
        @Primary
        FakeTokenVerifier fakeTokenVerifier() {
            return new FakeTokenVerifier();
        }
    }

    static class FakeTokenVerifier implements FirebaseTokenVerifier {

        private final Map<String, FirebaseAuthentication> tokens = new ConcurrentHashMap<>();

        void add(String token, String uid, String email) {
            tokens.put(token, new FirebaseAuthentication(uid, email, true));
        }

        @Override
        public FirebaseAuthentication verify(String idToken) throws VerificationException {
            FirebaseAuthentication authentication = tokens.get(idToken);
            if (authentication == null) {
                throw new VerificationException(VerificationFailure.INVALID_TOKEN, null);
            }
            return authentication;
        }
    }
}
