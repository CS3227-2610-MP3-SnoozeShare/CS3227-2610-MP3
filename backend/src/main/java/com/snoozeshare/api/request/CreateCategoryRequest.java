package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Create or rename a ticket category. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateCategoryRequest(@NotBlank @Size(max = 60) String label) {
}
