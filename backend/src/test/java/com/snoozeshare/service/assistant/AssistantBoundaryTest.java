package com.snoozeshare.service.assistant;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.settlement.SettlementCalculator;
import com.snoozeshare.service.TicketService;
import com.snoozeshare.service.WalletService;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

/** R2/E50: gathering facts cannot turn into settlement or notes mutation. */
class AssistantBoundaryTest {
    private static final String AREA = "com.snoozeshare.service.assistant..";

    @Test
    void productionAssistantCannotSettleOrWriteNotesE50() {
        var production = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.snoozeshare.service.assistant");
        services().check(production);
        settlement().check(production);
        notes().check(production);
    }

    @Test
    void deliberateForbiddenDependenciesAndNotesWritesFailE50() {
        var fixtures = new ClassFileImporter().importClasses(ForbiddenFixture.class);
        for (ArchRule rule : new ArchRule[] {services(), settlement(), notes()}) {
            var result = rule.evaluate(fixtures);
            assertThat(result.hasViolation()).isTrue();
            assertThat(result.getFailureReport().getDetails().toString()).contains("ForbiddenFixture");
        }
    }

    private static ArchRule services() {
        return noClasses().that().resideInAPackage(AREA).should().dependOnClassesThat().haveNameMatching(
                "com[.]snoozeshare[.]service([.]impl)?[.](TicketService|DisputeSettlementService|TransactionService|"
                + "LedgerWriter|WalletService|BookingService|AccountGovernanceService)(Impl)?");
    }

    private static ArchRule settlement() {
        return noClasses().that().resideInAPackage(AREA).should().dependOnClassesThat()
                .resideInAPackage("com.snoozeshare.domain.settlement..");
    }

    private static ArchRule notes() {
        return noClasses().that().resideInAPackage(AREA).should()
                .callMethod(Ticket.class, "setAgentNotes", String.class);
    }

    static final class ForbiddenFixture {
        private TicketService tickets;
        private WalletService wallets;
        private SettlementCalculator calculator;

        void overwrite(Ticket ticket) {
            ticket.setAgentNotes("Synthetic violation");
        }
    }
}
