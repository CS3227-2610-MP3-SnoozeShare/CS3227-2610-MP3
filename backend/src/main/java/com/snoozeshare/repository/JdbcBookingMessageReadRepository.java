package com.snoozeshare.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.snoozeshare.config.ConditionalOnDatabase;

/** JDBC operations for booking message read tracking (composite-key table). */
@Repository
@ConditionalOnDatabase
public class JdbcBookingMessageReadRepository {

    private final JdbcTemplate jdbc;

    public JdbcBookingMessageReadRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void markRead(UUID bookingId, UUID userId,
                         UUID lastReadMessageId) {
        jdbc.update(
                "INSERT INTO booking_message_reads "
                + "(booking_id, user_id, last_read_message_id) "
                + "VALUES (?, ?, ?) "
                + "ON CONFLICT (booking_id, user_id) "
                + "DO UPDATE SET last_read_message_id = EXCLUDED"
                + ".last_read_message_id",
                bookingId, userId, lastReadMessageId);
    }

    public int countUnread(UUID bookingId, UUID userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM booking_messages m "
                + "WHERE m.booking_id = ? "
                + "AND m.author_id <> ? "
                + "AND m.sent_at > COALESCE("
                + "(SELECT bmr.last_read_message_id "
                + "FROM booking_message_reads bmr "
                + "JOIN booking_messages lm "
                + "ON lm.id = bmr.last_read_message_id "
                + "WHERE bmr.booking_id = ? "
                + "AND bmr.user_id = ?), "
                + "CAST('1970-01-01' AS TIMESTAMPTZ))",
                Integer.class,
                bookingId, userId, bookingId, userId);
        return count != null ? count : 0;
    }
}
