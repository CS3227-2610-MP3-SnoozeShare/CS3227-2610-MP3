package com.snoozeshare.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import com.snoozeshare.ai.config.LlmProperties;
import com.snoozeshare.domain.enums.Role;

/** E10 (bad registrations fail startup naming id and field) and E11 (duplicates fail, none is fine). */
class AiFeatureRegistryTest {

    private record Reply(String text) { }

    @EnableConfigurationProperties(LlmProperties.class)
    static class Base {
        @Bean
        AiFeatureRegistry registry(ObjectProvider<AiFeature<?>> beans, LlmProperties properties) {
            return new AiFeatureRegistry(beans, properties);
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Base.class);

    static AiFeature<String> feature(String id) {
        return new AiFeature<>(id, Set.of(Role.GUEST), ModelTier.FAST, "You help.", 200, 0.2, 1, 10,
            OutputSpec.text(500, false), call -> "fallback", "mock");
    }

    private static String chain(Throwable error) {
        StringBuilder text = new StringBuilder();
        for (Throwable t = error; t != null; t = t.getCause()) {
            text.append(t.getMessage()).append('\n');
        }
        return text.toString();
    }

    private void assertFails(AiFeature<?> bean, String id, String field) {
        runner.withBean("feature", AiFeature.class, () -> bean).run((AssertableApplicationContext context) -> {
            assertThat(context).hasFailed();
            assertThat(chain(context.getStartupFailure())).contains(id).contains(field);
        });
    }

    @Test
    void noFeaturesStartsEmpty() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(AiFeatureRegistry.class).all()).isEmpty();
        });
    }

    @Test
    void validFeatureIsRegistered() {
        runner.withBean("feature", AiFeature.class, () -> feature("guest.helper")).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(AiFeatureRegistry.class).find("guest.helper")).isPresent();
        });
    }

    @Test
    void duplicateIdFailsStartup() {
        runner.withBean("first", AiFeature.class, () -> feature("guest.helper"))
            .withBean("second", AiFeature.class, () -> feature("guest.helper"))
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(chain(context.getStartupFailure())).contains("guest.helper").contains("twice");
            });
    }

    @Test
    void emptyRoleSetFails() {
        AiFeature<String> f = new AiFeature<>("guest.helper", Set.of(), ModelTier.FAST, "p", 200, 0.2, 1, 10,
            OutputSpec.text(500, false), c -> "x", "m");
        assertFails(f, "guest.helper", "allowedRoles");
    }

    @Test
    void systemRoleFails() {
        AiFeature<String> f = new AiFeature<>("guest.helper", Set.of(Role.GUEST, Role.SYSTEM), ModelTier.FAST, "p",
            200, 0.2, 1, 10, OutputSpec.text(500, false), c -> "x", "m");
        assertFails(f, "guest.helper", "allowedRoles");
    }

    @Test
    void badIdFails() {
        assertFails(feature("Bad_Id"), "Bad_Id", "id");
        assertFails(feature("noDotHere"), "noDotHere", "id");
        assertFails(feature("a.b".repeat(20)), "id", "id");
    }

    @Test
    void zeroMaxTokensFails() {
        AiFeature<String> f = new AiFeature<>("guest.helper", Set.of(Role.GUEST), ModelTier.FAST, "p", 0, 0.2, 1, 10,
            OutputSpec.text(500, false), c -> "x", "m");
        assertFails(f, "guest.helper", "maxTokens");
    }

    @Test
    void nonRecordJsonClassFails() {
        AiFeature<Object> f = new AiFeature<>("guest.helper", Set.of(Role.GUEST), ModelTier.FAST, "p", 200, 0.2, 1,
            10, OutputSpec.json(Object.class, false), c -> new Object(), "{}");
        assertFails(f, "guest.helper", "output");
    }

    @Test
    void recordJsonClassPasses() {
        AiFeature<Reply> f = new AiFeature<>("guest.helper", Set.of(Role.GUEST), ModelTier.FAST, "p", 200, 0.2, 1,
            10, OutputSpec.json(Reply.class, false), c -> new Reply("x"), "{\"text\":\"x\"}");
        runner.withBean("feature", AiFeature.class, () -> f)
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void promptBudgetAboveMaxPromptCharsFails() {
        AiFeature<String> f = new AiFeature<>("guest.helper", Set.of(Role.GUEST), ModelTier.FAST, "p", 200, 0.2, 1,
            10, 8_000, 30, OutputSpec.text(500, false), c -> "x", "m");
        assertFails(f, "guest.helper", "maxInputChars");
    }

    @Test
    void outputLengthOutOfRangeFails() {
        AiFeature<String> f = new AiFeature<>("guest.helper", Set.of(Role.GUEST), ModelTier.FAST, "p", 200, 0.2, 1,
            10, OutputSpec.text(4_001, false), c -> "x", "m");
        assertFails(f, "guest.helper", "output.maxOutputChars");
    }
}
