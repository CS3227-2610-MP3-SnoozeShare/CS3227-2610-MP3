package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.util.UUID;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.service.AuditService;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;

/** E52: migrate a populated V005 schema without cleaning the shared integration schema. */
class TicketAssistantMigrationTest extends AgentApiIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void widensTheExistingAuthorConstraintWithoutLosingMessages() throws Exception {
        var scenario = scenarios.openDispute(RemedyType.FULL_REFUND);
        String schema = "assistant_migration_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("create schema " + schema);
        try (Connection connection = dataSource.getConnection()) {
            connection.setSchema(schema);
            var isolated = new SingleConnectionDataSource(connection, true);
            Flyway.configure().dataSource(isolated).schemas(schema).defaultSchema(schema)
                    .target("005").load().migrate();
            JdbcTemplate old = new JdbcTemplate(isolated);
            String constraint = old.queryForObject("select conname from pg_constraint "
                    + "where conrelid = 'messages'::regclass and contype = 'c' "
                    + "and pg_get_constraintdef(oid) like '%author_role%'", String.class);
            assertThat(constraint).isEqualTo("messages_author_role_check");
            old.update("insert into users select * from public.users where id in (?, ?, ?)",
                    scenario.agent().getId(), scenario.guest().getId(), scenario.host().getId());
            old.update("insert into properties select * from public.properties where id = ?",
                    scenario.property().getId());
            old.update("insert into bookings select * from public.bookings where id = ?", scenario.booking().getId());
            old.update("insert into tickets select * from public.tickets where id = ?", scenario.ticket().getId());
            UUID original = UUID.randomUUID();
            old.update("insert into messages values (?, ?, 'GUEST', ?, 'GUEST', 'Original', now())",
                    original, scenario.ticket().getId(), scenario.guest().getId());

            assertThat(Flyway.configure().dataSource(isolated).schemas(schema).defaultSchema(schema)
                    .load().migrate().migrationsExecuted).isEqualTo(1);
            assertThat(old.queryForObject("select body from messages where id = ?", String.class, original))
                    .isEqualTo("Original");
            old.update("insert into messages values (?, ?, 'GUEST', ?, 'SYSTEM', 'Assistant', now())",
                    UUID.randomUUID(), scenario.ticket().getId(), AuditService.SYSTEM_ACTOR_ID);
            assertThatThrownBy(() -> old.update(
                    "insert into messages values (?, ?, 'GUEST', ?, 'BOGUS', 'Invalid', now())",
                    UUID.randomUUID(), scenario.ticket().getId(), AuditService.SYSTEM_ACTOR_ID))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThat(old.queryForObject("select count(*) from ticket_assistants", Integer.class)).isZero();
            connection.setSchema("public");
        } finally {
            jdbc.execute("drop schema " + schema + " cascade");
        }
    }
}
