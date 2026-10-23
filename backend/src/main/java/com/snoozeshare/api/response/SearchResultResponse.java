package com.snoozeshare.api.response;

import com.snoozeshare.service.SearchResult;

public record SearchResultResponse(PropertyResponse property, boolean available) {

    public static SearchResultResponse from(SearchResult r) {
        return new SearchResultResponse(PropertyResponse.from(r.property()), r.available());
    }
}
