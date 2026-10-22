package com.snoozeshare.ai;

/** Who spoke a {@link Turn}. */
public enum TurnRole {
    /** A person; always wrapped as untrusted input. */
    USER,
    /** An earlier model reply; callers take it from server-stored history only, never from client input. */
    ASSISTANT
}
