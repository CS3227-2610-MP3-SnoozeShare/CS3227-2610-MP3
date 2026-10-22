package com.snoozeshare.service;

import org.springframework.dao.DataAccessResourceFailureException;

/** Test-only exceptions with controlled first stack frames for API error mapping. */
public final class ThrowFixture {

    private ThrowFixture() { }

    public static void invalidRequest() {
        throw new IllegalArgumentException("Service validation message");
    }

    public static void conflict() {
        throw new IllegalStateException("Service conflict message");
    }

    public static void repositoryFailure() {
        throw new DataAccessResourceFailureException("database secret SQL SELECT 1");
    }
}
