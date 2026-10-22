package com.snoozeshare.controller.guest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.PostgresIntegrationTest;
import com.snoozeshare.auth.security.FirebaseTokenVerifier;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationException;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationFailure;
import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.repository.WalletRepository;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "snoozeshare.security.enabled=true")
@AutoConfigureMockMvc
@Import(GuestWalletControllerTest.TestAuthConfig.class)
class GuestWalletControllerTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private FakeTokenVerifier verifier;

    @Autowired
    private ObjectMapper objectMapper;

    private String guestToken;
    private UUID guestId;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        String guestUid = "guest-uid-" + UUID.randomUUID();
        guestToken = "guest-token-" + UUID.randomUUID();
        guestId = UUID.randomUUID();

        verifier.add(guestToken, guestUid, "guest@test.com");

        User guest = userRepository.save(new User(
                guestId, guestUid, Role.GUEST, "Alice Guest",
                "guest-" + UUID.randomUUID() + "@test.com",
                AccountStatus.ACTIVE, null, now, now, null));
        walletRepository.save(new Wallet(
                UUID.randomUUID(), guest.getId(),
                new BigDecimal("1000.00"), "SGD", now));
    }

    @Test
    void getWallet() throws Exception {
        mockMvc.perform(get("/api/guest/wallet")
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(1000.00))
            .andExpect(jsonPath("$.currency").value("SGD"));
    }

    @Test
    void getTransactions() throws Exception {
        mockMvc.perform(get("/api/guest/wallet/transactions")
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void topUp() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("amount", "200.00"));

        mockMvc.perform(post("/api/guest/wallet/topup")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(1200.00));
    }

    @Test
    void withdraw() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("amount", "300.00"));

        mockMvc.perform(post("/api/guest/wallet/withdraw")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balance").value(700.00));
    }

    @Test
    void topUpZeroAmount() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("amount", "0"));

        mockMvc.perform(post("/api/guest/wallet/topup")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void withdrawZeroAmount() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("amount", "0"));

        mockMvc.perform(post("/api/guest/wallet/withdraw")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void withdrawExceedsBalance() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("amount", "5000.00"));

        mockMvc.perform(post("/api/guest/wallet/withdraw")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    void rejectsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/guest/wallet"))
            .andExpect(status().isUnauthorized());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestAuthConfig {

        @Bean
        @Primary
        FakeTokenVerifier fakeTokenVerifier() {
            return new FakeTokenVerifier();
        }
    }

    static class FakeTokenVerifier implements FirebaseTokenVerifier {

        private final Map<String, FirebaseAuthentication> tokens =
                new ConcurrentHashMap<>();

        void add(String token, String uid, String email) {
            tokens.put(token, new FirebaseAuthentication(uid, email, true));
        }

        @Override
        public FirebaseAuthentication verify(String idToken)
                throws VerificationException {
            FirebaseAuthentication auth = tokens.get(idToken);
            if (auth == null) {
                throw new VerificationException(
                        VerificationFailure.INVALID_TOKEN, null);
            }
            return auth;
        }
    }
}
