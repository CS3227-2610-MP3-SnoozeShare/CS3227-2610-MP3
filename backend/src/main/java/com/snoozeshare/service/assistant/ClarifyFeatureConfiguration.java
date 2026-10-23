package com.snoozeshare.service.assistant;

import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.snoozeshare.ai.AiFeature;
import com.snoozeshare.ai.ModelTier;
import com.snoozeshare.ai.OutputSpec;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.Role;

/** Registers the assistant contract without changing shared gateway behavior. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDatabase
@ConditionalOnProperty(prefix = "snoozeshare.assistant", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AssistantProperties.class)
public class ClarifyFeatureConfiguration {
    /** Guest-only fact gathering, with the spec's credential-free mock reply. */
    @Bean
    @ConditionalOnDatabase
    @ConditionalOnProperty(prefix = "snoozeshare.assistant", name = "enabled",
        havingValue = "true", matchIfMissing = true)
    public AiFeature<ClarifyOutput> clarifyFeature() {
        return new AiFeature<>("ticket.clarify", Set.of(Role.GUEST), ModelTier.STANDARD,
            ClarifyPrompt.TEXT, 2500, 0.2, 1, 6, 2000, 10,
            OutputSpec.json(ClarifyOutput.class, false), call -> ClarifyOutput.NONE,
            "{\"enough\":false,\"questions\":[\"On which date did the problem first happen?\"],"
                + "\"summary\":\"Mock summary.\",\"timeline\":[],\"claims\":[],\"evidence\":[],"
                + "\"missing\":[\"Date of the problem\"],\"hostPoints\":[]}");
    }
}
