package com.snoozeshare.api.request;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotNull;

/** Submit a booking request for a property. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateBookingRequest(
        @NotNull UUID propertyId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {
}
