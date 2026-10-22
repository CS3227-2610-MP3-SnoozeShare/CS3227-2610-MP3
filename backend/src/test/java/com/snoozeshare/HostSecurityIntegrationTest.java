package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.repository.AuthUserRepository;
import com.snoozeshare.auth.security.FirebaseTokenVerifier;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationException;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationFailure;

/** Verifies Host requests reuse the authenticated principal and never call protected work on rejection. */
@AutoConfigureMockMvc
@org.springframework.boot.test.context.SpringBootTest(properties = "snoozeshare.security.enabled=true")
@Import({
    HostSecurityIntegrationTest.TestAuthConfiguration.class,
    HostSecurityIntegrationTest.HostProbeController.class
})
class HostSecurityIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthUserRepository authUsers;

    @Autowired
    private FakeTokenVerifier verifier;

    @Autowired
    private HostProbeController probe;

    private String hostToken;
    private String guestToken;
    private String inactiveToken;

    @BeforeEach
    void setUp() {
        hostToken = "host-" + UUID.randomUUID();
        guestToken = "guest-" + UUID.randomUUID();
        inactiveToken = "inactive-" + UUID.randomUUID();
        verifier.add(hostToken, hostToken);
        verifier.add(guestToken, guestToken);
        verifier.add(inactiveToken, inactiveToken);
        authUsers.insert(authUser(hostToken, AuthRole.HOST, "ACTIVE"));
        authUsers.insert(authUser(guestToken, AuthRole.GUEST, "ACTIVE"));
        authUsers.insert(authUser(inactiveToken, AuthRole.HOST, "SUSPENDED"));
        probe.reset();
    }

    @Test
    void missingOrInvalidBearerTokenIsRejectedBeforeProtectedWork() throws Exception {
        mockMvc.perform(get("/api/host/probe"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
            .andExpect(jsonPath("$.requestId").isNotEmpty());
        mockMvc.perform(get("/api/host/probe").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        assertThat(probe.calls()).isZero();
    }

    @Test
    void activeGuestCannotCallHostEndpoint() throws Exception {
        mockMvc.perform(get("/api/host/probe").header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ROLE_MISMATCH"));
        assertThat(probe.calls()).isZero();
    }

    @Test
    void inactiveHostCannotCallHostEndpoint() throws Exception {
        mockMvc.perform(get("/api/host/probe").header("Authorization", "Bearer " + inactiveToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PROFILE_INACTIVE"));
        assertThat(probe.calls()).isZero();
    }

    private AuthUserRepository.AuthUser authUser(String uid, AuthRole role, String status) {
        return new AuthUserRepository.AuthUser(UUID.randomUUID(), uid, uid + "@example.com", role,
            "Test user", status, null, null);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestAuthConfiguration {

        @Bean
        @Primary
        FakeTokenVerifier hostFakeTokenVerifier() {
            return new FakeTokenVerifier();
        }

        @Bean
        @Primary
        AuthUserRepository hostInMemoryAuthUserRepository() {
            return new InMemoryAuthUserRepository();
        }
    }

    @RestController
    static class HostProbeController {

        private final AtomicInteger callCount = new AtomicInteger();

        @GetMapping("/api/host/probe")
        Map<String, String> probe() {
            callCount.incrementAndGet();
            return Map.of("result", "host");
        }

        int calls() {
            return callCount.get();
        }

        void reset() {
            callCount.set(0);
        }
    }

    static class FakeTokenVerifier implements FirebaseTokenVerifier {

        private final Map<String, FirebaseAuthentication> tokens = new ConcurrentHashMap<>();

        void add(String token, String uid) {
            tokens.put(token, new FirebaseAuthentication(uid, uid + "@example.com", true));
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

        private final Map<String, AuthUser> byUid = new ConcurrentHashMap<>();

        @Override
        public Optional<AuthUser> findByFirebaseUid(String firebaseUid) {
            return Optional.ofNullable(byUid.get(firebaseUid));
        }

        @Override
        public Optional<AuthUser> findById(UUID id) {
            return byUid.values().stream().filter(user -> user.id().equals(id)).findFirst();
        }

        @Override
        public AuthUser insert(AuthUser user) {
            byUid.put(user.firebaseUid(), user);
            return user;
        }
    }
}
