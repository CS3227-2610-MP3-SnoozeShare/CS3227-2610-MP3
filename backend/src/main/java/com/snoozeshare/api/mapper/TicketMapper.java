package com.snoozeshare.api.mapper;

import com.snoozeshare.api.request.FileTicketRequest;
import com.snoozeshare.service.requests.NewTicketRequest;

/** Maps ticket request DTOs to service-layer request types. */
public final class TicketMapper {

    private TicketMapper() {
    }

    public static NewTicketRequest toServiceRequest(FileTicketRequest req) {
        return new NewTicketRequest(req.bookingId(), req.category(), req.title(),
                req.description(), req.requestedRemedy(), req.supportingText());
    }
}
