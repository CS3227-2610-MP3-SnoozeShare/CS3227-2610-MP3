package com.snoozeshare.api.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.snoozeshare.controller.guest.AiSearchFilters;

/**
 * The extracted search filters returned to the client, without the explanation.
 *
 * @param city city or area name
 * @param startDate check-in date
 * @param endDate check-out date
 * @param guests number of guests
 * @param maxNightlyRate maximum price per night in dollars
 */
public record AiSearchFiltersResponse(String city, LocalDate startDate, LocalDate endDate,
                                      Integer guests, BigDecimal maxNightlyRate) {

    /**
     * Builds the response from the LLM output record.
     *
     * @param filters the extracted filters
     * @return the response DTO
     */
    public static AiSearchFiltersResponse from(AiSearchFilters filters) {
        return new AiSearchFiltersResponse(
            filters.city(), filters.startDate(), filters.endDate(),
            filters.guests(), filters.maxNightlyRate());
    }
}
