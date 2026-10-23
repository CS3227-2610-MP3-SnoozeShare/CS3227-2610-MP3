package com.snoozeshare.api.request;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.snoozeshare.domain.enums.RemedyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** File a support ticket or dispute. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record FileTicketRequest(
        @NotNull UUID bookingId,
        @NotBlank String category,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotNull RemedyType requestedRemedy,
        @Size(max = 5000) String supportingText) {
}
