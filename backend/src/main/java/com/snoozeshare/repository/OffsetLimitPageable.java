package com.snoozeshare.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Applies lookahead limits while keeping page offsets based on the requested page size. */
public final class OffsetLimitPageable implements Pageable {

    private final int page;
    private final int requestedSize;

    public OffsetLimitPageable(int page, int requestedSize) {
        if (page < 0 || requestedSize < 1 || requestedSize >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid page or requested size");
        }
        this.page = page;
        this.requestedSize = requestedSize;
    }

    @Override
    public int getPageNumber() {
        return page;
    }

    @Override
    public int getPageSize() {
        return requestedSize + 1;
    }

    @Override
    public long getOffset() {
        return (long) page * requestedSize;
    }

    @Override
    public Sort getSort() {
        return Sort.unsorted();
    }

    @Override
    public Pageable next() {
        return new OffsetLimitPageable(page + 1, requestedSize);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetLimitPageable(page - 1, requestedSize) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetLimitPageable(0, requestedSize);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetLimitPageable(pageNumber, requestedSize);
    }

    @Override
    public boolean hasPrevious() {
        return page > 0;
    }
}
