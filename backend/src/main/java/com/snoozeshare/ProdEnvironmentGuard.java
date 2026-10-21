package com.snoozeshare;

import java.util.List;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import com.snoozeshare.ai.config.LlmProperties;

/**
 * Fails startup under the {@code prod} profile when a required variable is unset, naming it.
 *
 * <p>Spring's property binder leaves an unresolved {@code ${VAR}} placeholder in place, so without this
 * check the failure would surface later as an unrelated driver error that does not name the variable.
 * Reading through {@link Environment#getProperty} resolves placeholders strictly and names the first
 * missing one.
 */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProdEnvironmentGuard implements BeanFactoryPostProcessor, EnvironmentAware {

    private static final List<String> REQUIRED_KEYS = List.of(
        "spring.datasource.url",
        "spring.datasource.username",
        "spring.datasource.password",
        "snoozeshare.cors.allowed-origin",
        "snoozeshare.security.enabled",
        "snoozeshare.proxy.trust-forwarded-headers",
        "snoozeshare.firebase.enabled",
        "snoozeshare.firebase.project-id",
        "snoozeshare.registration.host-code",
        "snoozeshare.registration.agent-code",
        "snoozeshare.ai.enabled",
        "GOOGLE_APPLICATION_CREDENTIALS");

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        for (String key : REQUIRED_KEYS) {
            environment.getRequiredProperty(key);
        }
        requireTrue("snoozeshare.security.enabled");
        requireTrue("snoozeshare.firebase.enabled");
        checkAi();
    }

    /** The AI gateway may be off in production, but never mocked, and when on it needs a key and an https URL. */
    private void checkAi() {
        if (Boolean.parseBoolean(environment.getProperty("snoozeshare.ai.mock", "false"))) {
            throw new IllegalStateException("snoozeshare.ai.mock must be false in the prod profile");
        }
        if (Boolean.parseBoolean(environment.getRequiredProperty("snoozeshare.ai.enabled"))) {
            requireNotBlank("snoozeshare.ai.base-url");
            requireNotBlank("snoozeshare.ai.api-key");
            if (!environment.getRequiredProperty("snoozeshare.ai.base-url").startsWith("https://")) {
                throw new IllegalStateException("snoozeshare.ai.base-url must be https in the prod profile");
            }
        }
    }

    private void requireNotBlank(String key) {
        String value = environment.getProperty(key);
        if (LlmProperties.isBlankOrPlaceholder(value)) {
            throw new IllegalStateException(
                key + " must be set in the prod profile when snoozeshare.ai.enabled is true");
        }
    }

    private void requireTrue(String key) {
        if (!Boolean.parseBoolean(environment.getRequiredProperty(key))) {
            throw new IllegalStateException(key + " must be true in the prod profile");
        }
    }
}
