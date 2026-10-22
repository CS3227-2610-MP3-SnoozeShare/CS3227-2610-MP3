package com.snoozeshare.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.AssigneeFilter;
import com.snoozeshare.domain.enums.AuditAction;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.model.TicketCategory;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.policy.DisputeWindowPolicy;
import com.snoozeshare.domain.statemachine.TicketStateMachine;
import com.snoozeshare.domain.validation.DomainValidation;
import com.snoozeshare.infra.events.TicketOpenedEvent;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.TicketCategoryRepository;
import com.snoozeshare.repository.TicketRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.AuditRecord;
import com.snoozeshare.service.AuditService;
import com.snoozeshare.service.DisputeSettlementService;
import com.snoozeshare.service.TicketService;
import com.snoozeshare.service.requests.NewTicketRequest;
import com.snoozeshare.service.requests.ResolutionRequest;

@Service
@ConditionalOnDatabase
public class TicketServiceImpl implements TicketService {

    private final TicketRepository tickets;
    private final TicketCategoryRepository categories;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final DisputeSettlementService settlement;
    private final AuditService audit;
    private final Clock clock;
    private final ApplicationEventPublisher publisher;
    private final DisputeWindowPolicy disputeWindow;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public TicketServiceImpl(TicketRepository tickets,
                             TicketCategoryRepository categories,
                             BookingRepository bookings, UserRepository users,
                             DisputeSettlementService settlement, AuditService audit,
                             Clock clock, ApplicationEventPublisher publisher) {
        this.tickets = tickets;
        this.categories = categories;
        this.bookings = bookings;
        this.users = users;
        this.settlement = settlement;
        this.audit = audit;
        this.clock = clock;
        this.publisher = publisher;
        this.disputeWindow = new DisputeWindowPolicy(clock);
    }

