package com.snoozeshare.api.controller.agent;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;

/** A1 to A5 of the agent dispute API. */
class AgentDisputeControllerTest extends AgentApiIntegrationTest {

    private ResultActions getAs(String path, String token) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", "Bearer " + token));
    }

    private ResultActions putAs(String path, String token, String json) throws Exception {
        return mockMvc.perform(put(path).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long auditRows(String action) {
        return jdbc.queryForObject("select count(*) from audit_log where action_type = ?", Long.class, action);
    }

    @Test
    void assignMovesToInReview() throws Exception { // F5, E14, E56
        var dispute = scenarios.openDispute(RemedyType.FULL_REFUND);
        String path = "/api/agent/disputes/" + dispute.ticket().getId() + "/assign";

        putAs(path, agent.token(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.ticket.assignedAgentId").value(agent.id().toString()));
        org.assertj.core.api.Assertions.assertThat(auditRows("TICKET_ASSIGNED")).isEqualTo(1);
        var repeat = putAs(path, agent.token(), "").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT")).andReturn();
        com.snoozeshare.testsupport.ErrorAssertions.assertCleanMessage(repeat, "Ticket is not open");
    }

    @Test
    void notesBoundaryAndClearing() throws Exception { // F6, E13
        var dispute = scenarios.inReviewDispute(agent.id());
        String path = "/api/agent/disputes/" + dispute.ticket().getId() + "/notes";

        putAs(path, agent.token(), "{\"notes\":\"" + "x".repeat(4000) + "\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.agentNotes").value("x".repeat(4000)));
        putAs(path, agent.token(), "{\"notes\":\"" + "x".repeat(4001) + "\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        putAs(path, agent.token(), "{\"notes\":null}").andExpect(jsonPath("$.agentNotes").value(""));
        putAs(path, agent.token(), "{\"notes\":\"\"}").andExpect(jsonPath("$.agentNotes").value(""));
    }

    @Test
    void otherAgentCannotSaveNotes() throws Exception { // E12
        var dispute = scenarios.inReviewDispute(agent.id());
        var other = testUsers.create(com.snoozeshare.domain.enums.Role.AGENT);

        var result = putAs("/api/agent/disputes/" + dispute.ticket().getId() + "/notes", other.token(),
                "{\"notes\":\"mine\"}").andExpect(status().isConflict()).andReturn();
        com.snoozeshare.testsupport.ErrorAssertions.assertCleanMessage(result, "Ticket is not assigned to this agent");
    }

    @Test
    void notesOnOpenOrResolvedIs409() throws Exception { // E16
        var open = scenarios.openDispute(RemedyType.FULL_REFUND);
        var resolved = scenarios.resolvedDispute(agent.id());

        for (var id : java.util.List.of(open.ticket().getId(), resolved.ticket().getId())) {
            var result = putAs("/api/agent/disputes/" + id + "/notes", agent.token(), "{\"notes\":\"n\"}")
                    .andExpect(status().isConflict()).andReturn();
            com.snoozeshare.testsupport.ErrorAssertions.assertCleanMessage(result,
                    "Notes can only be saved while the ticket is in review");
        }
    }

    private String resolvePath(java.util.UUID ticketId) {
        return "/api/agent/disputes/" + ticketId + "/resolve";
    }

    private String resolveBody(String mode, String refund, String reason) {
        return "{\"mode\":\"" + mode + "\",\"guestRefund\":" + refund + ",\"reason\":\"" + reason + "\"}";
    }

    private java.math.BigDecimal balanceOf(java.util.UUID userId) {
        return jdbc.queryForObject("select balance from wallets where user_id = ?", java.math.BigDecimal.class, userId);
    }

    @Test
    void resolveSettlesWithWorkedNumbers() throws Exception { // F7 (escrow is 120.00 in the scenario)
        var approved = scenarios.inReviewDispute(agent.id());
        var guestBefore = balanceOf(approved.guest().getId());
        var hostBefore = balanceOf(approved.host().getId());

        putAs(resolvePath(approved.ticket().getId()), agent.token(), resolveBody("MANUAL", "40.00", "partial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.status").value("RESOLVED_APPROVED"))
                .andExpect(jsonPath("$.ticket.resolutionReason").value("partial"));
        org.assertj.core.api.Assertions.assertThat(balanceOf(approved.guest().getId()).subtract(guestBefore))
                .isEqualByComparingTo("40.00");
        org.assertj.core.api.Assertions.assertThat(balanceOf(approved.host().getId()).subtract(hostBefore))
                .isEqualByComparingTo("77.60");

        var rejected = scenarios.inReviewDispute(agent.id());
        var hostBeforeReject = balanceOf(rejected.host().getId());
        putAs(resolvePath(rejected.ticket().getId()), agent.token(), resolveBody("MANUAL", "0.00", "none"))
                .andExpect(jsonPath("$.ticket.status").value("RESOLVED_REJECTED"));
        org.assertj.core.api.Assertions.assertThat(balanceOf(rejected.host().getId()).subtract(hostBeforeReject))
                .isEqualByComparingTo("116.40");
    }

    @Test
    void otherAgentCannotResolve() throws Exception { // E15
        var dispute = scenarios.inReviewDispute(agent.id());
        var other = testUsers.create(com.snoozeshare.domain.enums.Role.AGENT);
        var guestBefore = balanceOf(dispute.guest().getId());

        var result = putAs(resolvePath(dispute.ticket().getId()), other.token(),
                resolveBody("REJECT", "null", "nope")).andExpect(status().isConflict()).andReturn();

        com.snoozeshare.testsupport.ErrorAssertions.assertCleanMessage(result, "Ticket is not assigned to this agent");
        org.assertj.core.api.Assertions.assertThat(balanceOf(dispute.guest().getId())).isEqualByComparingTo(guestBefore);
    }

    @Test
    void resolveTwiceIs409() throws Exception { // E21
        var dispute = scenarios.inReviewDispute(agent.id());
        putAs(resolvePath(dispute.ticket().getId()), agent.token(), resolveBody("REJECT", "null", "first"))
                .andExpect(status().isOk());

        var result = putAs(resolvePath(dispute.ticket().getId()), agent.token(),
                resolveBody("REJECT", "null", "again")).andExpect(status().isConflict()).andReturn();

        com.snoozeshare.testsupport.ErrorAssertions.assertCleanMessage(result, "Ticket is not in review");
    }

    @Test
    void queueFiltersAndCounts() throws Exception { // F3
        var first = scenarios.openDispute(RemedyType.FULL_REFUND);
        scenarios.openDispute(RemedyType.FULL_REFUND);
        scenarios.openDispute(RemedyType.FULL_REFUND);
        scenarios.inReviewDispute(agent.id());

        getAs("/api/agent/disputes", agent.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.items[0].ticketId").value(first.ticket().getId().toString()))
                .andExpect(jsonPath("$.unassignedCount").value(3));
        getAs("/api/agent/disputes?assignee=MINE", agent.token())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.unassignedCount").value(3));
        getAs("/api/agent/disputes?assignee=UNASSIGNED", agent.token())
                .andExpect(jsonPath("$.items", hasSize(3)));
    }

    @Test
    void mineIsEmptyCountStaysGlobal() throws Exception { // E52
        scenarios.openDispute(RemedyType.FULL_REFUND);
        var other = testUsers.create(com.snoozeshare.domain.enums.Role.AGENT);

        getAs("/api/agent/disputes?assignee=MINE", other.token())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.unassignedCount").value(1));
    }

    @Test
    void detailShowsEscrowAndNotes() throws Exception { // F4, E17
        var dispute = scenarios.inReviewDispute(agent.id());

        getAs("/api/agent/disputes/" + dispute.ticket().getId(), agent.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escrowHeld").value(true))
                .andExpect(jsonPath("$.escrowAmount").isNumber())
                .andExpect(jsonPath("$", hasKey("agentNotes")))
                .andExpect(jsonPath("$.ticket.agentNotes").doesNotExist())
                .andExpect(jsonPath("$.ticket.assignedAgentId").value(agent.id().toString()));
    }

    @Test
    void unknownTicketIs404() throws Exception { // E11, E66
        var result = getAs("/api/agent/disputes/" + java.util.UUID.randomUUID(), agent.token())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND")).andReturn();
        com.snoozeshare.testsupport.ErrorAssertions.assertCleanMessage(result, "Ticket does not exist");
    }

    @Test
    void releasedEscrowDetailAndResolve() throws Exception { // E53 (detail half)
        var dispute = scenarios.releasedEscrowDispute();

        getAs("/api/agent/disputes/" + dispute.ticket().getId(), agent.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escrowHeld").value(false));
    }

    @Test
    void badAssignee() throws Exception { // E9
        getAs("/api/agent/disputes?assignee=BOGUS", agent.token())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownQueryParametersAreIgnored() throws Exception { // E80
        getAs("/api/agent/disputes?foo=1", agent.token()).andExpect(status().isOk());
    }
}
