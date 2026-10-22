package com.snoozeshare.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.snoozeshare.domain.enums.ThreadChannel;

/** JDBC operations for ticket message read tracking (composite-key table). */
@Repository

public class JdbcMessageReadRepository {

    private final JdbcTemplate jdbc;

    public JdbcMessageReadRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void markRead(UUID ticketId, ThreadChannel channel,
                         UUID userId, UUID lastReadMessageId) {
        jdbc.update(
                "INSERT INTO message_reads "
                + "(ticket_id, channel, user_id, last_read_message_id) "
                + "VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (ticket_id, channel, user_id) "
                + "DO UPDATE SET last_read_message_id = EXCLUDED"
                + ".last_read_message_id",
                ticketId, channel.name(), userId, lastReadMessageId);
    }

    public int countUnread(UUID ticketId, ThreadChannel channel,
                           UUID userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM messages m "
                + "WHERE m.ticket_id = ? AND m.channel = ? "
                + "AND m.author_id <> ? "
                + "AND m.sent_at > COALESCE("
                + "(SELECT lm.sent_at FROM message_reads mr "
                + "JOIN messages lm ON lm.id = mr.last_read_message_id "
                + "WHERE mr.ticket_id = ? AND mr.channel = ? "
                + "AND mr.user_id = ?), "
                + "CAST('1970-01-01' AS TIMESTAMPTZ))",
                Integer.class,
                ticketId, channel.name(), userId,
                ticketId, channel.name(), userId);
        return count != null ? count : 0;
    }
}
