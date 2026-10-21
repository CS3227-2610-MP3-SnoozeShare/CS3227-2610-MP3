package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Agent saves internal notes on a ticket. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record SaveNotesRequest(String notes) {
}
