package com.snoozeshare.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import com.snoozeshare.service.ListingSummaryMetrics;

class ListingMetricsServiceImplTest {

    @Test
    void summaryMetricsForHostUsesTwoGroupedQueriesForOneAnd101Listings() {
        for (int listingCount : List.of(1, 101)) {
            JdbcTemplate jdbc = mock(JdbcTemplate.class);
            UUID hostId = UUID.randomUUID();
            List<Map<String, Object>> bookingRows = IntStream.range(0, listingCount)
                .mapToObj(index -> Map.<String, Object>of(
                    "listing_id", UUID.randomUUID(), "booking_count", (long) index))
                .toList();
            when(jdbc.queryForList(anyString(), any(Object[].class)))
                .thenReturn(bookingRows)
                .thenReturn(List.of());

            ListingMetricsServiceImpl service = new ListingMetricsServiceImpl(jdbc, Clock.systemUTC());
            Map<UUID, ListingSummaryMetrics> summaries = service.summaryMetricsForHost(hostId);

            assertThat(summaries).hasSize(listingCount);
            assertThat(summaries.values()).allSatisfy(summary -> assertThat(summary.averageRating()).isZero());
            ArgumentCaptor<String> statements = ArgumentCaptor.forClass(String.class);
            verify(jdbc, times(2)).queryForList(statements.capture(), eq(hostId));
            assertThat(statements.getAllValues()).allSatisfy(statement ->
                assertThat(statement).contains("p.host_id = ?", "GROUP BY p.id"));
        }
    }

    @Test
    void summaryMetricsForHostUsesTwoGroupedQueriesAndMapsZeros() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        UUID hostId = UUID.randomUUID();
        UUID reviewedListingId = UUID.randomUUID();
        UUID unreviewedListingId = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), any(Object[].class)))
            .thenReturn(List.of(
                Map.of("listing_id", reviewedListingId, "booking_count", 3L),
                Map.of("listing_id", unreviewedListingId, "booking_count", 0L)))
            .thenReturn(List.of(Map.of("listing_id", reviewedListingId, "average_rating", 4.5)));

        ListingMetricsServiceImpl service = new ListingMetricsServiceImpl(jdbc, Clock.systemUTC());
        Map<UUID, ListingSummaryMetrics> summaries = service.summaryMetricsForHost(hostId);

        assertThat(summaries).containsOnlyKeys(reviewedListingId, unreviewedListingId);
        assertThat(summaries.get(reviewedListingId).bookingCount()).isEqualTo(3);
        assertThat(summaries.get(reviewedListingId).averageRating()).isEqualTo(4.5);
        assertThat(summaries.get(unreviewedListingId).bookingCount()).isZero();
        assertThat(summaries.get(unreviewedListingId).averageRating()).isZero();
        ArgumentCaptor<String> statements = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(2)).queryForList(statements.capture(), eq(hostId));
        assertThat(statements.getAllValues()).allSatisfy(statement ->
            assertThat(statement).contains("p.host_id = ?", "GROUP BY p.id"));
    }
}
