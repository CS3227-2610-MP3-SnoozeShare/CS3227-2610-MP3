package com.snoozeshare.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/** Protects the boundary between web controllers and persistence/domain internals. */
class ApiLayerTest {

    @Test
    void controllersFollowTheLayerRules() {
        var controllers = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.snoozeshare.api.controller..");
        noPersistenceDependencies("..api.controller..").check(controllers);
        noMoneyAndAuditDependencies("..api.controller..").check(controllers);
        uuidParametersAreExplicit("..api.controller..").check(controllers);
    }

    @Test
    void controllersAvoidPersistenceAndDomainModelTypes() {
        assertFixtureViolation(noPersistenceDependencies("..api.layerfixtures.."), "RepositoryFixture");
    }

    @Test
    void controllersAvoidMoneyAndAuditWriteServices() {
        assertFixtureViolation(noMoneyAndAuditDependencies("..api.layerfixtures.."), "MoneyFixture");
        assertFixtureViolation(noMoneyAndAuditDependencies("..api.layerfixtures.."), "AuditFixture");
    }

    @Test
    void controllerUuidParametersDeclareTheirSource() {
        assertFixtureViolation(uuidParametersAreExplicit("..api.layerfixtures.."), "UuidFixture");
    }

    private static void assertFixtureViolation(ArchRule rule, String fixtureName) {
        var fixtures = new ClassFileImporter().importPackages("com.snoozeshare.api.layerfixtures");
        var result = rule.evaluate(fixtures);
        assertThat(result.hasViolation()).as("rule must reject " + fixtureName).isTrue();
        assertThat(result.getFailureReport().getDetails().toString()).contains(fixtureName);
    }

    private static ArchRule noPersistenceDependencies(String packageName) {
        return noClasses().that().resideInAPackage(packageName)
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..repository..", "..domain.model..", "jakarta.persistence..").allowEmptyShould(true);
    }

    private static ArchRule noMoneyAndAuditDependencies(String packageName) {
        return noClasses().that().resideInAPackage(packageName)
                .should().dependOnClassesThat().haveNameMatching(
                        "com[.]snoozeshare[.]service([.]impl)?[.](LedgerWriter|WalletService|AuditService)")
                .allowEmptyShould(true);
    }

    private static ArchRule uuidParametersAreExplicit(String packageName) {
        return classes().that().resideInAPackage(packageName)
                .should(new ArchCondition<>("declare UUID method parameters with a source annotation") {
                    @Override
                    public void check(JavaClass item, ConditionEvents events) {
                        for (JavaMethod method : item.getMethods()) {
                            for (int index = 0; index < method.getRawParameterTypes().size(); index++) {
                                if (method.getRawParameterTypes().get(index).isEquivalentTo(UUID.class)) {
                                    boolean annotated = method.getParameters().get(index).getAnnotations().stream()
                                            .anyMatch(annotation -> annotation.getRawType().getName()
                                                    .equals("org.springframework.web.bind.annotation.PathVariable")
                                                    || annotation.getRawType().getName()
                                                    .equals(CurrentUserId.class.getName()));
                                    if (!annotated) {
                                        events.add(SimpleConditionEvent.violated(item,
                                                item.getName() + " has an unannotated UUID parameter"));
                                    }
                                }
                            }
                        }
                    }
                }).allowEmptyShould(true);
    }
}
