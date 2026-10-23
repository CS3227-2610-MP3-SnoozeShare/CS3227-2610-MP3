package com.snoozeshare.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.snoozeshare.ai.config.LlmProperties;

/**
 * Collects the {@link AiFeature} beans, validates each and rejects duplicate ids, so a bad registration fails
 * startup instead of a user's request. It has no database dependency and exists in every context; zero features
 * is valid.
 */
@Component
public class AiFeatureRegistry {

    private final Map<String, AiFeature<?>> features = new LinkedHashMap<>();

    /**
     * Builds the registry.
     *
     * @param beans every registered feature
     * @param properties the gateway settings
     */
    public AiFeatureRegistry(ObjectProvider<AiFeature<?>> beans, LlmProperties properties) {
        beans.orderedStream().forEach(feature -> {
            feature.validate(properties);
            if (features.putIfAbsent(feature.id(), feature) != null) {
                throw new IllegalArgumentException("AI feature '" + feature.id() + "': id is registered twice");
            }
        });
    }

    /**
     * Looks a feature up by id.
     *
     * @param id the feature id
     * @return the feature, or empty
     */
    public Optional<AiFeature<?>> find(String id) {
        return Optional.ofNullable(features.get(id));
    }

    /**
     * Lists every registered feature.
     *
     * @return the features in registration order
     */
    public List<AiFeature<?>> all() {
        return List.copyOf(features.values());
    }
}
