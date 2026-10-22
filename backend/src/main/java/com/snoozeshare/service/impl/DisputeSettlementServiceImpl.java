package com.snoozeshare.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.AuditAction;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.enums.ResolutionMode;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.enums.WalletTransactionType;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.domain.model.WalletTransaction;
import com.snoozeshare.domain.settlement.EscrowPolicy;
import com.snoozeshare.domain.settlement.SettlementBreakdown;
import com.snoozeshare.domain.settlement.SettlementCalculator;
import com.snoozeshare.domain.statemachine.BookingStateMachine;
import com.snoozeshare.domain.statemachine.TicketStateMachine;
import com.snoozeshare.domain.validation.DomainValidation;
import com.snoozeshare.infra.events.TicketResolvedEvent;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.LedgerRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.TicketRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.repository.WalletRepository;
import com.snoozeshare.service.AuditRecord;
import com.snoozeshare.service.AuditService;
import com.snoozeshare.service.DisputeSettlementService;
import com.snoozeshare.service.Settlement;

@Service
@ConditionalOnDatabase
public class DisputeSettlementServiceImpl implements DisputeSettlementService {

    private final TicketRepository tickets;
    private final BookingRepository bookings;
    private final PropertyRepository properties;
    private final UserRepository users;
    private final WalletRepository wallets;
    private final LedgerWriter ledger;
    private final LedgerRepository ledgerEntries;
    private final AuditService audit;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public DisputeSettlementServiceImpl(TicketRepository tickets,
                                        BookingRepository bookings,
                                        PropertyRepository properties,
                                        UserRepository users,
                                        WalletRepository wallets,
                                        LedgerWriter ledger,
                                        LedgerRepository ledgerEntries,
                                        AuditService audit,
                                        ApplicationEventPublisher publisher,
                                        Clock clock) {
        this.tickets = tickets;
        this.bookings = bookings;
        this.properties = properties;
        this.users = users;
        this.wallets = wallets;
        this.ledger = ledger;
        this.ledgerEntries = ledgerEntries;
        this.audit = audit;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Settlement settle(UUID ticketId, ResolutionMode mode, BigDecimal guestRefund,
                             UUID agentId, String reason) {
        DomainValidation.requireText(reason, "reason");
        User agent = agentId == null ? null : users.findById(agentId).orElse(null);
        AuthorizationService.requireRole(Role.AGENT, agent);
        Instant now = clock.instant();
        Settlement settlement = apply(ticketId, mode, guestRefund, agentId, reason.trim(), now);
        publisher.publishEvent(new TicketResolvedEvent(ticketId, agentId, now));
        return settlement;
    }

    private Settlement apply(UUID ticketId, ResolutionMode mode, BigDecimal guestRefund,
                             UUID agentId, String reason, Instant now) {
        Ticket ticket = tickets.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket does not exist"));
        if (ticket.getStatus() != TicketStatus.IN_REVIEW) {
            throw new IllegalStateException("Ticket is not in review");
        }
        if (!agentId.equals(ticket.getAssignedAgentId())) {
            throw new IllegalStateException("Ticket is not assigned to this agent");
        }
        Booking booking = bookings.findById(ticket.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking is not confirmed");
        }
        if (!EscrowPolicy.isHeld(ledgerEntries.entriesForBooking(booking.getId()))) {
            throw new IllegalStateException("Escrow is not held for this booking");
        }
        SettlementBreakdown split = SettlementCalculator.split(booking.getTotalAmount(),
                guestRefund);
        requireConsistent(mode, split, ticket);
        TicketStatus resolved = statusFor(mode, split);
        if (!TicketStateMachine.canTransition(ticket.getStatus(), resolved, Role.AGENT)
                || !BookingStateMachine.canTransition(booking.getStatus(),
                        BookingStatus.COMPLETED, Role.AGENT)) {
            throw new IllegalStateException("Transition is not allowed");
        }
        Wallet guestWallet = wallets.findByUserId(booking.getGuestId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Guest wallet does not exist"));
        Property property = properties.findById(booking.getListingId())
                .orElseThrow(() -> new IllegalArgumentException("Property does not exist"));
        Wallet hostWallet = wallets.findByUserId(property.getHostId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Host wallet does not exist"));

        ticket.setStatus(resolved);
        ticket.setResolutionReason(reason);
        ticket.setResolvedAt(now);
        tickets.save(ticket);

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(now);
        bookings.save(booking);

        audit.record(AuditRecord.builder(agentId, AuditAction.TICKET_RESOLVED, "Ticket",
                        ticketId)
                .status(TicketStatus.IN_REVIEW, resolved).reason(reason)
                .subject(ticket.getRaisedByUserId())
                .booking(booking.getId()).ticket(ticketId).at(now).build());
        audit.record(AuditRecord.builder(agentId, AuditAction.BOOKING_COMPLETED, "Booking",
                        booking.getId())
                .status(BookingStatus.CONFIRMED, BookingStatus.COMPLETED)
                .reason("Escrow settled by ticket resolution")
                .subject(booking.getGuestId()).booking(booking.getId()).ticket(ticketId)
                .at(now).build());

        WalletTransaction guestTransaction = null;
        WalletTransaction hostTransaction = null;
        if (split.guestRefund().signum() > 0) {
            WalletTransactionType type = mode == ResolutionMode.MANUAL
                    ? WalletTransactionType.AGENT_OVERRIDE
                    : WalletTransactionType.TICKET_REMEDY;
            guestTransaction = ledger.post(guestWallet.getId(), type, split.guestRefund(),
                    agentId, booking.getId(), ticket.getId(), null, now);
        }
        if (split.hostGross().signum() > 0) {
            hostTransaction = ledger.postPayout(hostWallet.getId(), split.hostNet(),
                    split.fee(), agentId, booking.getId(), ticket.getId(), now);
        }
        return new Settlement(ticket, booking, split, guestTransaction, hostTransaction);
    }

    private static void requireConsistent(ResolutionMode mode, SettlementBreakdown split,
                                           Ticket ticket) {
        boolean refunds = split.guestRefund().signum() > 0;
        if (mode == ResolutionMode.REJECT && refunds) {
            throw new IllegalArgumentException("Reject cannot refund the guest");
        }
        if (mode == ResolutionMode.ACCEPT && !refunds
                && ticket.getRequestedRemedy() != RemedyType.HOST_PAYOUT) {
            throw new IllegalArgumentException(
                    "Accept needs a refund above zero unless the requested remedy "
                            + "is a host payout");
        }
    }

    private static TicketStatus statusFor(ResolutionMode mode, SettlementBreakdown split) {
        return switch (mode) {
            case ACCEPT -> TicketStatus.RESOLVED_APPROVED;
            case REJECT -> TicketStatus.RESOLVED_REJECTED;
            case MANUAL -> split.guestRefund().signum() > 0
                    ? TicketStatus.RESOLVED_APPROVED : TicketStatus.RESOLVED_REJECTED;
        };
    }
}
