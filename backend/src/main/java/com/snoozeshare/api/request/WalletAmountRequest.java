package com.snoozeshare.api.request;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Wallet top-up or withdrawal amount. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record WalletAmountRequest(
        @NotNull @Positive @DecimalMin("0.01") @DecimalMax("100000.00")
        @Digits(integer = 6, fraction = 2) BigDecimal amount) {
}
