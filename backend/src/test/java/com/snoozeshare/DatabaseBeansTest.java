package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.snoozeshare.repository.LedgerRepository;
import com.snoozeshare.service.AuditService;
import com.snoozeshare.service.WalletService;

/**
 * The database-backed services and repositories exist in the running application. They carried
 * {@code @ConditionalOnBean(PropertyRepository.class)}, which never matched at scan time, so they were silently
 * absent until {@code @ConditionalOnDatabase} replaced it.
 */
class DatabaseBeansTest extends PostgresIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void servicesAndJdbcRepositoriesAreRegistered() {
        assertThat(context.getBeansOfType(AuditService.class)).hasSize(1);
        assertThat(context.getBeansOfType(WalletService.class)).hasSize(1);
        assertThat(context.getBeansOfType(LedgerRepository.class)).hasSize(1);
    }
}
