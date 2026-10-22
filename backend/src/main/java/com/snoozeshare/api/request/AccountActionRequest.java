package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Agent suspends or reactivates a user account. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record AccountActionRequest(@NotBlank @Size(max = 1000) String reason) {
}
