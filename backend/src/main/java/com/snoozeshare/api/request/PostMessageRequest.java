package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;

/** Post a message in a ticket thread or booking conversation. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record PostMessageRequest(@NotBlank String body) {
}
