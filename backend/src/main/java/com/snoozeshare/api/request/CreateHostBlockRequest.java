package com.snoozeshare.api.request;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/** Creates a manual availability block using inclusive API dates. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateHostBlockRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String reason) {

    @AssertTrue(message = "Reason must be at most 240 Unicode code points")
    public boolean isReasonWithinLimit() {
        return reason == null || reason.codePointCount(0, reason.length()) <= 240;
    }
}
