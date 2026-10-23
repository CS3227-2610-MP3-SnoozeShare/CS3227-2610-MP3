package com.snoozeshare.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import com.snoozeshare.PostgresIntegrationTest;
import com.snoozeshare.testsupport.FoundationTestController;

/** Proves the test agent route is not registered in the permit-all profile. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "snoozeshare.security.enabled=false")
@AutoConfigureMockMvc
class ApiSecurityDisabledIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void agentRouteIsNotRegisteredWhenSecurityIsDisabled() throws Exception {
        assertThat(applicationContext.getBeansOfType(FoundationTestController.class)).isEmpty();
        mockMvc.perform(get("/api/agent/_test/ping"))
                .andExpect(status().isNotFound());
    }
}
