package com.snoozeshare.api.mapper;

import java.util.List;

import com.snoozeshare.api.response.TicketCategoryResponse;
import com.snoozeshare.domain.model.TicketCategory;

/** Converts ticket categories to their response. */
public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static List<TicketCategoryResponse> toResponses(List<TicketCategory> categories) {
        return categories.stream().map(TicketCategoryResponse::from).toList();
    }

    public static TicketCategoryResponse toResponse(TicketCategory category) {
        return TicketCategoryResponse.from(category);
    }
}
