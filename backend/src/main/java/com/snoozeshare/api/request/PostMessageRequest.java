package com.snoozeshare.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Post a message in a ticket thread or booking conversation. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record PostMessageRequest(@NotBlank @Size(max = 5000) String body) {
}
