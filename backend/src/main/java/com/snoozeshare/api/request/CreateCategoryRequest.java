package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;

/** Create or rename a ticket category. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateCategoryRequest(@NotBlank String label) {
}
