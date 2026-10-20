package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.core.env.Environment;

/** Without PORT the server listens on 8080 (E20). */
@SpringBootTest(webEnvironment = WebEnvironment.NONE, properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
    "management.health.db.enabled=false"})
class ServerPortDefaultTest {

    @Autowired
    private Environment env;

    @Test
    void portDefaultsTo8080() {
        assertThat(env.getProperty("server.port")).isEqualTo("8080");
    }
}
