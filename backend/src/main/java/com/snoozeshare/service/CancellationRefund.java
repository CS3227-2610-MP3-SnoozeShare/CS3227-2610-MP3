package com.snoozeshare.service;

import java.math.BigDecimal;

public record CancellationRefund(BigDecimal amount, int percent) {
}
