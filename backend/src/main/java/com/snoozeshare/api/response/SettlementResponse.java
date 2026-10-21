package com.snoozeshare.api.response;

import java.math.BigDecimal;

import com.snoozeshare.service.Settlement;

public record SettlementResponse(
        TicketResponse ticket,
        BookingResponse booking,
        BigDecimal guestRefund,
        BigDecimal hostPayout,
        BigDecimal platformFee,
        TransactionResponse guestTransaction,
        TransactionResponse hostTransaction) {

    public static SettlementResponse from(Settlement s) {
        return new SettlementResponse(
                TicketResponse.from(s.ticket()),
                BookingResponse.from(s.booking()),
                s.breakdown().guestRefund(),
                s.breakdown().hostNet(),
                s.breakdown().fee(),
                s.guestTransaction() != null
                        ? TransactionResponse.from(s.guestTransaction()) : null,
                s.hostTransaction() != null
                        ? TransactionResponse.from(s.hostTransaction()) : null);
    }
}
