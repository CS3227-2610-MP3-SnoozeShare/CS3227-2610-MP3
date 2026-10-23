package com.snoozeshare.ai.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import com.snoozeshare.ProdEnvironmentGuard;

/** E34, prod half: the guard's AI rules, run against an environment that mirrors {@code application-prod.yml}. */
class ProdEnvironmentGuardAiTest {

    /** Every non-AI key the guard requires, so only the AI rules decide the outcome. */
    private static MockEnvironment env() {
        return new MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:postgresql://x/y")
            .withProperty("spring.datasource.username", "u")
            .withProperty("spring.datasource.password", "p")
            .withProperty("snoozeshare.cors.allowed-origin", "http://localhost:3000")
            .withProperty("snoozeshare.security.enabled", "true")
            .withProperty("snoozeshare.proxy.trust-forwarded-headers", "true")
            .withProperty("snoozeshare.firebase.enabled", "true")
            .withProperty("snoozeshare.firebase.project-id", "proj")
            .withProperty("snoozeshare.registration.host-code", "h")
            .withProperty("snoozeshare.registration.agent-code", "a")
            .withProperty("GOOGLE_APPLICATION_CREDENTIALS", "/tmp/x.json");
    }

    private static void run(MockEnvironment environment) {
        ProdEnvironmentGuard guard = new ProdEnvironmentGuard();
        guard.setEnvironment(environment);
        guard.postProcessBeanFactory(null);
    }

    @Test
    void unsetEnabledNamesTheVariable() {
        MockEnvironment environment = env().withProperty("snoozeshare.ai.enabled", "$" + "{SNOOZESHARE_AI_ENABLED}");
        assertThatThrownBy(() -> run(environment)).hasMessageContaining("SNOOZESHARE_AI_ENABLED");
    }

    @Test
    void disabledWithNoKeyPasses() {
        MockEnvironment environment = env().withProperty("snoozeshare.ai.enabled", "false");
        assertThatCode(() -> run(environment)).doesNotThrowAnyException();
    }

    @Test
    void enabledWithBlankKeyNamesTheKey() {
        MockEnvironment environment = env().withProperty("snoozeshare.ai.enabled", "true")
            .withProperty("snoozeshare.ai.base-url", "https://x.example/v1")
            .withProperty("snoozeshare.ai.api-key", "");
        assertThatThrownBy(() -> run(environment)).hasMessageContaining("snoozeshare.ai.api-key");
    }

    @Test
    void enabledWithBlankBaseUrlNamesTheKey() {
        MockEnvironment environment = env().withProperty("snoozeshare.ai.enabled", "true")
            .withProperty("snoozeshare.ai.base-url", "")
            .withProperty("snoozeshare.ai.api-key", "clsk_ab_cd");
        assertThatThrownBy(() -> run(environment)).hasMessageContaining("snoozeshare.ai.base-url");
    }

    @Test
    void mockIsRejectedWhetherEnabledOrNot() {
        for (String enabled : new String[] {"true", "false"}) {
            MockEnvironment environment = env().withProperty("snoozeshare.ai.enabled", enabled)
                .withProperty("snoozeshare.ai.mock", "true")
                .withProperty("snoozeshare.ai.base-url", "https://x.example/v1")
                .withProperty("snoozeshare.ai.api-key", "clsk_ab_cd");
            assertThatThrownBy(() -> run(environment)).hasMessageContaining("snoozeshare.ai.mock");
        }
    }

    @Test
    void baseUrlMustBeHttps() {
        MockEnvironment environment = env().withProperty("snoozeshare.ai.enabled", "true")
            .withProperty("snoozeshare.ai.base-url", "http://localhost:8089/v1")
            .withProperty("snoozeshare.ai.api-key", "clsk_ab_cd");
        assertThatThrownBy(() -> run(environment)).hasMessageContaining("https");
    }

    @Test
    void enabledWithKeyAndHttpsUrlPasses() {
        MockEnvironment environment = env().withProperty("snoozeshare.ai.enabled", "true")
            .withProperty("snoozeshare.ai.base-url", "https://x.example/v1")
            .withProperty("snoozeshare.ai.api-key", "clsk_ab_cd");
        assertThatCode(() -> run(environment)).doesNotThrowAnyException();
    }
}
