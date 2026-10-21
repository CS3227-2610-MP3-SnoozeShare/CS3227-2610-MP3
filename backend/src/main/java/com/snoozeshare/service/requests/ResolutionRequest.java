package com.snoozeshare.service.requests;

import java.math.BigDecimal;

import com.snoozeshare.domain.enums.ResolutionMode;

public record ResolutionRequest(ResolutionMode mode, BigDecimal guestRefund, String reason) {
}
