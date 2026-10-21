package com.snoozeshare.api.response;

import java.math.BigDecimal;

import com.snoozeshare.service.PriceBreakdown;

public record PriceBreakdownResponse(BigDecimal nightlyRate, int nights, BigDecimal totalAmount) {

    public static PriceBreakdownResponse from(PriceBreakdown p) {
        return new PriceBreakdownResponse(p.nightlyRate(), p.nights(), p.totalAmount());
    }
}
