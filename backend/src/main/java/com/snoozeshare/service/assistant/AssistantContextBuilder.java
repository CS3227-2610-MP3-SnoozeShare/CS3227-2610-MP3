package com.snoozeshare.service.assistant;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.snoozeshare.ai.AuditContext;
import com.snoozeshare.ai.LlmCall;
import com.snoozeshare.ai.Turn;
import com.snoozeshare.ai.TurnRole;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Message;
import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.model.TicketAssistant;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.MessageRepository;
import com.snoozeshare.repository.TicketAssistantRepository;
import com.snoozeshare.repository.TicketRepository;
import com.snoozeshare.repository.UserRepository;

/** Repository-only snapshot construction; the job supplies one read-only transaction. */
@Component
@ConditionalOnDatabase
@ConditionalOnProperty(prefix = "snoozeshare.assistant", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AssistantContextBuilder {
    private final TicketRepository tickets;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final MessageRepository messages;
    private final TicketAssistantRepository assistants;
    private final AssistantProperties properties;
    private final Clock clock;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public AssistantContextBuilder(TicketRepository tickets, BookingRepository bookings, UserRepository users,
            MessageRepository messages, TicketAssistantRepository assistants, AssistantProperties properties,
            Clock clock) {
        this.tickets = tickets;
        this.bookings = bookings;
        this.users = users;
        this.messages = messages;
        this.assistants = assistants;
        this.properties = properties;
        this.clock = clock;
    }

    /** Only immutable scalars/public gateway records escape the transaction. */
    public record Snapshot(LlmCall call, UUID lastGuestMessageId, boolean open, boolean pending,
                           boolean guestRaised, boolean activeGuest, boolean agentSpoke, boolean userLast) { }

    /** Missing related records are a no-op, leaving the bookkeeping row visibly stale. */
    public Snapshot load(UUID id) {
        Ticket ticket = tickets.findById(id).orElse(null);
        TicketAssistant row = assistants.findById(id).orElse(null);
        if (ticket == null || row == null) {
            return null;
        }
        Booking booking = bookings.findById(ticket.getBookingId()).orElse(null);
        var guest = users.findById(ticket.getRaisedByUserId()).orElse(null);
        if (booking == null || guest == null) {
            return null;
        }
        List<Message> thread = ordered(id);
        List<Turn> turns = new ArrayList<>();
        turns.add(new Turn(TurnRole.USER, "Title: " + ticket.getTitle() + "\nDescription: " + ticket.getDescription()
            + "\nSupporting text: " + (ticket.getSupportingText() == null ? "none" : ticket.getSupportingText())));
        UUID lastGuest = null;
        boolean agentSpoke = false;
        Message omitted = catchUpOmission(thread, row.getLastMessageId());
        for (Message message : thread) {
            if (message == omitted) {
                continue;
            }
            if (message.getAuthorRole() == Role.AGENT) {
                agentSpoke = true;
            } else if (message.getAuthorRole() == Role.GUEST) {
                lastGuest = message.getId();
                if (turns.getLast().role() == TurnRole.USER) {
                    Turn previous = turns.removeLast();
                    turns.add(new Turn(TurnRole.USER, previous.text() + "\n" + message.getBody()));
                } else {
                    turns.add(new Turn(TurnRole.USER, message.getBody()));
                }
            } else if (message.getAuthorRole() == Role.SYSTEM && ClarifyRendering.isQuestion(message.getBody())) {
                turns.add(new Turn(TurnRole.ASSISTANT, ClarifyRendering.replayQuestions(message.getBody())));
            }
        }
        LlmCall call = new LlmCall(ticket.getRaisedByUserId(), Role.GUEST, List.copyOf(turns),
            facts(ticket, booking, row.getRounds()), null,
            new AuditContext(id, ticket.getBookingId(), ticket.getRaisedByUserId()));
        return new Snapshot(call, lastGuest, open(ticket), row.getState() == TicketAssistant.State.PENDING,
            ticket.getRaisedByRole() == Role.GUEST, guest.getStatus() == AccountStatus.ACTIVE,
            agentSpoke, turns.getLast().role() == TurnRole.USER);
    }

    /**
     * SD23 catch-up selection: the newest recognized question is left out of this call only when the newest Guest
     * message precedes it and is newer than the cursor, since that reply was not seen when the question was made.
     */
    private static Message catchUpOmission(List<Message> thread, UUID cursor) {
        int guest = -1;
        int question = -1;
        int cursorAt = -1;
        for (int i = 0; i < thread.size(); i++) {
            Message message = thread.get(i);
            if (message.getAuthorRole() == Role.GUEST) {
                guest = i;
            } else if (message.getAuthorRole() == Role.SYSTEM && ClarifyRendering.isQuestion(message.getBody())) {
                question = i;
            }
            if (cursor != null && message.getId().equals(cursor)) {
                cursorAt = i;
            }
        }
        boolean newer = cursor == null ? guest >= 0 : cursorAt >= 0 && guest > cursorAt;
        return guest >= 0 && question > guest && newer ? thread.get(question) : null;
    }

    /** Compares positions in the repository's stable sent_at/id ordering, including equal-time ties. */
    public boolean hasNewerGuest(UUID id, UUID lastMessageId) {
        List<Message> thread = ordered(id);
        boolean after = lastMessageId == null;
        for (Message message : thread) {
            if (after && message.getAuthorRole() == Role.GUEST) {
                return true;
            }
            if (message.getId().equals(lastMessageId)) {
                after = true;
            }
        }
        return false;
    }

    private List<Message> ordered(UUID id) {
        List<Message> thread = new ArrayList<>(messages.findThread(id, ThreadChannel.GUEST));
        Collections.reverse(thread);
        return thread;
    }

    private String facts(Ticket ticket, Booking booking, int rounds) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of("Asia/Singapore")));
        return "Ticket status: " + ticket.getStatus().name()
            + "\nRequested remedy: " + ticket.getRequestedRemedy().name()
            + "\nBooking status: " + booking.getStatus().name()
            + "\nStay: " + booking.getStartDate() + " to " + booking.getEndDate() + " ("
            + ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate()) + " nights)"
            + "\nBooking total: " + booking.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString() + " SGD"
            + "\nDays since check-out: " + ChronoUnit.DAYS.between(booking.getEndDate(), today)
            + "\nToday: " + today + "\nQuestion rounds already asked: " + rounds + " of " + properties.maxRounds();
    }

    static boolean open(Ticket ticket) {
        return ticket.getStatus() == TicketStatus.OPEN || ticket.getStatus() == TicketStatus.IN_REVIEW;
    }
}
