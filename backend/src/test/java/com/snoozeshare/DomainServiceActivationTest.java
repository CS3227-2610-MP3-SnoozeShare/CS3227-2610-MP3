package com.snoozeshare;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/** Confirms persistence-backed services activate from the explicit default (E24). */
class DomainServiceActivationTest extends PostgresIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void persistenceBeansAreEnabledByDefault() {
        PersistenceBeanAssertions.assertPresent(context);
    }
}
