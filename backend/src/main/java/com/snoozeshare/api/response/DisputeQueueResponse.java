package com.snoozeshare.api.response;

import java.util.List;

/** The dispute queue plus the global count of open tickets nobody has taken. */
public record DisputeQueueResponse(List<DisputeSummaryResponse> items, int unassignedCount) {
}
