package com.snoozeshare.api.request;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Wallet top-up or withdrawal amount. */
@JsonIgnoreProperties(ignoreUnknown = false)
public record WalletAmountRequest(@NotNull @Positive BigDecimal amount) {
}
