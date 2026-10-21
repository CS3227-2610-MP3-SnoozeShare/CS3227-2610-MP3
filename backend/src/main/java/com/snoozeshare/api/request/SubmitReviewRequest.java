package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** Guest submits a review for a completed booking. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record SubmitReviewRequest(
        @Min(1) @Max(5) int rating,
        @NotBlank String comment) {
}
