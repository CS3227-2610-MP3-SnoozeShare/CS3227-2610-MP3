package com.snoozeshare.ai.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.ai.client.SoclaasClient;
import com.snoozeshare.ai.guard.OutputValidator;
import com.snoozeshare.ai.guard.PromptBuilder;
import com.snoozeshare.ai.limit.Backoff;
import com.snoozeshare.ai.limit.BudgetBlock;
import com.snoozeshare.ai.limit.CircuitBreaker;
import com.snoozeshare.ai.limit.Jitter;
import com.snoozeshare.ai.limit.LlmScheduler;
import com.snoozeshare.ai.limit.Sleeper;
import com.snoozeshare.ai.limit.UserThrottle;

import jakarta.validation.Validator;

/**
 * Binds {@link LlmProperties} in every context and defines the gateway's collaborators that need no database.
 * {@code LlmGateway}, {@code AiAuditWriter} and {@code ModelCheck} are components that exist only where the
 * repositories do, so a datasource-free context starts without a gateway.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LlmProperties.class)
public class AiConfig {

    @Bean
    PromptBuilder promptBuilder() {
        return new PromptBuilder();
    }

    @Bean
    @Lazy
    OutputValidator outputValidator(ObjectMapper mapper, Validator validator) {
        return new OutputValidator(mapper, validator);
    }

    @Bean
    SoclaasClient soclaasClient(LlmProperties props) {
        return new SoclaasClient(props);
    }

    @Bean
    LlmScheduler llmScheduler(LlmProperties props, Clock clock) {
        return new LlmScheduler(props, clock);
    }

    @Bean
    UserThrottle userThrottle(LlmProperties props, Clock clock) {
        return new UserThrottle(clock, props.perUserMaxInFlight());
    }

    @Bean
    CircuitBreaker circuitBreaker(LlmProperties props, Clock clock) {
        return new CircuitBreaker(clock, props.breakerFailures(), props.breakerOpenMs());
    }

    @Bean
    BudgetBlock budgetBlock(LlmProperties props, Clock clock) {
        return new BudgetBlock(clock, props.budgetBlockMs());
    }

    @Bean
    Backoff backoff() {
        return new Backoff(Jitter.SYSTEM);
    }

    @Bean
    Sleeper sleeper() {
        return Sleeper.SYSTEM;
    }
}
