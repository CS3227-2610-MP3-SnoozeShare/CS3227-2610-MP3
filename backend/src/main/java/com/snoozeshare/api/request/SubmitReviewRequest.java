package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Guest submits a review for a completed booking. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record SubmitReviewRequest(
        @Min(1) @Max(5) int rating,
        @NotBlank @Size(max = 2000) String comment) {
}
