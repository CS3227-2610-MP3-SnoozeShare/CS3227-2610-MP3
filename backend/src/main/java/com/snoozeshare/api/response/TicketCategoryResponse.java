package com.snoozeshare.api.response;

import java.util.UUID;

import com.snoozeshare.domain.model.TicketCategory;

public record TicketCategoryResponse(UUID id, String label, boolean active) {

    public static TicketCategoryResponse from(TicketCategory c) {
        return new TicketCategoryResponse(c.getId(), c.getLabel(), c.isActive());
    }
}
