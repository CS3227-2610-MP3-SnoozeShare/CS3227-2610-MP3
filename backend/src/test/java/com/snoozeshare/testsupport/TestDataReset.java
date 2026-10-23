package com.snoozeshare.testsupport;

import org.springframework.jdbc.core.JdbcTemplate;

/** Restores the shared integration-test database to its migrated seed state. */
public final class TestDataReset {

    private static final String SYSTEM_ID = "a0000000-0000-0000-0000-0000000000ff";

    private TestDataReset() { }

    public static void reset(JdbcTemplate jdbc) {
        jdbc.update("delete from ticket_assistants");
        jdbc.update("delete from message_reads");
        jdbc.update("delete from booking_message_reads");
        jdbc.update("delete from messages");
        jdbc.update("delete from booking_messages");
        jdbc.update("delete from audit_log");
        jdbc.update("delete from tickets");
        jdbc.update("delete from reviews");
        jdbc.update("delete from availability_blocks");
        jdbc.update("delete from bookings");
        jdbc.update("delete from ticket_categories");
        jdbc.update("delete from properties");
        jdbc.update("delete from wallets where user_id <> ?::uuid", SYSTEM_ID);
        jdbc.update("delete from users where id <> ?::uuid", SYSTEM_ID);
        jdbc.update("update wallets set balance = 0.00 where user_id = ?::uuid", SYSTEM_ID);
    }
}
