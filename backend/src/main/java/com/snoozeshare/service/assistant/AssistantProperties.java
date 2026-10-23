package com.snoozeshare.service.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Validated assistant settings, separate from the shared gateway's switch and limits. */
@ConfigurationProperties("snoozeshare.assistant")
public record AssistantProperties(@DefaultValue("true") boolean enabled,
                                  @DefaultValue("3") int maxRounds,
                                  @DefaultValue("2") int threads,
                                  @DefaultValue("50") int queue,
                                  @DefaultValue("10") int staleAfterMinutes) {
    /** Fails startup with the exact configuration key and its permitted range. */
    public AssistantProperties {
        range("max-rounds", maxRounds, 1, 5);
        range("threads", threads, 1, 4);
        range("queue", queue, 1, 500);
        range("stale-after-minutes", staleAfterMinutes, 2, 120);
    }

    private static void range(String key, int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException("snoozeshare.assistant." + key + " must be " + min + " to " + max);
        }
    }
}
