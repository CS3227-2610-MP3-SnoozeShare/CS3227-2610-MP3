package com.snoozeshare.testsupport;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.snoozeshare.PostgresIntegrationTest;
import com.snoozeshare.auth.repository.AuthUserRepository;
import com.snoozeshare.domain.enums.Role;

/** Shared authenticated API fixture backed by the real Postgres auth repository. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "snoozeshare.security.enabled=true")
@AutoConfigureMockMvc
@Import(AgentApiIntegrationTest.AgentApiTestConfiguration.class)
public abstract class AgentApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected FakeTokenVerifier tokenVerifier;

    @Autowired
    protected AuthUserRepository authUsers;

    @Autowired
    protected AgentTestUsers testUsers;

    @Autowired
    protected DisputeScenarioFactory scenarios;

    protected AgentTestUsers.TestUser agent;
    protected AgentTestUsers.TestUser guest;
    protected AgentTestUsers.TestUser host;

    @BeforeEach
    void resetApiFixture() {
        TestDataReset.reset(jdbc);
        tokenVerifier.clear();
        agent = testUsers.create(Role.AGENT);
        guest = testUsers.create(Role.GUEST);
        host = testUsers.create(Role.HOST);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AgentApiTestConfiguration {

        @Bean
        @Primary
        FakeTokenVerifier fakeTokenVerifier() {
            return new FakeTokenVerifier();
        }

        @Bean
        AgentTestUsers agentTestUsers(
                com.snoozeshare.repository.UserRepository users,
                com.snoozeshare.repository.WalletRepository wallets,
                FakeTokenVerifier verifier) {
            return new AgentTestUsers(users, wallets, verifier);
        }

        @Bean
        DisputeScenarioFactory disputeScenarioFactory(
                AgentTestUsers users,
                com.snoozeshare.repository.PropertyRepository properties,
                com.snoozeshare.repository.BookingRepository bookings,
                com.snoozeshare.repository.TicketRepository ticketRepository,
                com.snoozeshare.service.TicketService tickets,
                com.snoozeshare.service.WalletService wallets,
                com.snoozeshare.service.TransactionService transactions,
                org.springframework.jdbc.core.JdbcTemplate jdbc) {
            return new DisputeScenarioFactory(users, properties, bookings,
                    ticketRepository, tickets, wallets, transactions, jdbc);
        }
    }
}
