package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** The baseline migration is applied once and is idempotent (E11). */
class FlywayBaselineTest extends PostgresIntegrationTest {

    private static final String COUNT_SQL =
        "select count(*) from flyway_schema_history where version = '001' and success";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void baselineAppliedOnceAndMigrateIsNoOp() {
        assertThat(jdbc.queryForObject(COUNT_SQL, Integer.class)).isEqualTo(1);
        int executed = Flyway.configure().dataSource(dataSource).load().migrate().migrationsExecuted;
        assertThat(executed).isZero();
        assertThat(jdbc.queryForObject(COUNT_SQL, Integer.class)).isEqualTo(1);
    }
}
