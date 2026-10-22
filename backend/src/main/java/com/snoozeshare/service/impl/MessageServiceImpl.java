package com.snoozeshare.service.impl;

import java.time.Clock;
import java.time.Instant;
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

import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Message;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.validation.DomainValidation;
import com.snoozeshare.infra.events.MessagePostedEvent;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.JdbcMessageReadRepository;
import com.snoozeshare.repository.MessageRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.TicketRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.ConversationSummary;
import com.snoozeshare.service.MessageService;

@Service
@ConditionalOnProperty(name = "snoozeshare.persistence.enabled", havingValue = "true", matchIfMissing = false)
public class MessageServiceImpl implements MessageService {

    private static final String SUPPORT_TEAM = "Support team";

    private final MessageRepository messages;
    private final JdbcMessageReadRepository messageReads;
    private final TicketRepository tickets;
    private final BookingRepository bookings;
    private final PropertyRepository properties;
    private final UserRepository users;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public MessageServiceImpl(MessageRepository messages,
                              JdbcMessageReadRepository messageReads,
                              TicketRepository tickets,
                              BookingRepository bookings,
                              PropertyRepository properties,
                              UserRepository users,
                              ApplicationEventPublisher publisher,
                              Clock clock) {
        this.messages = messages;
        this.messageReads = messageReads;
        this.tickets = tickets;
        this.bookings = bookings;
        this.properties = properties;
        this.users = users;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public List<Message> thread(UUID ticketId, ThreadChannel channel, UUID viewerId,
                                Role viewerRole) {
        Ticket ticket = ticket(ticketId);
        requireParty(ticket, channel, viewerId, viewerRole);
        return messages.findThread(ticketId, channel);
    }

    @Override
    @Transactional
    public Message post(UUID ticketId, ThreadChannel channel, UUID authorId, Role authorRole,
                        String body) {
        DomainValidation.requireText(body, "body");
        Ticket ticket = ticket(ticketId);
        requireParty(ticket, channel, authorId, authorRole);
        if (!isOpen(ticket)) {
            throw new IllegalStateException("Ticket is resolved");
        }
        Instant now = clock.instant();
        Message saved = messages.save(new Message(UUID.randomUUID(), ticketId, channel, authorId,
                authorRole, body.trim(), now));
        publishAfterCommit(new MessagePostedEvent(saved.getId(), ticketId, now));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationSummary> conversationsFor(UUID userId, Role role) {
        ThreadChannel channel = channelOf(role);
        if (role == Role.HOST) {
            return tickets.findConversationTicketsByHost(userId, channel).stream()
                    .map(ticket -> summarize(ticket, channel, userId).summary())
                    .toList();
        }
        return ticketsFor(userId, role).stream()
                .map(ticket -> summarize(ticket, channel, userId))
                .sorted(Comparator.comparing(Ranked::lastActivity).reversed()
                        .thenComparing(ranked -> ranked.summary().ticketId()))
                .map(Ranked::summary)
                .toList();
    }

    @Override
    public int unreadCount(UUID userId, Role role) {
        ThreadChannel channel = channelOf(role);
        return ticketsFor(userId, role).stream()
                .mapToInt(ticket -> messageReads.countUnread(ticket.getId(), channel, userId))
                .sum();
    }

    private List<Ticket> ticketsFor(UUID userId, Role role) {
        return switch (role) {
            case GUEST -> tickets.findByGuest(userId);
            case HOST -> tickets.findByHost(userId);
            default -> throw new IllegalArgumentException(
                    "Only guests and hosts have a conversation inbox");
        };
    }

    @Override
    @Transactional
    public void markRead(UUID ticketId, ThreadChannel channel, UUID userId, Role role) {
        requireParty(ticket(ticketId), channel, userId, role);
        messages.findLast(ticketId, channel)
                .ifPresent(last -> messageReads.markRead(ticketId, channel, userId,
                        last.getId()));
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

    private Ranked summarize(Ticket ticket, ThreadChannel channel, UUID userId) {
        Booking booking = booking(ticket);
        Property property = property(booking);
        Optional<Message> last = messages.findLast(ticket.getId(), channel);
        String counterpart = ticket.getAssignedAgentId() == null ? SUPPORT_TEAM
                : users.findById(ticket.getAssignedAgentId())
                        .map(User::getDisplayName).orElse(SUPPORT_TEAM);
        ConversationSummary summary = new ConversationSummary(ticket.getId(), channel,
                DisputeQueryServiceImpl.label(ticket.getId()), ticket.getTitle(),
                property.getTitle(), counterpart, last.orElse(null),
                messageReads.countUnread(ticket.getId(), channel, userId), isOpen(ticket));
        return new Ranked(summary, last.map(Message::getSentAt).orElse(ticket.getCreatedAt()));
    }

    private void requireParty(Ticket ticket, ThreadChannel channel, UUID userId, Role role) {
        User user = users.findById(userId)
                .filter(found -> found.getRole() == role)
                .orElseThrow(() -> new IllegalArgumentException("Unknown user for this role"));
        if (role == Role.AGENT) {
            return;
        }
        Booking booking = booking(ticket);
        boolean party = switch (role) {
            case GUEST -> channel == ThreadChannel.GUEST
                    && booking.getGuestId().equals(user.getId());
            case HOST -> channel == ThreadChannel.HOST
                    && property(booking).getHostId().equals(user.getId());
            default -> false;
        };
        if (!party) {
            throw new IllegalArgumentException("Not a participant in this thread");
        }
    }

    private static ThreadChannel channelOf(Role role) {
        return switch (role) {
            case GUEST -> ThreadChannel.GUEST;
            case HOST -> ThreadChannel.HOST;
            default -> throw new IllegalArgumentException(
                    "Only guests and hosts have a conversation inbox");
        };
    }

    private static boolean isOpen(Ticket ticket) {
        return ticket.getStatus() == TicketStatus.OPEN
                || ticket.getStatus() == TicketStatus.IN_REVIEW;
    }

    private Ticket ticket(UUID ticketId) {
        return tickets.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket does not exist"));
    }

    private Booking booking(Ticket ticket) {
        return bookings.findById(ticket.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
    }

    private Property property(Booking booking) {
        return properties.findById(booking.getListingId())
                .orElseThrow(() -> new IllegalArgumentException("Property does not exist"));
    }

    private record Ranked(ConversationSummary summary, Instant lastActivity) {
    }
}
