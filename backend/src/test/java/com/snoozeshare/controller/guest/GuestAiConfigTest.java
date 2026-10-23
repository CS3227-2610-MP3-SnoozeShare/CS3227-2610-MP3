package com.snoozeshare.controller.guest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.snoozeshare.ai.AiFeature;
import com.snoozeshare.ai.ModelTier;
import com.snoozeshare.ai.config.LlmProperties;
import com.snoozeshare.domain.enums.Role;

class GuestAiConfigTest {

    private final GuestAiConfig config = new GuestAiConfig();

    /** F1, F3: the feature bean has the spec-mandated settings. */
    @Test
    void featureRegistersAtStartup() {
        AiFeature<AiSearchFilters> feature = config.guestSearch();

        assertThat(feature.id()).isEqualTo("guest.search");
        assertThat(feature.allowedRoles()).isEqualTo(Set.of(Role.GUEST));
        assertThat(feature.tier()).isEqualTo(ModelTier.FAST);
        assertThat(feature.maxTokens()).isEqualTo(300);
        assertThat(feature.temperature()).isEqualTo(0.2);
        assertThat(feature.priority()).isEqualTo(2);
        assertThat(feature.perUserPerMinute()).isEqualTo(5);
        assertThat(feature.maxInputChars()).isEqualTo(500);
        assertThat(feature.maxTurns()).isEqualTo(1);
        assertThat(feature.output().isJson()).isTrue();
        assertThat(feature.output().jsonType()).isEqualTo(AiSearchFilters.class);
        assertThat(feature.output().allowLinks()).isFalse();

        // Fallback returns all-null filters with the default explanation
        AiSearchFilters fallback = feature.fallback().apply(null);
        assertThat(fallback.city()).isNull();
        assertThat(fallback.startDate()).isNull();
        assertThat(fallback.endDate()).isNull();
        assertThat(fallback.guests()).isNull();
        assertThat(fallback.maxNightlyRate()).isNull();
        assertThat(fallback.explanation()).isNotBlank();

        // Mock response is valid JSON
        assertThat(feature.mockResponse()).contains("\"city\"");
    }

    /** Validate with enabled=false (enabled demands URL/key). */
    @Test
    void featurePassesValidation() {
        AiFeature<AiSearchFilters> feature = config.guestSearch();
        LlmProperties properties = new LlmProperties(
            false, false, "", "", "fast", "standard", "strong",
            25, 4, 20, 10_000, 30_000, 45_000, 2, 5, 30_000, 900_000,
            2, 200, 3000, 5, 40_000, false, "none"
        );
        assertThatCode(() -> feature.validate(properties)).doesNotThrowAnyException();
    }
}
