package com.snoozeshare.controller.guest;

import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.snoozeshare.ai.AiFeature;
import com.snoozeshare.ai.ModelTier;
import com.snoozeshare.ai.OutputSpec;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.Role;

/**
 * Registers the {@code guest.search} AI feature bean.
 */
@Configuration
@ConditionalOnDatabase
public class GuestAiConfig {

    private static final String SYSTEM_PROMPT = """
        You are a search assistant for SnoozeShare, a short-term property rental platform.

        The user will describe what kind of accommodation they are looking for in natural language. \
        Extract structured search filters from their query and return a JSON object.

        Return ONLY a JSON object with these fields:
        - "city": string or null \u2014 the city or area name to search, or null if not mentioned
        - "startDate": string or null \u2014 check-in date in YYYY-MM-DD format, or null if not mentioned
        - "endDate": string or null \u2014 check-out date in YYYY-MM-DD format, or null if not mentioned
        - "guests": integer or null \u2014 number of guests, or null if not mentioned
        - "maxNightlyRate": number or null \u2014 maximum price per night in dollars, or null if not mentioned
        - "explanation": string (required) \u2014 a one-sentence summary of what you understood from the query

        Rules:
        - Only extract information explicitly stated or clearly implied in the query
        - Do not invent or assume values not present in the query
        - If a relative date is mentioned (e.g. "next weekend", "tomorrow"), calculate it relative to today
        - The explanation must be concise and describe the search in plain language
        - Return valid JSON only, no markdown fences, no extra text""";

    private static final String FALLBACK_EXPLANATION =
        "I couldn't understand that query. Please try the manual filters.";

    private static final String MOCK_RESPONSE = """
        {"city":"Singapore","startDate":null,"endDate":null,\
        "guests":2,"maxNightlyRate":null,\
        "explanation":"Looking for accommodation in Singapore for 2 guests."}""";

    /**
     * The guest AI search feature: extracts search filters from natural language.
     *
     * @return the registered feature bean
     */
    @Bean
    AiFeature<AiSearchFilters> guestSearch() {
        return new AiFeature<>(
            "guest.search",
            Set.of(Role.GUEST),
            ModelTier.FAST,
            SYSTEM_PROMPT,
            300,
            0.2,
            2,
            5,
            500,
            1,
            OutputSpec.json(AiSearchFilters.class, false),
            call -> new AiSearchFilters(null, null, null, null, null, FALLBACK_EXPLANATION),
            MOCK_RESPONSE
        );
    }
}
