package com.snoozeshare.controller.guest;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.mapper.TicketMapper;
import com.snoozeshare.api.request.FileTicketRequest;
import com.snoozeshare.api.request.PostMessageRequest;
import com.snoozeshare.api.response.BookingConversationSummaryResponse;
import com.snoozeshare.api.response.ConversationSummaryResponse;
import com.snoozeshare.api.response.GuestConversationListResponse;
import com.snoozeshare.api.response.MessageResponse;
import com.snoozeshare.api.response.TicketResponse;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.model.TicketCategory;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.TicketRepository;
import com.snoozeshare.service.BookingConversationService;
import com.snoozeshare.service.MessageService;
import com.snoozeshare.service.TicketService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@ConditionalOnDatabase
public class GuestMessageController {

    private final TicketService ticketService;
    private final MessageService messageService;
    private final BookingConversationService bookingConversationService;
    private final BookingRepository bookings;
    private final TicketRepository tickets;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public GuestMessageController(TicketService ticketService,
                                  MessageService messageService,
                                  BookingConversationService bookingConversationService,
                                  BookingRepository bookings,
                                  TicketRepository tickets) {
        this.ticketService = ticketService;
        this.messageService = messageService;
        this.bookingConversationService = bookingConversationService;
        this.bookings = bookings;
        this.tickets = tickets;
    }

    @GetMapping("/api/guest/tickets/categories")
    List<String> listCategories() {
        return ticketService.listCategories().stream()
                .map(TicketCategory::getLabel)
                .toList();
    }

    @PostMapping("/api/guest/tickets")
    TicketResponse fileTicket(@Valid @RequestBody FileTicketRequest req,
                              HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        Booking booking = bookings.findById(req.bookingId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking does not exist"));
        if (!booking.getGuestId().equals(guestId)) {
            throw new ForbiddenException(
                    "You do not have access to this booking");
        }
        Ticket ticket = ticketService.fileTicket(
                TicketMapper.toServiceRequest(req), guestId, Role.GUEST);
        return TicketResponse.from(ticket);
    }

    @GetMapping("/api/guest/tickets")
    List<TicketResponse> myTickets(HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        return ticketService.myTickets(guestId).stream()
                .map(TicketResponse::from)
                .toList();
    }

    @GetMapping("/api/guest/messages/conversations")
    GuestConversationListResponse conversations(HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        List<ConversationSummaryResponse> ticketConvos =
                messageService.conversationsFor(guestId, Role.GUEST).stream()
                        .map(ConversationSummaryResponse::from)
                        .toList();
        List<BookingConversationSummaryResponse> bookingConvos =
                bookingConversationService.conversationsFor(guestId, Role.GUEST)
                        .stream()
                        .map(BookingConversationSummaryResponse::from)
                        .toList();
        return new GuestConversationListResponse(ticketConvos, bookingConvos);
    }

    @GetMapping("/api/guest/messages/tickets/{ticketId}")
    List<MessageResponse> ticketThread(@PathVariable UUID ticketId,
                                       HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        requireTicketOwnership(ticketId, guestId);
        return messageService.thread(ticketId, ThreadChannel.GUEST,
                guestId, Role.GUEST).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @PostMapping("/api/guest/messages/tickets/{ticketId}")
    MessageResponse postToTicket(@PathVariable UUID ticketId,
                                 @Valid @RequestBody PostMessageRequest req,
                                 HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        requireTicketOwnership(ticketId, guestId);
        return MessageResponse.from(messageService.post(
                ticketId, ThreadChannel.GUEST, guestId, Role.GUEST,
                req.body()));
    }

    @GetMapping("/api/guest/messages/bookings/{bookingId}")
    List<MessageResponse> bookingThread(@PathVariable UUID bookingId,
                                        HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        requireBookingOwnership(bookingId, guestId);
        return bookingConversationService.thread(
                bookingId, guestId, Role.GUEST).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @PostMapping("/api/guest/messages/bookings/{bookingId}")
    MessageResponse postToBooking(@PathVariable UUID bookingId,
                                  @Valid @RequestBody PostMessageRequest req,
                                  HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        requireBookingOwnership(bookingId, guestId);
        return MessageResponse.from(bookingConversationService.post(
                bookingId, guestId, Role.GUEST, req.body()));
    }

    @PutMapping("/api/guest/messages/tickets/{ticketId}/read")
    void markTicketRead(@PathVariable UUID ticketId,
                        HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        requireTicketOwnership(ticketId, guestId);
        messageService.markRead(ticketId, ThreadChannel.GUEST,
                guestId, Role.GUEST);
    }

    @PutMapping("/api/guest/messages/bookings/{bookingId}/read")
    void markBookingRead(@PathVariable UUID bookingId,
                         HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        requireBookingOwnership(bookingId, guestId);
        bookingConversationService.markRead(bookingId, guestId, Role.GUEST);
    }

    private void requireTicketOwnership(UUID ticketId, UUID guestId) {
        Ticket ticket = tickets.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ticket does not exist"));
        if (!ticket.getRaisedByUserId().equals(guestId)) {
            throw new ForbiddenException(
                    "You do not have access to this ticket");
        }
    }

    private void requireBookingOwnership(UUID bookingId, UUID guestId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking does not exist"));
        if (!booking.getGuestId().equals(guestId)) {
            throw new ForbiddenException(
                    "You do not have access to this booking");
        }
    }
}
