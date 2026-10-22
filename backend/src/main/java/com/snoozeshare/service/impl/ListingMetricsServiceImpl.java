package com.snoozeshare.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.service.ListingMetrics;
import com.snoozeshare.service.ListingMetricsService;
import com.snoozeshare.service.ListingReview;
import com.snoozeshare.service.ListingSummaryMetrics;

@Service
@ConditionalOnDatabase
public class ListingMetricsServiceImpl implements ListingMetricsService {

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public ListingMetricsServiceImpl(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public ListingMetrics metricsFor(UUID propertyId) {
        Instant now = clock.instant();
        LocalDate windowEnd = LocalDate.ofInstant(now, ZoneOffset.UTC);
        LocalDate windowStart = windowEnd.minusDays(30);

        int bookingCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE listing_id = ?",
                Integer.class, propertyId);

        long bookedNights = 0;
        var dates = jdbc.queryForList(
                "SELECT start_date, end_date FROM bookings WHERE listing_id = ? "
                        + "AND status IN ('CONFIRMED', 'COMPLETED', 'FORCE_COMPLETED')",
                propertyId);
        for (var row : dates) {
            LocalDate start = ((java.sql.Date) row.get("start_date")).toLocalDate();
            LocalDate end = ((java.sql.Date) row.get("end_date")).toLocalDate();
            LocalDate overlapStart = start.isAfter(windowStart) ? start : windowStart;
            LocalDate overlapEnd = end.isBefore(windowEnd) ? end : windowEnd;
            if (overlapStart.isBefore(overlapEnd)) {
                bookedNights += ChronoUnit.DAYS.between(overlapStart, overlapEnd);
            }
        }

        List<ListingReview> reviews = new ArrayList<>();
        int ratingTotal = 0;
        var reviewRows = jdbc.queryForList(
                "SELECT u.display_name, r.rating, r.comment FROM reviews r "
                        + "JOIN bookings b ON b.id = r.booking_id "
                        + "JOIN users u ON u.id = r.guest_id "
                        + "WHERE b.listing_id = ? ORDER BY r.created_at DESC",
                propertyId);
        for (var row : reviewRows) {
            int rating = ((Number) row.get("rating")).intValue();
            ratingTotal += rating;
            reviews.add(new ListingReview(
                    (String) row.get("display_name"), rating,
                    (String) row.get("comment")));
        }
        double averageRating = reviews.isEmpty() ? 0.0
                : (double) ratingTotal / reviews.size();

        Instant windowStartInstant = windowStart.atStartOfDay().toInstant(ZoneOffset.UTC);
        BigDecimal earnings = jdbc.queryForObject(
                "SELECT COALESCE(SUM(a.wallet_adjustment), 0) FROM audit_log a "
                        + "JOIN bookings b ON b.id = a.booking_id "
                        + "WHERE b.listing_id = ? AND a.action_type = 'BOOKING_PAYOUT' "
                        + "AND a.wallet_adjustment IS NOT NULL "
                        + "AND a.timestamp >= ? AND a.timestamp <= ?",
                BigDecimal.class, propertyId, java.sql.Timestamp.from(windowStartInstant),
                java.sql.Timestamp.from(now));

        double occupancyPercentage = Math.min(100.0, bookedNights / 30.0 * 100.0);
        return new ListingMetrics(bookingCount, averageRating, reviews, occupancyPercentage,
                earnings);
    }

    @Override
    public Map<UUID, ListingSummaryMetrics> summaryMetricsForHost(UUID hostId) {
        Map<UUID, Long> bookingCounts = new HashMap<>();
        jdbc.queryForList(
                "SELECT p.id AS listing_id, COUNT(b.id) AS booking_count "
                        + "FROM properties p LEFT JOIN bookings b ON b.listing_id = p.id "
                        + "WHERE p.host_id = ? GROUP BY p.id",
                hostId).forEach(row -> bookingCounts.put(
                (UUID) row.get("listing_id"), ((Number) row.get("booking_count")).longValue()));

        Map<UUID, Double> averageRatings = new HashMap<>();
        jdbc.queryForList(
                "SELECT p.id AS listing_id, AVG(r.rating) AS average_rating "
                        + "FROM properties p "
                        + "LEFT JOIN bookings b ON b.listing_id = p.id "
                        + "LEFT JOIN reviews r ON r.booking_id = b.id "
                        + "WHERE p.host_id = ? GROUP BY p.id",
                hostId).forEach(row -> {
                    Object average = row.get("average_rating");
                    averageRatings.put((UUID) row.get("listing_id"),
                            average == null ? 0.0 : ((Number) average).doubleValue());
                });

        Map<UUID, ListingSummaryMetrics> summaries = new HashMap<>();
        bookingCounts.forEach((listingId, bookingCount) -> summaries.put(listingId,
                new ListingSummaryMetrics(bookingCount, averageRatings.getOrDefault(listingId, 0.0))));
        return Map.copyOf(summaries);
    }
}
