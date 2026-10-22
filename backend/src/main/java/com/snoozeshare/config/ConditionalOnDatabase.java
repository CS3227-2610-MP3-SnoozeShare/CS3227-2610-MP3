package com.snoozeshare.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Registers a component only when the application has a database, so contexts that exclude the datasource
 * auto-configuration (the port tests) start without the services and repositories that need one.
 *
 * <p>This replaces the {@code ConditionalOnBean} check on {@code PropertyRepository}. That condition is evaluated while
 * component scanning runs, before Spring Data registers its repositories, so it never matched in the running
 * application and the annotated classes were silently absent.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Conditional(ConditionalOnDatabase.DatabaseConfigured.class)
public @interface ConditionalOnDatabase {

    /** Matches unless {@code spring.autoconfigure.exclude} lists the datasource auto-configuration. */
    class DatabaseConfigured implements Condition {

        private static final String DATASOURCE_AUTOCONFIG = "DataSourceAutoConfiguration";

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String excluded = context.getEnvironment().getProperty("spring.autoconfigure.exclude", "");
            return !excluded.contains(DATASOURCE_AUTOCONFIG);
        }
    }
}
