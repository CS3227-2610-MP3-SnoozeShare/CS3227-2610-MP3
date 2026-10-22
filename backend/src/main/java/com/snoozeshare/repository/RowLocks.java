package com.snoozeshare.repository;

import org.springframework.stereotype.Component;

import com.snoozeshare.config.ConditionalOnDatabase;

import jakarta.persistence.EntityManager;

/** Applies the PostgreSQL lock wait bound inside the caller's transaction. */
@Component
@ConditionalOnDatabase
public class RowLocks {

    private final EntityManager entityManager;

    public RowLocks(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void timeout() {
        entityManager.createNativeQuery("SET LOCAL lock_timeout = '5000ms'").executeUpdate();
    }
}
