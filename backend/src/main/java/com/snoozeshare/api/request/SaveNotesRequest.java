package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.Size;

/** Agent saves internal notes on a ticket. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record SaveNotesRequest(@Size(max = 4000) String notes) {
}
