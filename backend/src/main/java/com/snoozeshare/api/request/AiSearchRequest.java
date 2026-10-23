package com.snoozeshare.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for the AI search endpoint.
 *
 * @param query the natural language search query (1-500 characters)
 */
public record AiSearchRequest(@NotBlank @Size(min = 1, max = 500) String query) {
}
