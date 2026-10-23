package com.snoozeshare.api.response;

import java.util.List;

/**
 * Response body for the AI search endpoint.
 *
 * @param results the search results (empty when degraded)
 * @param explanation the AI's summary or a fallback message
 * @param filters the extracted search filters (null when degraded)
 * @param aiAvailable true when the AI processed the query successfully
 * @param errorCode the error code when degraded, null otherwise
 */
public record AiSearchResponse(List<SearchResultResponse> results, String explanation,
                                AiSearchFiltersResponse filters, boolean aiAvailable,
                                String errorCode) {
}
