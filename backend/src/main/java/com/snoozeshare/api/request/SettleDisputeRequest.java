package com.snoozeshare.api.request;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.snoozeshare.domain.enums.ResolutionMode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Settle a dispute with a resolution mode and optional custom refund. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record SettleDisputeRequest(
        @NotNull ResolutionMode mode,
        BigDecimal guestRefund,
        @NotBlank String reason) {
}
