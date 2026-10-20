package com.snoozeshare.auth.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.snoozeshare.auth.model.AuthRole;

import jakarta.validation.constraints.NotNull;

/** Client input for first-time backend profile registration. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record RegisterRequest(
    @NotNull AuthRole role,
    String registrationCode,
    String displayName) { }
