package com.snoozeshare.api.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotNull;

/** Block dates on a property for host-side unavailability. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateHostBlockRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String reason) {
}
