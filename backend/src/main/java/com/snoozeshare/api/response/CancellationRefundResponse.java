package com.snoozeshare.api.response;

import java.math.BigDecimal;

import com.snoozeshare.service.CancellationRefund;

public record CancellationRefundResponse(BigDecimal amount, int percent) {

    public static CancellationRefundResponse from(CancellationRefund r) {
        return new CancellationRefundResponse(r.amount(), r.percent());
    }
}
