package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;

/** Agent suspends or reactivates a user account. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record AccountActionRequest(@NotBlank String reason) {
}
