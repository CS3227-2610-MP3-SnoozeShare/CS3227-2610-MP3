package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotNull;

/** Activate or deactivate a ticket category. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record SetCategoryActiveRequest(@NotNull Boolean active) {
}
