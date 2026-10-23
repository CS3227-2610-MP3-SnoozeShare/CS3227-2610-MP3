package com.snoozeshare.api.controller.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;
import com.snoozeshare.testsupport.ErrorAssertions;

/** A12 to A16: dispute ticket categories. */
class AgentCategoryControllerTest extends AgentApiIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ResultActions send(MockHttpServletRequestBuilder builder, String json) throws Exception {
        builder.header("Authorization", "Bearer " + agent.token());
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(builder);
    }

    private ResultActions label(MockHttpServletRequestBuilder builder, String label) throws Exception {
        return send(builder, "{\"label\":\"" + label + "\"}");
    }

    private UUID create(String label) throws Exception {
        MvcResult result = label(post("/api/agent/categories"), label).andExpect(status().isCreated()).andReturn();
        return UUID.fromString(MAPPER.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private long auditRows(String action) {
        Long count = jdbc.queryForObject("select count(*) from audit_log where action_type = ?", Long.class, action);
        return count == null ? 0 : count;
    }

    @Test
    void createRenameToggleDelete() throws Exception { // F12, E56
        UUID id = create("Noise");
        send(get("/api/agent/categories"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].label", hasItem("Noise")));

        label(put("/api/agent/categories/" + id), "Loud noise").andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Loud noise")).andExpect(jsonPath("$.active").value(true));
        send(put("/api/agent/categories/" + id + "/active"), "{\"active\":false}").andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        send(delete("/api/agent/categories/" + id), null).andExpect(status().isNoContent());

        send(get("/api/agent/categories"), null).andExpect(jsonPath("$[*].label", org.hamcrest.Matchers.not(
                hasItem("Loud noise"))));
        assertThat(auditRows("TICKET_CATEGORY_CREATED")).isEqualTo(1);
    }

    @Test
    void duplicateLabelsIgnoreCaseAndSpaces() throws Exception { // E35
        UUID other = create("Cleanliness");
        create("Noise");

        MvcResult create = label(post("/api/agent/categories"), "  noise ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST")).andReturn();
        ErrorAssertions.assertCleanMessage(create, "Category label already exists");
        label(put("/api/agent/categories/" + other), "NOISE").andExpect(status().isBadRequest());
    }

    @Test
    void usedCategoryCannotBeDeleted() throws Exception { // E37
        var dispute = scenarios.openDispute(RemedyType.FULL_REFUND);
        UUID id = jdbc.queryForObject("select id from ticket_categories where label = ?", UUID.class,
                dispute.category());

        MvcResult result = send(delete("/api/agent/categories/" + id), null).andExpect(status().isConflict())
                .andReturn();

        ErrorAssertions.assertCleanMessage(result, "Category is in use by tickets; deactivate it instead");
        assertThat(jdbc.queryForObject("select count(*) from ticket_categories where id = ?", Long.class, id))
                .isEqualTo(1);
    }

    @Test
    void unknownCategory() throws Exception { // E38
        UUID missing = UUID.randomUUID();
        label(put("/api/agent/categories/" + missing), "Anything").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        send(put("/api/agent/categories/" + missing + "/active"), "{\"active\":true}")
                .andExpect(status().isNotFound());
        send(delete("/api/agent/categories/" + missing), null).andExpect(status().isNotFound());
    }

    @Test
    void renameToSameLabelIsAccepted() throws Exception { // E54
        UUID id = create("Noise");

        label(put("/api/agent/categories/" + id), "Noise").andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Noise"));
    }

    @Test
    void labelLengthRules() throws Exception { // E65
        label(post("/api/agent/categories"), "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        label(post("/api/agent/categories"), "x".repeat(61)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        label(post("/api/agent/categories"), "x".repeat(60)).andExpect(status().isCreated());
    }

    @Test
    void activeFlagIsRequired() throws Exception { // E79
        UUID id = create("Noise");
        for (String body : new String[] {"{}", "{\"active\":null}"}) {
            send(put("/api/agent/categories/" + id + "/active"), body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }
}
