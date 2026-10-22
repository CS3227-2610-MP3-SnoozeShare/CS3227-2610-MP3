package com.snoozeshare.api.controller.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.snoozeshare.domain.model.User;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;
import com.snoozeshare.testsupport.DisputeScenarioFactory.HostScenario;
import com.snoozeshare.testsupport.ErrorAssertions;

/** A8 to A10: the Guest and Host accounts an agent can suspend and reactivate. */
class AgentAccountControllerTest extends AgentApiIntegrationTest {

    private ResultActions action(String verb, UUID userId, String reason) throws Exception {
        return mockMvc.perform(put("/api/agent/accounts/" + userId + "/" + verb)
                .header("Authorization", "Bearer " + agent.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + reason + "\"}"));
    }

    private String statusOf(String table, UUID id) {
        return jdbc.queryForObject("select status from " + table + " where id = ?", String.class, id);
    }

    private long auditRows(String action) {
        Long count = jdbc.queryForObject("select count(*) from audit_log where action_type = ?", Long.class, action);
        return count == null ? 0 : count;
    }

    @Test
    void listHoldsGuestsAndHostsOnly() throws Exception { // E44
        mockMvc.perform(get("/api/agent/accounts").header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.role == 'AGENT' || @.role == 'SYSTEM')]", hasSize(0)))
                .andExpect(jsonPath("$[0].userId").exists())
                .andExpect(jsonPath("$[0].suspensionReason").hasJsonPath());
    }

    @Test
    void suspendAndReactivateAreAudited() throws Exception { // E56, E26
        User target = scenarios.guestWithConfirmedFutureBooking();
        UUID id = target.getId();

        action("suspend", id, "Chargeback abuse").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.suspensionReason").value("Chargeback abuse"));
        assertThat(auditRows("ACCOUNT_SUSPENDED")).isEqualTo(1);
        assertThat(auditRows("BOOKING_FORCE_CANCELLED")).isEqualTo(1);

        action("reactivate", id, "Appeal accepted").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.suspensionReason").isEmpty());
        assertThat(auditRows("ACCOUNT_REACTIVATED")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from bookings where guest_id = ? "
                + "and status = 'FORCE_CANCELLED'", Long.class, id)).isEqualTo(1);
    }

    @Test
    void staffAndUnknownTargets() throws Exception { // E24, E27
        for (String verb : new String[] {"suspend", "reactivate"}) {
            MvcResult staff = action(verb, agent.id(), "no").andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST")).andReturn();
            ErrorAssertions.assertCleanMessage(staff, "Support agent accounts cannot be suspended or reactivated");
            MvcResult missing = action(verb, UUID.randomUUID(), "no").andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND")).andReturn();
            ErrorAssertions.assertCleanMessage(missing, "User does not exist");
        }
    }

    @Test
    void repeatedActionsAreConflicts() throws Exception { // E25
        ErrorAssertions.assertCleanMessage(
                action("reactivate", guest.id(), "x").andExpect(status().isConflict()).andReturn(),
                "Account is not suspended");
        action("suspend", guest.id(), "x").andExpect(status().isOk());
        ErrorAssertions.assertCleanMessage(
                action("suspend", guest.id(), "x").andExpect(status().isConflict()).andReturn(),
                "Account is already suspended");
    }

    @Test
    void reasonRules() throws Exception { // E28, E65
        action("suspend", guest.id(), "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        action("suspend", guest.id(), "x".repeat(1001)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        action("suspend", guest.id(), "x".repeat(1000)).andExpect(status().isOk());
    }

    @Test
    void suspendedGuestIsLockedOutAtOnce() throws Exception { // E29
        action("suspend", guest.id(), "Policy").andExpect(status().isOk());

        mockMvc.perform(get("/api/guest/ping").header("Authorization", "Bearer " + guest.token()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PROFILE_INACTIVE"));
    }

    @Test
    void hostSuspensionCascades() throws Exception { // E70
        HostScenario scenario = scenarios.hostWithTwoListingsAndOneBooking();

        action("suspend", scenario.host().getId(), "Fraud").andExpect(status().isOk());

        assertThat(statusOf("properties", scenario.firstListing().getId())).isEqualTo("INACTIVE");
        assertThat(statusOf("properties", scenario.secondListing().getId())).isEqualTo("INACTIVE");
        assertThat(statusOf("bookings", scenario.booking().getId())).isEqualTo("FORCE_CANCELLED");
    }

    @Test
    void stayStartingTodayIsKept() throws Exception { // E70
        HostScenario scenario = scenarios.hostWithTwoListingsAndOneBooking();
        jdbc.update("update bookings set start_date = current_date, end_date = current_date + 3 where id = ?",
                scenario.booking().getId());

        action("suspend", scenario.host().getId(), "Fraud").andExpect(status().isOk());

        assertThat(statusOf("bookings", scenario.booking().getId())).isEqualTo("CONFIRMED");
        assertThat(statusOf("properties", scenario.firstListing().getId())).isEqualTo("INACTIVE");
    }

    @Test
    void failedCascadeRollsEverythingBack() throws Exception { // E68
        User target = scenarios.guestWithConfirmedFutureBooking();
        doThrow(new DataAccessResourceFailureException("db down")).when(ledgerWriter)
                .post(any(), any(), any(), any(), any(), any(), any(), any());

        MvcResult result = action("suspend", target.getId(), "Fraud").andExpect(status().isInternalServerError())
                .andReturn();

        ErrorAssertions.assertCleanMessage(result, "Something went wrong");
        assertThat(statusOf("users", target.getId())).isEqualTo("ACTIVE");
        assertThat(auditRows("ACCOUNT_SUSPENDED")).isZero();
        assertThat(auditRows("BOOKING_FORCE_CANCELLED")).isZero();
        assertThat(jdbc.queryForObject("select count(*) from bookings where guest_id = ? and status = 'CONFIRMED'",
                Long.class, target.getId())).isEqualTo(1);
    }
}
