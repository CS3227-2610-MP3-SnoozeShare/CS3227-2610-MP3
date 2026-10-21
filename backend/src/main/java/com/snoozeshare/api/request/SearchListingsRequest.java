package com.snoozeshare.api.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Search listings with optional filters. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record SearchListingsRequest(
        String city,
        LocalDate startDate,
        LocalDate endDate,
        Integer guests,
        BigDecimal maxNightlyRate) {
}
