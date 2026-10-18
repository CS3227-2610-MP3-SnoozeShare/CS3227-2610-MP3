package com.snoozeshare.service;

import static com.snoozeshare.service.BookingServiceTest.migratedConnection;
import static com.snoozeshare.service.BookingServiceTest.seedContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.infra.events.InProcessEventBus;
import com.snoozeshare.infra.events.events.WalletTransactionRecordedEvent;
import com.snoozeshare.repository.jdbc.JdbcLedgerRepository;

/** The ledger writer queues the wallet event itself, so no caller can forget it (MP2 feedback B3 / D1). */
class WalletEventPublishingTest {

    @Test
    void cancellingABookingPublishesTheRefundEventOnce() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("500.00"));
            var bus = new InProcessEventBus();
            BookingService service = BookingServiceTest.createService(connection, bus);
            Booking booking = service.submitRequest(ctx.guestId(), ctx.propertyId(),
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(13));
            List<WalletTransactionRecordedEvent> events = new ArrayList<>();
            bus.subscribe(WalletTransactionRecordedEvent.class, events::add);

            service.cancel(booking.bookingId(), ctx.guestId());

            var refund = new JdbcLedgerRepository(connection).entriesForBooking(booking.bookingId()).stream()
                    .filter(entry -> entry.type() == com.snoozeshare.domain.enums.WalletTransactionType.ESCROW_REFUND)
                    .findFirst().orElseThrow();
            assertEquals(1, events.size());
            assertEquals(refund.transactionId(), events.get(0).transactionId());
            assertEquals(refund.walletId(), events.get(0).walletId());
        }
    }

    @Test
    void aFailedTransactionPublishesNoWalletEvent() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("100.00"));
            var bus = new InProcessEventBus();
            List<WalletTransactionRecordedEvent> events = new ArrayList<>();
            bus.subscribe(WalletTransactionRecordedEvent.class, events::add);
            BookingService service = BookingServiceTest.createService(connection, bus);

            assertThrows(IllegalArgumentException.class, () -> service.submitRequest(ctx.guestId(), ctx.propertyId(),
                    LocalDate.now().plusDays(10), LocalDate.now().plusDays(13)));

            assertEquals(0, events.size());
        }
    }

    @Test
    void everyServiceLedgerWritePublishesExactlyOneEventPerRow() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("1000.00"));
            var bus = new InProcessEventBus();
            List<WalletTransactionRecordedEvent> events = new ArrayList<>();
            bus.subscribe(WalletTransactionRecordedEvent.class, events::add);
            var tx = com.snoozeshare.testsupport.Tx.of(connection, bus);
            var wallets = new com.snoozeshare.repository.jdbc.JdbcWalletRepository(connection);
            var ledgerRepo = new JdbcLedgerRepository(connection);
            WalletService walletService = new com.snoozeshare.service.impl.WalletServiceImpl(tx.manager(), wallets,
                    tx.ledger(), ledgerRepo);

            walletService.topUp(ctx.guestId(), new BigDecimal("25.00"));
            walletService.withdraw(ctx.guestId(), new BigDecimal("5.00"));

            var rows = ledgerRepo.entriesForWallet(wallets.findByUserId(ctx.guestId()).orElseThrow().walletId());
            assertEquals(rows.size(), events.size());
            assertEquals(2, events.size());
        }
    }
}
