package com.snoozeshare.controller.guest;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The structured filters extracted by the LLM from a natural language guest search query.
 *
 * @param city city or area name, or null if not mentioned
 * @param startDate check-in date, or null if not mentioned
 * @param endDate check-out date, or null if not mentioned
 * @param guests number of guests, or null if not mentioned
 * @param maxNightlyRate maximum price per night in dollars, or null if not mentioned
 * @param explanation a one-sentence summary of the understood query (always present)
 */
public record AiSearchFilters(
    @Nullable String city,
    @Nullable LocalDate startDate,
    @Nullable LocalDate endDate,
    @Nullable @Min(1) Integer guests,
    @Nullable @DecimalMin("0.01") BigDecimal maxNightlyRate,
    @NotBlank @Size(max = 200) String explanation) {
}
