package com.snoozeshare.api.host;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.repository.AuthUserRepository;
import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.PropertyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.AuditLogEntry;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.repository.AuditLogRepository;
import com.snoozeshare.repository.AvailabilityBlockRepository;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.AvailabilityService;

/** Exercises authenticated Host calendar contracts against PostgreSQL. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "snoozeshare.security.enabled=true")
@AutoConfigureMockMvc
@Import(HostListingsControllerTest.TestAuthConfiguration.class)
class HostCalendarIntegrationTest {

    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthUserRepository authUsers;

    @Autowired
    private PropertyRepository properties;

    @Autowired
    private AvailabilityBlockRepository blocks;

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private UserRepository users;

    @Autowired
    private AuditLogRepository audits;

    @Autowired
    private AvailabilityService availability;

    @Autowired
    private HostListingsControllerTest.FakeTokenVerifier verifier;

    private UUID hostId;
    private UUID propertyId;
    private String token;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        propertyId = UUID.randomUUID();
        token = "host-calendar-" + UUID.randomUUID();
        verifier.add(token, token);
        authUsers.insert(new AuthUserRepository.AuthUser(hostId, token, token + "@example.com",
            AuthRole.HOST, "Host", "ACTIVE", null, null));
        users.save(new User(hostId, token, Role.HOST, "Host", token + "@example.com",
            AccountStatus.ACTIVE, null, Instant.now(), Instant.now(), null));
        properties.save(new Property(propertyId, hostId, ListingStatus.ACTIVE, "Calendar listing",
            "Description", PropertyType.APARTMENT, "1 Main St", "Singapore", "Central", 123456,
            2, 1, 1, new BigDecimal("80.00"), LocalTime.of(15, 0), LocalTime.of(11, 0),
            Set.of(), Instant.now()));
    }

    @Test
    void monthProjectionReturnsAscendingDaysAndInclusiveBlockRanges() throws Exception {
        YearMonth month = YearMonth.now().plusYears(1).withMonth(3);
        LocalDate start = month.atDay(10);
        LocalDate end = month.atDay(12);
        String created = mockMvc.perform(post("/api/host/listings/" + propertyId + "/blocks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(blockBody(start, end, "  Maintenance  ")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.startDate").value(start.toString()))
            .andExpect(jsonPath("$.endDate").value(end.toString()))
            .andExpect(jsonPath("$.reason").value("Maintenance"))
            .andReturn().getResponse().getContentAsString();

        UUID blockId = UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(created).get("id").asText());
        assertThat(blocks.findById(blockId).orElseThrow().getEndDate()).isEqualTo(end.plusDays(1));

        mockMvc.perform(get("/api/host/listings/" + propertyId + "/calendar")
                .param("year", Integer.toString(month.getYear()))
                .param("month", Integer.toString(month.getMonthValue()))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.days.length()").value(month.lengthOfMonth()))
            .andExpect(jsonPath("$.days[0].date").value(month.atDay(1).toString()))
            .andExpect(jsonPath("$.days[30].date").value(month.atDay(31).toString()))
            .andExpect(jsonPath("$.days[9].blocked").value(true))
            .andExpect(jsonPath("$.days[9].booked").value(false))
            .andExpect(jsonPath("$.blocks[0].endDate").value(end.toString()));

        assertThat(audits.findAll()).filteredOn(entry -> entry.getEntityId().equals(blockId))
            .singleElement()
            .satisfies(entry -> {
                assertThat(entry.getActionType()).isEqualTo("HOST_BLOCK_CREATED");
                assertThat(entry.getReason()).contains(start.toString(), end.toString(), "Maintenance");
            });
    }

    @Test
    void rejectsInvalidAndOverlappingRangesWithoutChangingExistingBlock() throws Exception {
        LocalDate start = LocalDate.now().plusDays(30);
        LocalDate end = start.plusDays(2);
        String created = mockMvc.perform(post("/api/host/listings/" + propertyId + "/blocks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(blockBody(start, end, null)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        UUID createdId = UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(created).get("id").asText());

        LocalDate past = LocalDate.now().minusDays(2);
        mockMvc.perform(post("/api/host/listings/" + propertyId + "/blocks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(blockBody(past, past.plusDays(1), null)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("DOMAIN_RULE_VIOLATION"));

        mockMvc.perform(post("/api/host/listings/" + propertyId + "/blocks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(blockBody(end, end.plusDays(1), null)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("DOMAIN_RULE_VIOLATION"));
        mockMvc.perform(post("/api/host/listings/" + propertyId + "/blocks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(blockBody(end, start, null)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("DOMAIN_RULE_VIOLATION"));
        mockMvc.perform(post("/api/host/listings/" + propertyId + "/blocks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\":\"not-a-date\",\"endDate\":\"2030-01-02\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(blocks.findByPropertyId(propertyId)).hasSize(1);
        assertThat(audits.findAll()).filteredOn(entry -> createdId.equals(entry.getEntityId()))
            .extracting(AuditLogEntry::getActionType)
            .containsExactly("HOST_BLOCK_CREATED");
    }

    @Test
    void concurrentOverlappingRequestsCreateAtMostOneBlock() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        LocalDate from = LocalDate.now().plusDays(60);
        LocalDate to = from.plusDays(2);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<String>> outcomes = List.of(
                executor.submit(() -> createAfter(start, from, to)),
                executor.submit(() -> createAfter(start, from, to)));
            start.countDown();

            assertThat(outcomes.stream().map(this::outcome).toList())
                .containsExactlyInAnyOrder("created", "overlap");
        }
        assertThat(blocks.findByPropertyId(propertyId)).hasSize(1);
    }

    @Test
    void calendarIncludesBookingsWithoutStoredAvailabilityRows() throws Exception {
        YearMonth month = YearMonth.now().plusYears(1).withMonth(4);
        UUID guestId = UUID.randomUUID();
        users.save(new User(guestId, "calendar-guest-" + guestId, Role.GUEST, "Guest",
            guestId + "@example.com", AccountStatus.ACTIVE, null, Instant.now(), Instant.now(), null));
        LocalDate checkIn = month.atDay(5);
        LocalDate checkOut = month.atDay(7);
        bookings.save(new Booking(UUID.randomUUID(), propertyId, guestId, checkIn, checkOut,
            BookingStatus.PENDING, new BigDecimal("80.00"), new BigDecimal("160.00"),
            Instant.now(), null, null, null));

        mockMvc.perform(get("/api/host/listings/" + propertyId + "/calendar")
                .param("year", Integer.toString(month.getYear()))
                .param("month", Integer.toString(month.getMonthValue()))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.days[4].booked").value(true))
            .andExpect(jsonPath("$.days[5].booked").value(true))
            .andExpect(jsonPath("$.days[6].booked").value(false))
            .andExpect(jsonPath("$.blocks[0].source").value("BOOKING"))
            .andExpect(jsonPath("$.blocks[0].endDate").value(checkOut.minusDays(1).toString()))
            .andExpect(jsonPath("$.blocks[0].reason").doesNotExist());
    }

    @Test
    void deletingOwnedBlockReturnsNoContentAndAuditsTheRemoval() throws Exception {
        LocalDate start = LocalDate.now().plusDays(150);
        String created = mockMvc.perform(post("/api/host/listings/" + propertyId + "/blocks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(blockBody(start, start, "Owner use")))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        UUID blockId = UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(created).get("id").asText());

        mockMvc.perform(delete("/api/host/blocks/" + blockId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
        assertThat(blocks.findById(blockId)).isEmpty();
        assertThat(audits.findAll()).extracting(AuditLogEntry::getActionType)
            .contains("HOST_BLOCK_REMOVED");
        mockMvc.perform(delete("/api/host/blocks/" + blockId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void anotherHostCannotReadTheCalendarOrRemoveItsBlocks() throws Exception {
        LocalDate start = LocalDate.now().plusDays(90);
        String created = mockMvc.perform(post("/api/host/listings/" + propertyId + "/blocks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(blockBody(start, start.plusDays(1), null)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        UUID blockId = UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(created).get("id").asText());

        UUID otherHostId = UUID.randomUUID();
        String otherToken = "other-calendar-" + otherHostId;
        verifier.add(otherToken, otherToken);
        authUsers.insert(new AuthUserRepository.AuthUser(otherHostId, otherToken,
            otherToken + "@example.com", AuthRole.HOST, "Other", "ACTIVE", null, null));
        users.save(new User(otherHostId, otherToken, Role.HOST, "Other", otherToken + "@example.com",
            AccountStatus.ACTIVE, null, Instant.now(), Instant.now(), null));

        mockMvc.perform(get("/api/host/listings/" + propertyId + "/calendar")
                .param("year", Integer.toString(start.getYear()))
                .param("month", Integer.toString(start.getMonthValue()))
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mockMvc.perform(delete("/api/host/blocks/" + blockId)
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        assertThat(blocks.findById(blockId)).isPresent();
    }

    @Test
    void reasonLimitCountsUnicodeCodePointsAndUnknownFieldsAreRejected() throws Exception {
        LocalDate start = LocalDate.now().plusDays(120);
        String tooLongReason = "😀".repeat(241);
        mockMvc.perform(post("/api/host/listings/" + propertyId + "/blocks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(blockBody(start, start.plusDays(1), tooLongReason)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/host/listings/" + propertyId + "/blocks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\":\"" + start + "\",\"endDate\":\""
                    + start.plusDays(1) + "\",\"hostId\":\"" + UUID.randomUUID() + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void calendarRejectsOutOfRangeYearAndMonth() throws Exception {
        mockMvc.perform(get("/api/host/listings/" + propertyId + "/calendar")
                .param("year", "1999")
                .param("month", "1")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/host/listings/" + propertyId + "/calendar")
                .param("year", "2100")
                .param("month", "13")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private String createAfter(CountDownLatch start, LocalDate from, LocalDate to) throws Exception {
        start.await();
        try {
            availability.createHostBlock(propertyId, from, to, hostId, null);
            return "created";
        } catch (IllegalArgumentException exception) {
            return "overlap";
        }
    }

    private String outcome(Future<String> result) {
        try {
            return result.get();
        } catch (Exception exception) {
            throw new AssertionError("Calendar operation failed unexpectedly", exception);
        }
    }

    private String blockBody(LocalDate start, LocalDate end, String reason) {
        String reasonJson = reason == null ? "null" : "\"" + reason + "\"";
        return "{\"startDate\":\"" + start + "\",\"endDate\":\"" + end
            + "\",\"reason\":" + reasonJson + "}";
    }
}
