package com.snoozeshare.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/** Protects the boundary between web controllers and persistence/domain internals. */
class ApiLayerTest {

    @Test
    void controllersFollowTheLayerRules() {
        var controllers = new ClassFileImporter().importPackages("com.snoozeshare.api.controller..");
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
        JavaClass fixture = new ClassFileImporter().importPackages("com.snoozeshare.api.layerfixtures")
                .stream().filter(type -> type.getSimpleName().equals(fixtureName))
                .findFirst().orElseThrow();
        assertThat(rule.evaluate(new ClassFileImporter().importPackages("com.snoozeshare"))
                .getFailureReport().getDetails()).isNotEmpty();
    }

    private static ArchRule noPersistenceDependencies(String packageName) {
        return classes().that().resideInAPackage(packageName)
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..repository..", "..domain.model..", "jakarta.persistence..").allowEmptyShould(true);
    }

    private static ArchRule noMoneyAndAuditDependencies(String packageName) {
        return classes().that().resideInAPackage(packageName)
                .should().dependOnClassesThat().haveFullyQualifiedName("com.snoozeshare.service.impl.LedgerWriter")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("com.snoozeshare.service.WalletService")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("com.snoozeshare.service.AuditService")
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
