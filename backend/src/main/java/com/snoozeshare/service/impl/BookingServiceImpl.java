package com.snoozeshare.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.enums.WalletTransactionType;
import com.snoozeshare.domain.model.AvailabilityBlock;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.Review;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.domain.policy.DisputeWindowPolicy;
import com.snoozeshare.domain.policy.StayDatePolicy;
import com.snoozeshare.domain.settlement.SettlementCalculator;
import com.snoozeshare.domain.statemachine.BookingStateMachine;
import com.snoozeshare.infra.events.BookingCancelledEvent;
import com.snoozeshare.infra.events.BookingConfirmedEvent;
import com.snoozeshare.repository.AvailabilityBlockRepository;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.ReviewRepository;
import com.snoozeshare.repository.TicketRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.repository.WalletRepository;
import com.snoozeshare.service.BookingService;
import com.snoozeshare.service.CancellationRefund;
import com.snoozeshare.service.HostBookingRow;
import com.snoozeshare.service.Money;
import com.snoozeshare.service.TransactionService;
import com.snoozeshare.service.TripFilter;

@Service

public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookings;
    private final PropertyRepository properties;
    private final AvailabilityBlockRepository blocks;
    private final WalletRepository wallets;
    private final LedgerWriter ledger;
    private final UserRepository users;
    private final ReviewRepository reviews;
    private final TransactionService transactionService;
    private final TicketRepository tickets;
    private final ApplicationEventPublisher publisher;
    private final DisputeWindowPolicy disputeWindow;
    private final StayDatePolicy stayDates;
    private final Clock clock;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public BookingServiceImpl(BookingRepository bookings,
                              PropertyRepository properties,
                              AvailabilityBlockRepository blocks,
                              WalletRepository wallets,
                              LedgerWriter ledger,
                              UserRepository users,
                              ReviewRepository reviews,
                              TransactionService transactionService,
                              TicketRepository tickets,
                              ApplicationEventPublisher publisher,
                              Clock clock) {
        this.bookings = bookings;
        this.properties = properties;
        this.blocks = blocks;
        this.wallets = wallets;
        this.ledger = ledger;
        this.users = users;
        this.reviews = reviews;
        this.transactionService = transactionService;
        this.tickets = tickets;
        this.publisher = publisher;
        this.clock = clock;
        this.disputeWindow = new DisputeWindowPolicy(clock);
        this.stayDates = new StayDatePolicy(clock);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Booking submitRequest(UUID guestId, UUID propertyId,
                                 LocalDate start, LocalDate end) {
        stayDates.requireValidStay(start, end);
        User guest = users.findById(guestId)
                .orElseThrow(() -> new IllegalArgumentException("Guest does not exist"));
        if (guest.getRole() != Role.GUEST) {
            throw new IllegalArgumentException("Only guests can submit booking requests");
        }
        if (guest.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }
        Property property = properties.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property does not exist"));
        if (property.getStatus() != ListingStatus.ACTIVE) {
            throw new IllegalArgumentException("Listing is not active");
        }
        if (!bookings.findOverlapping(propertyId, start, end).isEmpty()
                || !blocks.findOverlapping(propertyId, start, end).isEmpty()) {
            throw new IllegalArgumentException("Dates are not available for this property");
        }
        long nights = ChronoUnit.DAYS.between(start, end);
        BigDecimal nightlyRate = property.getBaseNightlyRate();
        BigDecimal totalAmount = nightlyRate.multiply(BigDecimal.valueOf(nights));
        Instant now = Instant.now();
        UUID bookingId = UUID.randomUUID();
        Booking newBooking = new Booking(bookingId, propertyId, guestId,
                start, end, BookingStatus.PENDING, nightlyRate, totalAmount,
                now, null, null, null);
        bookings.save(newBooking);
        blocks.save(new AvailabilityBlock(UUID.randomUUID(), propertyId,
                start, end, "BOOKING", bookingId, null));
        Wallet wallet = wallets.findByUserId(guestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Guest wallet does not exist"));
        ledger.post(wallet.getId(), WalletTransactionType.ESCROW_HOLD,
                totalAmount.negate(), guestId, bookingId, null, null, now);
        return newBooking;
    }

    @Override
    public List<Booking> tripsFor(UUID guestId, TripFilter filter) {
        List<Booking> all = bookings.findByGuestId(guestId);
        if (filter == null || filter.status() == null) {
            return all;
        }
        return all.stream()
                .filter(b -> b.getStatus() == filter.status())
                .toList();
    }

    @Override
    public List<Booking> pendingRequestsFor(UUID hostId) {
        return bookings.findByHostPending(hostId);
    }

    @Override
    public List<HostBookingRow> pendingRequestRowsFor(UUID hostId) {
        return toHostRows(bookings.findByHostPending(hostId));
    }

    @Override
    public List<HostBookingRow> historyRowsFor(UUID hostId) {
        return toHostRows(bookings.findByHost(hostId));
    }

    private List<HostBookingRow> toHostRows(List<Booking> source) {
        return source.stream().map(booking -> {
            Property property = properties.findById(booking.getListingId())
                    .orElseThrow(() -> new IllegalArgumentException("Property does not exist"));
            String guestName = users.findById(booking.getGuestId())
                    .orElseThrow(() -> new IllegalArgumentException("Guest does not exist"))
                    .getDisplayName();
            List<Review> guestReviews = reviews.findByGuestId(booking.getGuestId());
            OptionalDouble rating = guestReviews.isEmpty() ? OptionalDouble.empty()
                    : OptionalDouble.of(guestReviews.stream()
                            .mapToInt(Review::getRating).average().orElse(0));
            long nights = ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate());
            BigDecimal net = SettlementCalculator.hostNet(booking.getTotalAmount());
            return new HostBookingRow(booking, guestName, property.getTitle(), nights,
                    booking.getTotalAmount(), net, rating);
        }).toList();
    }

    @Override
    public Booking decide(UUID bookingId, boolean approve, UUID hostId) {
        return decide(bookingId, approve, hostId, null);
    }

    @Override
    @Transactional
    public Booking decide(UUID bookingId, boolean approve, UUID hostId,
                          String hostDecisionMessage) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
        Property property = properties.findById(booking.getListingId())
                .orElseThrow(() -> new IllegalArgumentException("Property does not exist"));
        if (!property.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Only the property host can decide");
        }
        BookingStatus target = approve ? BookingStatus.CONFIRMED : BookingStatus.REJECTED;
        if (!BookingStateMachine.canTransition(booking.getStatus(), target, Role.HOST)) {
            throw new IllegalStateException("Cannot transition from " + booking.getStatus()
                    + " to " + target);
        }
        Instant now = Instant.now();
        String message = approve || hostDecisionMessage == null
                ? null : hostDecisionMessage.trim();
        if (message != null && message.isEmpty()) {
            message = null;
        }
        booking.setStatus(target);
        booking.setDecidedAt(now);
        booking.setHostDecisionMessage(message);
        bookings.save(booking);
        if (!approve) {
            blocks.deleteByBookingId(bookingId);
            Wallet wallet = wallets.findByUserId(booking.getGuestId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Guest wallet does not exist"));
            ledger.post(wallet.getId(), WalletTransactionType.ESCROW_REFUND,
                    booking.getTotalAmount(), hostId, bookingId, null, null, now);
        }
        if (approve) {
            publisher.publishEvent(new BookingConfirmedEvent(bookingId, hostId,
                    Instant.now()));
        } else {
            publisher.publishEvent(new BookingCancelledEvent(bookingId, hostId,
                    Instant.now()));
        }
        return booking;
    }

    @Override
    @Transactional
    public Booking cancel(UUID bookingId, UUID actingGuestId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
        if (!booking.getGuestId().equals(actingGuestId)) {
            throw new IllegalArgumentException("Only the booking guest can cancel");
        }
        if (!BookingStateMachine.canTransition(booking.getStatus(),
                BookingStatus.CANCELLED_BY_GUEST, Role.GUEST)) {
            throw new IllegalStateException("Cannot cancel booking in status "
                    + booking.getStatus());
        }
        BigDecimal refundAmount = calculateRefundAmount(booking);
        Instant now = Instant.now();
        booking.setStatus(BookingStatus.CANCELLED_BY_GUEST);
        booking.setDecidedAt(now);
        bookings.save(booking);
        blocks.deleteByBookingId(bookingId);
        Wallet wallet = wallets.findByUserId(actingGuestId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Guest wallet does not exist"));
        ledger.post(wallet.getId(), WalletTransactionType.ESCROW_REFUND, refundAmount,
                actingGuestId, bookingId, null, null, now);
        publisher.publishEvent(new BookingCancelledEvent(bookingId, actingGuestId,
                Instant.now()));
        return booking;
    }

    @Override
    public CancellationRefund previewCancellationRefund(UUID bookingId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
        BigDecimal amount = calculateRefundAmount(booking);
        int percent = amount.compareTo(booking.getTotalAmount()) == 0 ? 100 : 50;
        return new CancellationRefund(amount, percent);
    }

    BigDecimal calculateRefundAmount(Booking booking) {
        if (booking.getStatus() == BookingStatus.PENDING) {
            return booking.getTotalAmount();
        }
        long hoursUntilCheckIn = ChronoUnit.HOURS.between(
                clock.instant(), booking.getStartDate().atStartOfDay()
                        .atZone(ZoneId.systemDefault()).toInstant());
        if (hoursUntilCheckIn > 48) {
            return booking.getTotalAmount();
        }
        return booking.getTotalAmount().divide(BigDecimal.valueOf(2))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public Booking complete(UUID bookingId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            return booking;
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be completed");
        }
        if (!disputeWindow.canAutoComplete(booking)) {
            throw new IllegalStateException("Booking is not eligible for completion");
        }
        boolean blocked = tickets.findByBookingId(bookingId).stream()
                .anyMatch(ticket -> ticket.getStatus() == TicketStatus.OPEN
                        || ticket.getStatus() == TicketStatus.IN_REVIEW);
        if (blocked) {
            throw new IllegalStateException("Open dispute blocks booking completion");
        }
        transactionService.settleBookingCompletion(bookingId);
        return bookings.findById(bookingId).orElseThrow();
    }

    @Override
    @Transactional
    public int completeEligibleBookings() {
        int completed = 0;
        for (Booking booking : bookings.findConfirmedEndingOnOrBefore(
                disputeWindow.completionCutoff())) {
            try {
                complete(booking.getId());
                completed++;
            } catch (IllegalStateException ignored) {
                // Open disputes and transiently ineligible rows remain held.
            }
        }
        return completed;
    }

    @Override
    public Money previewHostEarnings(UUID bookingId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
        return new Money(SettlementCalculator.hostNet(booking.getTotalAmount()), "SGD");
    }
}
