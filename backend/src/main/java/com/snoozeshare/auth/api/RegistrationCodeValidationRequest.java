package com.snoozeshare.auth.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.snoozeshare.auth.model.AuthRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Client input for pre-validating a privileged registration code. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record RegistrationCodeValidationRequest(
    @NotNull AuthRole role,
    @NotBlank @Size(max = 128) String registrationCode) { }