    @Override
    public List<Ticket> myTickets(UUID userId) {
        return tickets.findByRaisedByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public Ticket fileTicket(NewTicketRequest request, UUID raisedByUserId, Role raisedByRole) {
        Booking booking = bookings.findById(request.bookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
        if (raisedByRole != Role.GUEST) {
            throw new IllegalArgumentException("Only guests may file a ticket");
        }
        if (!booking.getGuestId().equals(raisedByUserId)) {
            throw new IllegalArgumentException("Only the booking guest may file a ticket");
        }
        if (!disputeWindow.canFileTicket(booking)) {
            throw new IllegalStateException("Booking is not eligible for a dispute");
        }
        String category = DomainValidation.requireText(request.category(), "category");
        boolean validCategory = categories.findByActiveTrue().stream()
                .anyMatch(c -> c.getLabel().equalsIgnoreCase(category.trim()));
        if (!validCategory) {
            throw new IllegalArgumentException("Invalid ticket category");
        }
        String title = DomainValidation.requireText(request.title(), "title");
        String description = DomainValidation.requireText(request.description(), "description");
        boolean duplicate = tickets.findByRaisedByUserIdOrderByCreatedAtDesc(raisedByUserId)
                .stream()
                .anyMatch(t -> t.getBookingId().equals(request.bookingId()));
        if (duplicate) {
            throw new IllegalStateException("A ticket already exists for this booking");
        }
        Instant now = clock.instant();
        Ticket ticket = new Ticket(UUID.randomUUID(), request.bookingId(), raisedByUserId,
                raisedByRole, category.trim(), title.trim(), description.trim(),
                request.requestedRemedy(), request.supportingText(),
                TicketStatus.OPEN, null, null, null, now, null);
        Ticket saved = tickets.save(ticket);
        audit.record(AuditRecord.builder(raisedByUserId, AuditAction.TICKET_OPENED, "Ticket",
                        saved.getId())
                .subject(raisedByUserId).booking(saved.getBookingId()).ticket(saved.getId())
                .at(now).build());
        publisher.publishEvent(new TicketOpenedEvent(saved.getId(), raisedByUserId, now));
        return saved;
    }

    @Override
    public List<TicketCategory> listCategories() {
        return categories.findByActiveTrue();
    }

    @Override
    public List<TicketCategory> listAllCategories() {
        return categories.findAll();
    }

    @Override
    @Transactional
    public TicketCategory createCategory(String label, UUID agentId) {
        requireAgent(agentId);
        String clean = DomainValidation.requireText(label, "label").trim();
        if (categories.labelInUse(clean, null)) {
            throw new IllegalArgumentException("Category label already exists");
        }
        TicketCategory created = categories.save(
                new TicketCategory(UUID.randomUUID(), clean, true));
        audit.record(AuditRecord.builder(agentId, AuditAction.TICKET_CATEGORY_CREATED,
                        "TicketCategory", created.getId())
                .status(null, "ACTIVE").reason("Category created: " + created.getLabel())
                .build());
        return created;
    }

    @Override
    @Transactional
    public TicketCategory renameCategory(UUID categoryId, String label, UUID agentId) {
        requireAgent(agentId);
        TicketCategory existing = loadCategory(categoryId);
        String clean = DomainValidation.requireText(label, "label").trim();
        if (categories.labelInUse(clean, categoryId)) {
            throw new IllegalArgumentException("Category label already exists");
        }
        String oldLabel = existing.getLabel();
        existing.setLabel(clean);
        TicketCategory renamed = categories.save(existing);
        audit.record(AuditRecord.builder(agentId, AuditAction.TICKET_CATEGORY_RENAMED,
                        "TicketCategory", categoryId)
                .reason("Renamed \"" + oldLabel + "\" to \"" + renamed.getLabel() + "\"")
                .build());
        return renamed;
    }

    @Override
    @Transactional
    public TicketCategory setCategoryActive(UUID categoryId, boolean active, UUID agentId) {
        requireAgent(agentId);
        TicketCategory existing = loadCategory(categoryId);
        boolean before = existing.isActive();
        existing.setActive(active);
        TicketCategory updated = categories.save(existing);
        audit.record(AuditRecord.builder(agentId, AuditAction.TICKET_CATEGORY_TOGGLED,
                        "TicketCategory", categoryId)
                .status(before ? "ACTIVE" : "INACTIVE", updated.isActive() ? "ACTIVE" : "INACTIVE")
                .reason("Category: " + existing.getLabel()).build());
        return updated;
    }

    @Override
    @Transactional
    public void deleteCategory(UUID categoryId, UUID agentId) {
        requireAgent(agentId);
        TicketCategory existing = loadCategory(categoryId);
        if (tickets.existsByCategory(existing.getLabel())) {
            throw new IllegalStateException(
                    "Category is in use by tickets; deactivate it instead");
        }
        categories.deleteById(categoryId);
        audit.record(AuditRecord.builder(agentId, AuditAction.TICKET_CATEGORY_DELETED,
                        "TicketCategory", categoryId)
                .status(existing.isActive() ? "ACTIVE" : "INACTIVE", null)
                .reason("Category deleted: " + existing.getLabel()).build());
    }

    @Override
    public List<Ticket> queueForAgent(TicketStatus statusFilter, AssigneeFilter assignee,
                                      UUID agentId) {
        return tickets.findQueue(statusFilter, assignee, agentId);
    }

    @Override
    @Transactional
    public Ticket assignToMe(UUID ticketId, UUID agentId) {
        requireAgent(agentId);
        Ticket ticket = loadTicket(ticketId);
        if (ticket.getStatus() != TicketStatus.OPEN || !TicketStateMachine.canTransition(
                ticket.getStatus(), TicketStatus.IN_REVIEW, Role.AGENT)) {
            throw new IllegalStateException("Ticket is not open");
        }
        if (ticket.getAssignedAgentId() != null
                && !agentId.equals(ticket.getAssignedAgentId())) {
            throw new IllegalStateException("Ticket is already assigned to another agent");
        }
        TicketStatus before = ticket.getStatus();
        ticket.setStatus(TicketStatus.IN_REVIEW);
        ticket.setAssignedAgentId(agentId);
        Ticket updated = tickets.save(ticket);
        audit.record(ticketRow(agentId, AuditAction.TICKET_ASSIGNED, ticket)
                .status(before, updated.getStatus()).reason("Assigned for review").build());
        return updated;
    }

    @Override
    @Transactional
    public Ticket saveNotes(UUID ticketId, String notes, UUID agentId) {
        requireAgent(agentId);
        Ticket ticket = loadTicket(ticketId);
        if (ticket.getStatus() != TicketStatus.IN_REVIEW) {
            throw new IllegalStateException(
                    "Notes can only be saved while the ticket is in review");
        }
        if (!agentId.equals(ticket.getAssignedAgentId())) {
            throw new IllegalStateException("Ticket is not assigned to this agent");
        }
        String text = notes == null ? "" : notes;
        ticket.setAgentNotes(text);
        Ticket updated = tickets.save(ticket);
        audit.record(ticketRow(agentId, AuditAction.TICKET_NOTE_SAVED, ticket)
                .reason("Internal note updated").build());
        return updated;
    }

    @Override
    @Transactional
    public Ticket unassign(UUID ticketId, UUID agentId) {
        requireAgent(agentId);
        Ticket ticket = loadTicket(ticketId);
        if (ticket.getStatus() != TicketStatus.IN_REVIEW || !TicketStateMachine.canTransition(
                ticket.getStatus(), TicketStatus.OPEN, Role.AGENT)) {
            throw new IllegalStateException("Ticket is not in review");
        }
        if (!agentId.equals(ticket.getAssignedAgentId())) {
            throw new IllegalStateException("Ticket is not assigned to this agent");
        }
        TicketStatus before = ticket.getStatus();
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setAssignedAgentId(null);
        Ticket updated = tickets.save(ticket);
        audit.record(ticketRow(agentId, AuditAction.TICKET_UNASSIGNED, ticket)
                .status(before, updated.getStatus()).reason("Returned to the queue").build());
        return updated;
    }

    private static AuditRecord.Builder ticketRow(UUID agentId, AuditAction action,
                                                  Ticket ticket) {
        return AuditRecord.builder(agentId, action, "Ticket", ticket.getId())
                .subject(ticket.getRaisedByUserId()).booking(ticket.getBookingId())
                .ticket(ticket.getId());
    }

    @Override
    @Transactional
    public Ticket resolve(UUID ticketId, ResolutionRequest request, UUID agentId) {
        requireAgent(agentId);
        Ticket ticket = loadTicket(ticketId);
        Booking booking = bookings.findById(ticket.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
        BigDecimal refund = switch (request.mode()) {
            case REJECT -> BigDecimal.ZERO;
            case MANUAL -> requireAmount(request.guestRefund());
            case ACCEPT -> switch (ticket.getRequestedRemedy()) {
                case FULL_REFUND -> booking.getTotalAmount();
                case HOST_PAYOUT -> BigDecimal.ZERO;
                case PARTIAL_REFUND, OTHER -> requirePositive(request.guestRefund());
            };
        };
        return settlement.settle(ticketId, request.mode(), refund, agentId, request.reason())
                .ticket();
    }

    private static BigDecimal requireAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Guest refund is required");
        }
        return amount;
    }

    private static BigDecimal requirePositive(BigDecimal amount) {
        if (requireAmount(amount).signum() <= 0) {
            throw new IllegalArgumentException(
                    "Accept needs a refund above zero; use Reject or Manual adjustment "
                            + "for a zero refund");
        }
        return amount;
    }

    private User requireAgent(UUID agentId) {
        User user = agentId == null ? null
                : users.findById(agentId).orElse(null);
        AuthorizationService.requireRole(Role.AGENT, user);
        return user;
    }

    private Ticket loadTicket(UUID ticketId) {
        return tickets.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket does not exist"));
    }

    private TicketCategory loadCategory(UUID categoryId) {
        return categories.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category does not exist"));
    }
}
