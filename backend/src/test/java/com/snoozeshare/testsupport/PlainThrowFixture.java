package com.snoozeshare.testsupport;

/** Throws from outside the service and domain packages to exercise fixed messages. */
public final class PlainThrowFixture {

    private PlainThrowFixture() { }

    public static void invalidRequest() {
        throw new IllegalArgumentException("framework text with SQL parameter secret");
    }

    public static void secretFailure() {
        throw new IllegalStateException("secret request body and password");
    }
}
