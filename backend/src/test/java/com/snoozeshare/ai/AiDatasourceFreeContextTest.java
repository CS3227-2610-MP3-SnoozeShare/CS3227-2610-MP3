package com.snoozeshare.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;

import com.snoozeshare.ai.audit.AiAuditWriter;
import com.snoozeshare.ai.config.LlmProperties;

/** E39: a context without a datasource starts, binds the settings, and has no gateway bean. */
@SpringBootTest(webEnvironment = WebEnvironment.NONE, properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
    "management.health.db.enabled=false"})
class AiDatasourceFreeContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void startsWithoutAGatewayOrWriter() {
        assertThat(context.getBeansOfType(LlmGateway.class)).isEmpty();
        assertThat(context.getBeansOfType(AiAuditWriter.class)).isEmpty();
        assertThat(context.getBean(LlmProperties.class).enabled()).isFalse();
        assertThat(context.getBean(AiFeatureRegistry.class).all()).isEmpty();
    }
}
