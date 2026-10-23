package com.snoozeshare.api.layerfixtures;

import com.snoozeshare.repository.TicketRepository;

/** Deliberately violates the persistence boundary for the ArchUnit test. */
final class RepositoryFixture {
    private TicketRepository repository;
}
