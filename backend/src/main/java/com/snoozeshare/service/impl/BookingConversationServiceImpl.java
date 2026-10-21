package com.snoozeshare.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.BookingMessage;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.validation.DomainValidation;
import com.snoozeshare.infra.events.BookingMessagePostedEvent;
import com.snoozeshare.repository.BookingMessageRepository;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.JdbcBookingMessageReadRepository;
import com.snoozeshare.repository.OffsetLimitPageable;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.BookingConversationService;
import com.snoozeshare.service.BookingConversationSummary;

@Service
@ConditionalOnProperty(name = "snoozeshare.persistence.enabled", havingValue = "true", matchIfMissing = false)
public class BookingConversationServiceImpl implements BookingConversationService {

    static final int CHAT_DAYS_AFTER_CHECKOUT = 7;

    private final BookingMessageRepository messages;
    private final JdbcBookingMessageReadRepository messageReads;
    private final BookingRepository bookings;
    private final PropertyRepository properties;
    private final UserRepository users;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public BookingConversationServiceImpl(BookingMessageRepository messages,
                                          JdbcBookingMessageReadRepository messageReads,
                                          BookingRepository bookings,
                                          PropertyRepository properties,
                                          UserRepository users,
                                          ApplicationEventPublisher publisher,
                                          Clock clock) {
        this.messages = messages;
        this.messageReads = messageReads;
        this.bookings = bookings;
        this.properties = properties;
        this.users = users;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public List<BookingMessage> thread(UUID bookingId, UUID viewerId, Role viewerRole) {
        requireParty(booking(bookingId), viewerId, viewerRole);
        return messages.findThread(bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingMessage> thread(UUID bookingId, UUID viewerId, Role viewerRole,
                                       int page, int size) {
        requireParty(booking(bookingId), viewerId, viewerRole);
        return messages.findThreadPage(bookingId, new OffsetLimitPageable(page, size));
    }

    @Override
    @Transactional
    public BookingMessage post(UUID bookingId, UUID authorId, Role authorRole, String body) {
        DomainValidation.requireText(body, "body");
        Booking booking = booking(bookingId);
        requireParty(booking, authorId, authorRole);
        if (!isOpen(booking)) {
            throw new IllegalStateException("Chat is closed for this booking");
        }
        Instant now = clock.instant();
        BookingMessage saved = messages.save(new BookingMessage(UUID.randomUUID(), bookingId,
                authorId, authorRole, body.trim(), now));
        publishAfterCommit(new BookingMessagePostedEvent(saved.getId(), bookingId, now));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingConversationSummary> conversationsFor(UUID userId, Role role,
                                                               int page, int size) {
        if (role != Role.HOST) {
            throw new IllegalArgumentException("Host inbox requires a Host role");
        }
        LocalDate openCutoff = LocalDate.now(clock).minusDays(CHAT_DAYS_AFTER_CHECKOUT);
        return bookings.findConversationBookingsByHost(userId, openCutoff,
                new OffsetLimitPageable(page, size)).stream()
                .map(booking -> summarize(booking, userId).summary())
                .toList();
    }

    @Override
    public List<BookingConversationSummary> conversationsFor(UUID userId, Role role) {
        return partyBookings(userId, role).stream()
                .map(booking -> summarize(booking, userId))
                .filter(ranked -> ranked.summary().open()
                        || ranked.summary().lastMessage() != null)
                .sorted(Comparator.comparing(Ranked::lastActivity).reversed())
                .map(Ranked::summary)
                .toList();
    }

    @Override
    public int unreadCount(UUID userId, Role role) {
        return partyBookings(userId, role).stream()
                .mapToInt(booking -> messageReads.countUnread(booking.getId(), userId))
                .sum();
    }

    @Override
    @Transactional
    public void markRead(UUID bookingId, UUID userId, Role role) {
        requireParty(booking(bookingId), userId, role);
        messages.findLast(bookingId)
                .ifPresent(last -> messageReads.markRead(bookingId, userId, last.getId()));
    }

    private void publishAfterCommit(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publisher.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.publishEvent(event);
            }
        });
    }

    private List<Booking> partyBookings(UUID userId, Role role) {
        return switch (role) {
            case GUEST -> bookings.findByGuestId(userId);
            case HOST -> bookings.findByHost(userId);
            default -> throw new IllegalArgumentException(
                    "Only guests and hosts have booking chats");
        };
    }

    private Ranked summarize(Booking booking, UUID userId) {
        Property property = property(booking);
        Optional<BookingMessage> last = messages.findLast(booking.getId());
        UUID counterpartId = property.getHostId();
        if (booking.getGuestId().equals(userId)) {
            counterpartId = property.getHostId();
        } else {
            counterpartId = booking.getGuestId();
        }
        String counterpart = users.findById(counterpartId)
                .map(User::getDisplayName).orElse("Unknown user");
        BookingConversationSummary summary = new BookingConversationSummary(booking.getId(),
                property.getTitle(), booking.getStartDate(), booking.getEndDate(), counterpart,
                last.orElse(null), messageReads.countUnread(booking.getId(), userId),
                isOpen(booking));
        Instant activity = last.map(BookingMessage::getSentAt)
                .orElse(booking.getDecidedAt() != null
                        ? booking.getDecidedAt() : booking.getCreatedAt());
        return new Ranked(summary, activity);
    }

    private void requireParty(Booking booking, UUID userId, Role role) {
        User user = users.findById(userId)
                .filter(found -> found.getRole() == role)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user for this role"));
        boolean party = switch (role) {
            case GUEST -> booking.getGuestId().equals(user.getId());
            case HOST -> property(booking).getHostId().equals(user.getId());
            default -> false;
        };
        if (!party) {
            throw new IllegalArgumentException("Not a participant in this booking chat");
        }
    }

    private boolean isOpen(Booking booking) {
        return booking.getStatus() == BookingStatus.CONFIRMED
                && !LocalDate.now(clock).isAfter(
                        booking.getEndDate().plusDays(CHAT_DAYS_AFTER_CHECKOUT));
    }

    private Booking booking(UUID bookingId) {
        return bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
    }

    private Property property(Booking booking) {
        return properties.findById(booking.getListingId())
                .orElseThrow(() -> new IllegalArgumentException("Property does not exist"));
    }

    private record Ranked(BookingConversationSummary summary, Instant lastActivity) {
    }
}
