package com.snoozeshare.ui.guest.tickets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.snoozeshare.domain.model.TicketCategory;

class TicketFilingControllerTest {

    private static TicketCategory category(String label) {
        return new TicketCategory(UUID.randomUUID(), label, true);
    }

    @Test
    void otherIsAlwaysTheLastCategoryWhateverTheServiceOrder() {
        List<String> labels = TicketFilingController.categoryLabels(List.of(
                category("Cleanliness"), category("Other"), category("Damage Dispute"),
                category("Host Unresponsive")));

        assertEquals(List.of("Cleanliness", "Damage Dispute", "Host Unresponsive", "Other"), labels);
    }

    @Test
    void categoriesWithoutOtherKeepTheirOrder() {
        List<String> labels = TicketFilingController.categoryLabels(List.of(
                category("Property Mismatch"), category("Cleanliness")));

        assertEquals(List.of("Property Mismatch", "Cleanliness"), labels);
    }
}
