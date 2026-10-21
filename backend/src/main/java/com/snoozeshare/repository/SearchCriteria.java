package com.snoozeshare.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Criteria for searching available properties. */
public record SearchCriteria(
        String city,
        Integer guests,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal maxNightlyRate) {

    public SearchCriteria(String city, Integer guests,
                          LocalDate startDate, LocalDate endDate) {
        this(city, guests, startDate, endDate, null);
    }
}
