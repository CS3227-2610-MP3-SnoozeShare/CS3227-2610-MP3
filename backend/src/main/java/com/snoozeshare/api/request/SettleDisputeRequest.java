package com.snoozeshare.api.request;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.snoozeshare.domain.enums.ResolutionMode;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Settle a dispute with a resolution mode and optional custom refund. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record SettleDisputeRequest(
        @NotNull ResolutionMode mode,
        @DecimalMin("0") @Digits(integer = 9, fraction = 2) BigDecimal guestRefund,
        @NotBlank @Size(max = 1000) String reason) {
}
