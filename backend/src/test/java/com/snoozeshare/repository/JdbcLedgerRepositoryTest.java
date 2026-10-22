package com.snoozeshare.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.model.WalletTransaction;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;
import com.snoozeshare.testsupport.DisputeScenarioFactory.Scenario;

/** The ledger reads money rows out of audit_log; the settlement path reads them on PostgreSQL. */
class JdbcLedgerRepositoryTest extends AgentApiIntegrationTest {

    @Autowired
    private LedgerRepository ledger;

    @Test
    void readsEscrowRowsForABookingWithTheirTimestamps() {
        Scenario scenario = scenarios.openDispute(RemedyType.FULL_REFUND);

        var entries = ledger.entriesForBooking(scenario.booking().getId());

        assertThat(entries).isNotEmpty();
        assertThat(entries).extracting(WalletTransaction::timestamp).doesNotContainNull();
    }
}
