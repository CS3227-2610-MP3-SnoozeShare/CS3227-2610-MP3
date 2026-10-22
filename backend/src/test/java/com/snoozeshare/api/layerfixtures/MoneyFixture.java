package com.snoozeshare.api.layerfixtures;

import com.snoozeshare.service.impl.LedgerWriter;

/** Deliberately violates the money-service boundary for the ArchUnit test. */
final class MoneyFixture {
    private LedgerWriter ledgerWriter;
}
