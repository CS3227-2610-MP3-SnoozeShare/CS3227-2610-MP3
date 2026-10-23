package com.snoozeshare.controller.guest;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.snoozeshare.ai.AiFeature;
import com.snoozeshare.ai.LlmCall;
import com.snoozeshare.ai.LlmGateway;
import com.snoozeshare.ai.LlmResult;
import com.snoozeshare.ai.Turn;
import com.snoozeshare.ai.TurnRole;
import com.snoozeshare.api.response.AiSearchFiltersResponse;
import com.snoozeshare.api.response.AiSearchResponse;
import com.snoozeshare.api.response.SearchResultResponse;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.repository.SearchCriteria;
import com.snoozeshare.service.ListingService;

/**
 * Runs an AI-assisted guest search: sends the query to the LLM, extracts filters, and searches.
 */
@Service
@ConditionalOnDatabase
public class GuestAiSearchService {

    private static final Logger log = LoggerFactory.getLogger(GuestAiSearchService.class);
    private static final String INVALID_DATES_CODE = "INVALID_SEARCH_DATES";
    private static final String INVALID_DATES_EXPLANATION =
        "The AI extracted dates that could not be searched. Please try again or use the manual filters.";

    private final LlmGateway gateway;
    private final AiFeature<AiSearchFilters> feature;
    private final ListingService listingService;
    private final Clock clock;

    GuestAiSearchService(LlmGateway gateway, AiFeature<AiSearchFilters> feature,
                         ListingService listingService, Clock clock) {
        this.gateway = gateway;
        this.feature = feature;
        this.listingService = listingService;
        this.clock = clock;
    }

    /**
     * Runs an AI search for the given user and query.
     *
     * @param userId the authenticated user's id
     * @param query the natural language search query
     * @return the search response
     */
    AiSearchResponse search(UUID userId, String query) {
        LocalDate today = LocalDate.now(clock);
        String trustedContext = "Today is " + today + ", " + today.getDayOfWeek() + ".";

        LlmCall call = new LlmCall(
            userId, Role.GUEST,
            List.of(new Turn(TurnRole.USER, query)),
            trustedContext, null, null);

        LlmResult<AiSearchFilters> result = gateway.run(feature, call);

        if (!result.ok()) {
            return degraded(result.value().explanation(), result.code().name());
        }

        if (result.flagged()) {
            log.warn("AI search flagged input: userId={}, featureId={}", userId, feature.id());
        }

        AiSearchFilters filters = result.value();

        List<SearchResultResponse> results;
        try {
            SearchCriteria criteria = new SearchCriteria(
                filters.city(), filters.guests(),
                filters.startDate(), filters.endDate(),
                filters.maxNightlyRate());
            results = listingService.search(criteria).stream()
                .map(SearchResultResponse::from)
                .toList();
        } catch (IllegalArgumentException e) {
            return degraded(INVALID_DATES_EXPLANATION, INVALID_DATES_CODE);
        }

        return new AiSearchResponse(
            results,
            filters.explanation(),
            AiSearchFiltersResponse.from(filters),
            true,
            null);
    }

    private static AiSearchResponse degraded(String explanation, String errorCode) {
        return new AiSearchResponse(List.of(), explanation, null, false, errorCode);
    }
}
