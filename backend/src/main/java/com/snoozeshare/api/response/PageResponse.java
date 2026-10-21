package com.snoozeshare.api.response;

import java.util.List;

/** A bounded page with a flag indicating whether another page is available. */
public record PageResponse<T>(List<T> items, int page, int size, boolean hasMore) {

    public PageResponse {
        items = List.copyOf(items);
    }

    public static <T> PageResponse<T> from(List<T> fetched, int page, int size) {
        boolean hasMore = fetched.size() > size;
        List<T> items = fetched.subList(0, Math.min(size, fetched.size()));
        return new PageResponse<>(items, page, size, hasMore);
    }
}
