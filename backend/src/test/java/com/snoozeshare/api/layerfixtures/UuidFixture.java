package com.snoozeshare.api.layerfixtures;

import java.util.UUID;

/** Deliberately leaves a UUID parameter source unspecified for the ArchUnit test. */
final class UuidFixture {
    public UUID load(UUID id) {
        return id;
    }
}
