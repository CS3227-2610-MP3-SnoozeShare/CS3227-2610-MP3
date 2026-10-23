package com.snoozeshare.api;

/** The request reached an actor-aware handler without an authenticated actor id. */
public class MissingActorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MissingActorException() {
        super("Authenticated actor is missing");
    }
}
