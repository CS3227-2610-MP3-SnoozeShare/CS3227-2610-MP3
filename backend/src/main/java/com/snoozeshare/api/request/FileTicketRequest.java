package com.snoozeshare.api.request;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.snoozeshare.domain.enums.RemedyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** File a support ticket or dispute. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record FileTicketRequest(
        @NotNull UUID bookingId,
        @NotBlank String category,
        @NotBlank String title,
        @NotBlank String description,
        RemedyType requestedRemedy,
        String supportingText) {
}
