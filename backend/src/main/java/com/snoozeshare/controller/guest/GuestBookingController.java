package com.snoozeshare.controller.guest;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.request.CreateBookingRequest;
import com.snoozeshare.api.response.BookingResponse;
import com.snoozeshare.api.response.CancellationRefundResponse;
import com.snoozeshare.api.response.GuestTripResponse;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.Review;
import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.policy.DisputeWindowPolicy;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.ReviewRepository;
import com.snoozeshare.repository.TicketRepository;
import com.snoozeshare.service.BookingService;
import com.snoozeshare.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/guest/bookings")
@ConditionalOnDatabase
public class GuestBookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final PropertyRepository properties;
    private final UserService userService;
    private final TicketRepository tickets;
    private final ReviewRepository reviews;
    private final DisputeWindowPolicy disputeWindow;
    private final Clock clock;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public GuestBookingController(BookingService bookingService,
                                  BookingRepository bookingRepository,
                                  PropertyRepository properties,
                                  UserService userService,
                                  TicketRepository tickets,
                                  ReviewRepository reviews,
                                  Clock clock) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        this.properties = properties;
        this.userService = userService;
        this.tickets = tickets;
        this.reviews = reviews;
        this.clock = clock;
        this.disputeWindow = new DisputeWindowPolicy(clock);
    }

    @PostMapping
    BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest req,
                                  HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        Booking booking = bookingService.submitRequest(
                guestId, req.propertyId(), req.startDate(), req.endDate());
        return BookingResponse.from(booking);
    }

    @GetMapping
    List<GuestTripResponse> listTrips(HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        List<Booking> allBookings = bookingService.tripsFor(guestId, null);
        LocalDate today = LocalDate.now(clock);

        Set<UUID> guestTicketBookingIds = tickets
                .findByRaisedByUserIdOrderByCreatedAtDesc(guestId).stream()
                .map(Ticket::getBookingId)
                .collect(Collectors.toSet());

        Set<UUID> reviewedBookingIds = reviews.findByGuestId(guestId).stream()
                .map(Review::getBookingId)
                .collect(Collectors.toSet());

        return allBookings.stream()
                .map(b -> toTripResponse(b, today, guestTicketBookingIds,
                        reviewedBookingIds))
                .toList();
    }

    @GetMapping("/{id}/cancel-preview")
    CancellationRefundResponse cancelPreview(@PathVariable UUID id,
                                             HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        Booking booking = findBookingOrThrow(id);
        requireOwnership(booking, guestId);
        return CancellationRefundResponse.from(
                bookingService.previewCancellationRefund(id));
    }

    @PostMapping("/{id}/cancel")
    BookingResponse cancelBooking(@PathVariable UUID id,
                                  HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        Booking booking = findBookingOrThrow(id);
        requireOwnership(booking, guestId);
        Booking cancelled = bookingService.cancel(id, guestId);
        return BookingResponse.from(cancelled);
    }

    private GuestTripResponse toTripResponse(Booking b, LocalDate today,
                                             Set<UUID> guestTicketBookingIds,
                                             Set<UUID> reviewedBookingIds) {
        Property property = properties.findById(b.getListingId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Property does not exist"));
        User host = userService.findById(property.getHostId());

        boolean canCancel = b.getStatus() == BookingStatus.PENDING
                || (b.getStatus() == BookingStatus.CONFIRMED
                    && b.getStartDate().isAfter(today));

        boolean canFileDispute = !guestTicketBookingIds.contains(b.getId())
                && disputeWindow.canFileTicket(b);

        boolean hasReview = reviewedBookingIds.contains(b.getId());

        return new GuestTripResponse(
                BookingResponse.from(b),
                property.getTitle(),
                host.getDisplayName(),
                canCancel,
                canFileDispute,
                hasReview);
    }

    private Booking findBookingOrThrow(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking does not exist"));
    }

    private void requireOwnership(Booking booking, UUID guestId) {
        if (!booking.getGuestId().equals(guestId)) {
            throw new ForbiddenException(
                    "You do not have access to this booking");
        }
    }
}
