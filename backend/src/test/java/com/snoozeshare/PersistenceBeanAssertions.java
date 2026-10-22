package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.context.ApplicationContext;

import com.snoozeshare.repository.JdbcBookingMessageReadRepository;
import com.snoozeshare.repository.JdbcLedgerRepository;
import com.snoozeshare.repository.JdbcMessageReadRepository;
import com.snoozeshare.service.AccountGovernanceService;
import com.snoozeshare.service.AuditService;
import com.snoozeshare.service.AvailabilityService;
import com.snoozeshare.service.BookingConversationService;
import com.snoozeshare.service.BookingService;
import com.snoozeshare.service.DisputeQueryService;
import com.snoozeshare.service.DisputeSettlementService;
import com.snoozeshare.service.ListingMetricsService;
import com.snoozeshare.service.ListingService;
import com.snoozeshare.service.MessageService;
import com.snoozeshare.service.ReviewService;
import com.snoozeshare.service.TicketService;
import com.snoozeshare.service.TransactionService;
import com.snoozeshare.service.UserService;
import com.snoozeshare.service.WalletService;

/** Assertions for the complete set of persistence-backed Spring beans (N6). */
final class PersistenceBeanAssertions {

    private static final Class<?>[] PERSISTENCE_BEAN_TYPES = {
        AccountGovernanceService.class,
        AuditService.class,
        AvailabilityService.class,
        BookingConversationService.class,
        BookingService.class,
        DisputeQueryService.class,
        DisputeSettlementService.class,
        ListingMetricsService.class,
        ListingService.class,
        MessageService.class,
        ReviewService.class,
        TicketService.class,
        TransactionService.class,
        UserService.class,
        WalletService.class,
        JdbcBookingMessageReadRepository.class,
        JdbcLedgerRepository.class,
        JdbcMessageReadRepository.class
    };

    private PersistenceBeanAssertions() {
    }

    static void assertPresent(ApplicationContext context) {
        assertThat(context.getEnvironment()
                .getProperty("snoozeshare.persistence.enabled", Boolean.class)).isTrue();
        assertTypesPresent(context);
    }

    static void assertAbsent(ApplicationContext context) {
        assertThat(context.getEnvironment()
                .getProperty("snoozeshare.persistence.enabled", Boolean.class)).isFalse();
        for (Class<?> beanType : PERSISTENCE_BEAN_TYPES) {
            assertThat(context.getBeanNamesForType(beanType))
                    .as("bean type %s", beanType.getSimpleName())
                    .isEmpty();
        }
    }

    private static void assertTypesPresent(ApplicationContext context) {
        for (Class<?> beanType : PERSISTENCE_BEAN_TYPES) {
            assertThat(context.getBeanNamesForType(beanType))
                    .as("bean type %s", beanType.getSimpleName())
                    .isNotEmpty();
        }
    }
}
