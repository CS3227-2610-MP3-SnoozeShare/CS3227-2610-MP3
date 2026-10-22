package com.snoozeshare.api.host;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.repository.AuthUserRepository;
import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.AuditLogEntry;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.infra.events.WalletTransactionRecordedEvent;
import com.snoozeshare.repository.AuditLogRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.repository.WalletRepository;
import com.snoozeshare.service.WalletService;

/** E3/E9/E10/E12/E15/E17: authenticated Host wallet contract and ledger safety. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "snoozeshare.security.enabled=true")
@AutoConfigureMockMvc
@Import(HostListingsControllerTest.TestAuthConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
@RecordApplicationEvents
class HostWalletIntegrationTest {

    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthUserRepository authUsers;
    @Autowired private UserRepository users;
    @Autowired private WalletRepository wallets;
    @Autowired private AuditLogRepository audits;
    @Autowired private HostListingsControllerTest.FakeTokenVerifier verifier;
    @Autowired private WalletService walletService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ApplicationEvents events;

    private UUID hostId;
    private UUID walletId;
    private String token;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        walletId = UUID.randomUUID();
        token = "host-wallet-" + UUID.randomUUID();
        verifier.add(token, token);
        authUsers.insert(new AuthUserRepository.AuthUser(hostId, token, token + "@example.com",
            AuthRole.HOST, "Host", "ACTIVE", null, null));
        users.save(new User(hostId, token, Role.HOST, "Host", token + "@example.com",
            AccountStatus.ACTIVE, null, Instant.now(), Instant.now(), null));
        wallets.save(new Wallet(walletId, hostId, new BigDecimal("100.00"), "SGD", Instant.now()));
    }

    @Test
    void statementReturnsAllNewestRowsInStableOrderAndOnlyShowsTheAuthenticatedHost() throws Exception {
        Instant sameTimestamp = Instant.parse("2026-01-01T00:00:00Z");
        for (int index = 1; index <= 101; index++) {
            audits.save(new AuditLogEntry(new UUID(0, index), hostId, "Host", "TOP_UP",
                "WALLET", walletId, null, null, new BigDecimal("1.00"), "seed",
                hostId, "Host", null, null, sameTimestamp, new BigDecimal(index)));
        }
        UUID otherHost = UUID.randomUUID();
        users.save(new User(otherHost, "other-" + otherHost, Role.HOST, "Other",
            "other-" + otherHost + "@example.com", AccountStatus.ACTIVE, null,
            Instant.now(), Instant.now(), null));
        wallets.save(new Wallet(UUID.randomUUID(), otherHost, new BigDecimal("50.00"),
            "SGD", Instant.now()));
        Wallet otherWallet = wallets.findByUserId(otherHost).orElseThrow();
        for (int index = 1; index <= 5; index++) {
            audits.save(new AuditLogEntry(new UUID(1, index), otherHost, "Other", "TOP_UP",
                "WALLET", otherWallet.getId(), null, null, new BigDecimal("1.00"), "foreign",
                otherHost, "Other", null, null, sameTimestamp, new BigDecimal(index)));
        }

        String statement = mockMvc.perform(get("/api/host/wallet")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.wallet.id").value(walletId.toString()))
            .andExpect(jsonPath("$.statement.length()").value(101))
            .andExpect(jsonPath("$.statement[0].id").value(new UUID(0, 101).toString()))
            .andExpect(jsonPath("$.statement[100].id").value(new UUID(0, 1).toString()))
            .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/host/wallet?page=0&size=1")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(result ->
                assertThat(result.getResponse().getContentAsString()).isEqualTo(statement));
    }

    @Test
    void eachTopUpIsSeparateAndInvalidOrInsufficientWithdrawalsDoNotChangeLedger() throws Exception {
        String first = topUp("10.25");
        String second = topUp("10.25");
        UUID firstId = transactionId(first);
        UUID secondId = transactionId(second);
        assertThat(firstId).isNotEqualTo(secondId);
        assertThat(wallets.findById(walletId).orElseThrow().getBalance())
            .isEqualByComparingTo("120.50");
        assertThat(audits.findAll()).filteredOn(row -> hostId.equals(row.getSubjectUserId()))
            .filteredOn(row -> "TOP_UP".equals(row.getActionType())).hasSize(2);

        int ledgerCount = walletRows().size();
        mockMvc.perform(post("/api/host/wallet/withdraw")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":500.00}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("DOMAIN_RULE_VIOLATION"));
        mockMvc.perform(post("/api/host/wallet/top-up")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":100000.001}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/host/wallet/top-up")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1.00,\"userId\":\"" + UUID.randomUUID() + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/host/wallet/top-up")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":0.00}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/host/wallet/withdraw")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":-1.00}"))
            .andExpect(status().isBadRequest());
        assertThat(wallets.findById(walletId).orElseThrow().getBalance())
            .isEqualByComparingTo("120.50");
        assertThat(walletRows()).hasSize(ledgerCount);
    }

    @Test
    void concurrentWithdrawalsSerializeOnTheWalletRow() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<String>> outcomes = List.of(
                executor.submit(() -> withdrawAfter(start)),
                executor.submit(() -> withdrawAfter(start)));
            start.countDown();
            assertThat(outcomes.stream().map(this::outcome).toList())
                .containsExactlyInAnyOrder("withdrawn", "insufficient");
        }
        assertThat(wallets.findById(walletId).orElseThrow().getBalance())
            .isEqualByComparingTo("20.00");
    }

    @Test
    void walletAmountNeverAppearsInCapturedLogs(CapturedOutput output) throws Exception {
        topUp("73.19");
        assertThat(output).doesNotContain("73.19");
        assertThat(events.stream(WalletTransactionRecordedEvent.class)
            .filter(event -> walletId.equals(event.walletId()))).hasSize(1);
    }

    @Test
    void rolledBackWalletMutationWritesNoLedgerAndPublishesNoEvent() {
        int ledgerCount = walletRows().size();
        long eventCount = events.stream(WalletTransactionRecordedEvent.class).count();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
                walletService.topUp(hostId, new BigDecimal("5.25"));
                throw new IllegalStateException("test rollback");
            });
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessage("test rollback");
        }
        assertThat(wallets.findById(walletId).orElseThrow().getBalance())
            .isEqualByComparingTo("100.00");
        assertThat(walletRows()).hasSize(ledgerCount);
        assertThat(events.stream(WalletTransactionRecordedEvent.class).count()).isEqualTo(eventCount);
    }

    private String topUp(String amount) throws Exception {
        return mockMvc.perform(post("/api/host/wallet/top-up")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":" + amount + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mocked").value(true))
            .andExpect(jsonPath("$.replayed").doesNotExist())
            .andReturn().getResponse().getContentAsString();
    }

    private UUID transactionId(String response) throws Exception {
        return UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(response).get("transaction").get("id").asText());
    }

    private List<AuditLogEntry> walletRows() {
        return audits.findAll().stream()
            .filter(row -> hostId.equals(row.getSubjectUserId()))
            .filter(row -> row.getWalletAdjustment() != null)
            .toList();
    }

    private String withdrawAfter(CountDownLatch start) throws Exception {
        start.await();
        try {
            walletService.withdraw(hostId, new BigDecimal("80.00"));
            return "withdrawn";
        } catch (IllegalArgumentException exception) {
            return "insufficient";
        }
    }

    private String outcome(Future<String> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
