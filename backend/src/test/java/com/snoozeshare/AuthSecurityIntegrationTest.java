package com.snoozeshare;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Optional;
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
import org.springframework.test.web.servlet.MockMvc;

import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.repository.AuthUserRepository;
import com.snoozeshare.auth.security.FirebaseTokenVerifier;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationException;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationFailure;

/** Verifies the stateless bearer-token and role namespace boundary. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "snoozeshare.security.enabled=true")
@AutoConfigureMockMvc
@Import(AuthSecurityIntegrationTest.TestAuthConfiguration.class)
class AuthSecurityIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthUserRepository users;

    @Autowired
    private FakeTokenVerifier verifier;

    private String guestToken;
    private String hostToken;

    @BeforeEach
    void registerProfiles() {
        guestToken = "guest-" + UUID.randomUUID();
        hostToken = "host-" + UUID.randomUUID();
        verifier.add(guestToken, guestToken, "guest@example.com");
        verifier.add(hostToken, hostToken, "host@example.com");
        users.insert(new AuthUserRepository.AuthUser(
            UUID.randomUUID(), guestToken, "guest@example.com", AuthRole.GUEST, "Guest", "ACTIVE", null, null));
        users.insert(new AuthUserRepository.AuthUser(
            UUID.randomUUID(), hostToken, "host@example.com", AuthRole.HOST, "Host", "ACTIVE", null, null));
    }

    @Test
    void healthAndCorsPreflightRemainPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"status\":\"UP\"}"));
        mockMvc.perform(options("/api/guest/ping").header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk());
    }

    @Test
    void protectedPathRejectsMissingAndInvalidTokens() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void roleNamespaceUsesDatabaseRole() throws Exception {
        mockMvc.perform(get("/api/guest/ping").header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isOk())
            .andExpect(content().string("guest"));
        mockMvc.perform(get("/api/host/ping").header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ROLE_MISMATCH"));
    }

    @Test
    void unknownApiPathRemainsNotFound() throws Exception {
        mockMvc.perform(get("/api/not-a-route"))
            .andExpect(status().isNotFound());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestAuthConfiguration {

        @Bean
        @Primary
        FakeTokenVerifier fakeTokenVerifier() {
            return new FakeTokenVerifier();
        }

        @Bean
        @Primary
        AuthUserRepository authUserRepository() {
            return new InMemoryAuthUserRepository();
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

    static class InMemoryAuthUserRepository implements AuthUserRepository {

        private final Map<String, AuthUser> users = new ConcurrentHashMap<>();

        @Override
        public Optional<AuthUser> findByFirebaseUid(String firebaseUid) {
            return Optional.ofNullable(users.get(firebaseUid));
        }

        @Override
        public Optional<AuthUser> findById(UUID id) {
            return users.values().stream().filter(user -> user.id().equals(id)).findFirst();
        }

        @Override
        public AuthUser insert(AuthUser user) {
            users.put(user.firebaseUid(), user);
            return user;
        }
    }

}
