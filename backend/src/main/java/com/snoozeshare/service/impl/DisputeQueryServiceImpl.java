package com.snoozeshare.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

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

@Service
@ConditionalOnProperty(name = "snoozeshare.persistence.enabled", havingValue = "true", matchIfMissing = false)
public class DisputeQueryServiceImpl implements DisputeQueryService {

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
        return tickets.findQueue(status, assignee, agentId).stream()
                .map(this::summarize).toList();
    }

    @Override
    public DisputeDetail detail(UUID ticketId) {
        Ticket ticket = tickets.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket does not exist"));
        Booking booking = booking(ticket);
        Property property = property(booking);
        boolean held = EscrowPolicy.isHeld(ledgerEntries.entriesForBooking(booking.getId()));
        return new DisputeDetail(ticket, label(ticketId), property.getTitle(),
                booking.getStartDate(), booking.getEndDate(), name(booking.getGuestId()),
                name(property.getHostId()), name(ticket.getRaisedByUserId()),
                agentName(ticket), booking.getStatus(), booking.getTotalAmount(), held,
                phase(booking, held, LocalDate.now(clock)));
    }

    private DisputeSummary summarize(Ticket ticket) {
        Booking booking = booking(ticket);
        Property property = property(booking);
        return new DisputeSummary(ticket.getId(), label(ticket.getId()), ticket.getTitle(),
                ticket.getCategory(), property.getTitle(), name(booking.getGuestId()),
                name(property.getHostId()), agentName(ticket), ticket.getStatus(),
                ticket.getCreatedAt());
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
        return users.findById(userId).map(User::getDisplayName).orElse("Unknown user");
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
