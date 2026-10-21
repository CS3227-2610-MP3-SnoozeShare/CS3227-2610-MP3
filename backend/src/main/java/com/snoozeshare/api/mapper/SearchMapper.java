package com.snoozeshare.api.mapper;

import com.snoozeshare.api.request.SearchListingsRequest;
import com.snoozeshare.repository.SearchCriteria;

/** Maps search request DTOs to repository criteria. */
public final class SearchMapper {

    private SearchMapper() {
    }

    public static SearchCriteria toCriteria(SearchListingsRequest req) {
        return new SearchCriteria(req.city(), req.guests(),
                req.startDate(), req.endDate(), req.maxNightlyRate());
    }
}
