package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;

/** Resolve a support ticket. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record ResolveTicketRequest(@NotBlank String resolutionReason) {
}
