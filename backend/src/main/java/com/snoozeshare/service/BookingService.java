package com.snoozeshare.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.model.Booking;

public interface BookingService {

    Booking submitRequest(UUID guestId, UUID propertyId, LocalDate start, LocalDate end);

    List<Booking> tripsFor(UUID guestId, TripFilter filter);

    List<Booking> pendingRequestsFor(UUID hostId);

    List<HostBookingRow> pendingRequestRowsFor(UUID hostId);

    List<HostBookingRow> pendingRequestRowsFor(UUID hostId, int page, int size);

    List<HostBookingRow> historyRowsFor(UUID hostId);

    List<HostBookingRow> historyRowsFor(UUID hostId, int page, int size);

    Booking decide(UUID bookingId, boolean approve, UUID hostId);

    Booking decide(UUID bookingId, boolean approve, UUID hostId, String hostDecisionMessage);

    Booking cancel(UUID bookingId, UUID actingGuestId);

    CancellationRefund previewCancellationRefund(UUID bookingId);

    Booking complete(UUID bookingId);

    int completeEligibleBookings();

    Money previewHostEarnings(UUID bookingId);
}
