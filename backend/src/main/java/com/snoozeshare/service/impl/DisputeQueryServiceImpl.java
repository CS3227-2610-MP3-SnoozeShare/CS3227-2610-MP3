package com.snoozeshare.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.AssigneeFilter;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.settlement.EscrowPolicy;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.LedgerRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.TicketRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.DisputeDetail;
import com.snoozeshare.service.DisputeQueryService;
import com.snoozeshare.service.DisputeSummary;
import com.snoozeshare.service.NotFoundException;

@Service
@ConditionalOnDatabase
public class DisputeQueryServiceImpl implements DisputeQueryService {

    private static final String UNKNOWN_USER = "Unknown user";

    private final TicketRepository tickets;
    private final BookingRepository bookings;
    private final PropertyRepository properties;
    private final UserRepository users;
    private final LedgerRepository ledgerEntries;
    private final Clock clock;

    public DisputeQueryServiceImpl(TicketRepository tickets, BookingRepository bookings,
                                   PropertyRepository properties, UserRepository users,
                                   LedgerRepository ledgerEntries, Clock clock) {
        this.tickets = tickets;
        this.bookings = bookings;
        this.properties = properties;
        this.users = users;
        this.ledgerEntries = ledgerEntries;
        this.clock = clock;
    }

    @Override
    public List<DisputeSummary> queue(TicketStatus status, AssigneeFilter assignee,
                                      UUID agentId) {
        List<Ticket> found = tickets.findQueue(status, assignee, agentId);
        Map<UUID, Booking> bookingsById = byId(bookings.findAllById(
                found.stream().map(Ticket::getBookingId).collect(Collectors.toSet())), Booking::getId);
        Map<UUID, Property> propertiesById = byId(properties.findAllById(
                bookingsById.values().stream().map(Booking::getListingId).collect(Collectors.toSet())),
                Property::getId);
        Set<UUID> userIds = new HashSet<>();
        for (Ticket ticket : found) {
            Booking booking = require(bookingsById.get(ticket.getBookingId()), "Booking does not exist");
            Property property = require(propertiesById.get(booking.getListingId()), "Property does not exist");
            userIds.add(booking.getGuestId());
            userIds.add(property.getHostId());
            if (ticket.getAssignedAgentId() != null) {
                userIds.add(ticket.getAssignedAgentId());
            }
        }
        Map<UUID, String> names = new HashMap<>();
        users.findAllById(userIds).forEach(user -> names.put(user.getId(), user.getDisplayName()));
        return found.stream().map(ticket -> {
            Booking booking = bookingsById.get(ticket.getBookingId());
            Property property = propertiesById.get(booking.getListingId());
            return new DisputeSummary(ticket.getId(), label(ticket.getId()), ticket.getTitle(),
                    ticket.getCategory(), property.getTitle(),
                    names.getOrDefault(booking.getGuestId(), UNKNOWN_USER),
                    names.getOrDefault(property.getHostId(), UNKNOWN_USER),
                    ticket.getAssignedAgentId() == null ? null
                            : names.getOrDefault(ticket.getAssignedAgentId(), UNKNOWN_USER),
                    ticket.getStatus(), ticket.getCreatedAt());
        }).toList();
    }

    private static <T> T require(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static <T> Map<UUID, T> byId(Iterable<T> items, Function<T, UUID> id) {
        Map<UUID, T> map = new HashMap<>();
        items.forEach(item -> map.put(id.apply(item), item));
        return map;
    }

    @Override
    public int unassignedCount() {
        return (int) tickets.countByStatusAndAssignedAgentIdIsNull(TicketStatus.OPEN);
    }

    @Override
    public DisputeDetail detail(UUID ticketId) {
        Ticket ticket = tickets.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket does not exist"));
        Booking booking = booking(ticket);
        Property property = property(booking);
        boolean held = EscrowPolicy.isHeld(ledgerEntries.entriesForBooking(booking.getId()));
        return new DisputeDetail(ticket, label(ticketId), property.getTitle(),
                booking.getStartDate(), booking.getEndDate(), name(booking.getGuestId()),
                name(property.getHostId()), name(ticket.getRaisedByUserId()),
                agentName(ticket), booking.getStatus(), booking.getTotalAmount(), held,
                phase(booking, held, LocalDate.now(clock)));
    }

    private Booking booking(Ticket ticket) {
        return bookings.findById(ticket.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
    }

    private Property property(Booking booking) {
        return properties.findById(booking.getListingId())
                .orElseThrow(() -> new IllegalArgumentException("Property does not exist"));
    }

    private String agentName(Ticket ticket) {
        return ticket.getAssignedAgentId() == null ? null
                : name(ticket.getAssignedAgentId());
    }

    private String name(UUID userId) {
        return users.findById(userId).map(User::getDisplayName).orElse(UNKNOWN_USER);
    }

    static String label(UUID ticketId) {
        String text = ticketId.toString();
        return "#" + text.substring(text.length() - 4);
    }

    private static String phase(Booking booking, boolean held, LocalDate today) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            if (held && booking.getEndDate().isBefore(today)) {
                return "Stay ended \u2014 escrow held";
            }
            if (booking.getStartDate().isAfter(today)) {
                return "Upcoming";
            }
            if (!booking.getEndDate().isBefore(today)) {
                return "Active";
            }
        }
        return prettify(booking.getStatus());
    }

    private static String prettify(BookingStatus status) {
        String text = status.name().replace('_', ' ').toLowerCase();
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
