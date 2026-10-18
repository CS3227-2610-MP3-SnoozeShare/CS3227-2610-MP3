package com.snoozeshare.service;

import static com.snoozeshare.service.BookingServiceTest.createService;
import static com.snoozeshare.service.BookingServiceTest.migratedConnection;
import static com.snoozeshare.service.BookingServiceTest.seedContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.repository.jdbc.JdbcBookingRepository;

/** Guards the joined boundary TA1 found: completion must never pay the host while filing is still open. */
class DisputeWindowBoundaryTest {

    private static Booking confirmedEndedDaysAgo(Connection connection, BookingTestIds ids, BookingService service,
                                                 int daysAgo) {
        Booking booking = service.submitRequest(ids.guestId(), ids.propertyId(),
                LocalDate.now().minusDays(daysAgo + 3L), LocalDate.now().minusDays(daysAgo));
        return new JdbcBookingRepository(connection).save(new Booking(booking.bookingId(), booking.listingId(),
                booking.guestId(), booking.startDate(), booking.endDate(), BookingStatus.CONFIRMED,
                booking.nightlyRateSnapshot(), booking.totalAmount(), booking.createdAt(), Instant.now(), null));
    }

    @Test
    void sweepDoesNotCompleteABookingWhoseFilingWindowIsStillOpen() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("1000.00"));
            BookingService service = createService(connection);
            Booking onDaySeven = confirmedEndedDaysAgo(connection, new BookingTestIds(ctx.guestId(), ctx.propertyId()),
                    service, 7);

            assertEquals(0, service.completeEligibleBookings());
            assertThrows(IllegalStateException.class, () -> service.complete(onDaySeven.bookingId()));
            assertEquals(BookingStatus.CONFIRMED,
                    new JdbcBookingRepository(connection).findById(onDaySeven.bookingId()).orElseThrow().status());
        }
    }

    @Test
    void sweepCompletesOnceTheWindowHasClosed() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("1000.00"));
            BookingService service = createService(connection);
            Booking onDayEight = confirmedEndedDaysAgo(connection, new BookingTestIds(ctx.guestId(), ctx.propertyId()),
                    service, 8);

            assertEquals(1, service.completeEligibleBookings());
            assertEquals(BookingStatus.COMPLETED,
                    new JdbcBookingRepository(connection).findById(onDayEight.bookingId()).orElseThrow().status());
        }
    }

    private record BookingTestIds(java.util.UUID guestId, java.util.UUID propertyId) {
    }
}
