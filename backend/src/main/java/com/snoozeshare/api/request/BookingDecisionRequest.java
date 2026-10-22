package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** Host approves or rejects a booking request. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record BookingDecisionRequest(
        @NotNull Boolean approve,
        String message) {

    @AssertTrue(message = "Message must be at most 500 Unicode code points")
    public boolean isMessageWithinLimit() {
        return message == null || message.codePointCount(0, message.length()) <= 500;
    }
}
