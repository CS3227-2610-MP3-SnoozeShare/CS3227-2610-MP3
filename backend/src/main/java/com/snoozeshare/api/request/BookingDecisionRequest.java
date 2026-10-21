package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotNull;

/** Host approves or rejects a booking request. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record BookingDecisionRequest(
        @NotNull Boolean approve,
        String message) {
}
