package com.snoozeshare.service.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.ai.AiFeature;
import com.snoozeshare.ai.ModelTier;
import com.snoozeshare.ai.config.LlmProperties;
import com.snoozeshare.ai.guard.OutputValidator;
import com.snoozeshare.domain.enums.Role;

import jakarta.validation.Validation;

/** E79/E80 JSON schema and feature registration, through the real gateway validator. */
class ClarifyFeatureTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final OutputValidator validator = new OutputValidator(mapper,
        Validation.buildDefaultValidatorFactory().getValidator());
    private final AiFeature<ClarifyOutput> feature = new ClarifyFeatureConfiguration().clarifyFeature();

    @EnableConfigurationProperties(LlmProperties.class)
    static class GatewaySettings { }

    @Test
    void promptPermitsOnlyServerExpandedSupportingDocumentMarkerP13E89() {
        assertThat(feature.systemPrompt()).contains("[supporting-documents]", "enough", "false",
            "another question round", "Never emit an address", "ticket id", "subject", "alternative recipient");
    }

    @Test
    void registrationMockAndPromptBoundE80() {
        new ApplicationContextRunner().withUserConfiguration(GatewaySettings.class).run(context -> {
            LlmProperties settings = context.getBean(LlmProperties.class);
            feature.validate(settings);
            assertThat(feature.maxInputChars() * (feature.maxTurns() + 1) + 15000)
                .isEqualTo(37000).isLessThanOrEqualTo(settings.maxPromptChars());
        });
        assertThat(feature.id()).isEqualTo("ticket.clarify");
        assertThat(feature.allowedRoles()).isEqualTo(Set.of(Role.GUEST));
        assertThat(feature.tier()).isEqualTo(ModelTier.STANDARD);
        assertThat(feature.maxTokens()).isEqualTo(2500);
        assertThat(feature.temperature()).isEqualTo(0.2);
        assertThat(feature.priority()).isEqualTo(1);
        assertThat(feature.perUserPerMinute()).isEqualTo(6);
        assertThat(feature.output().allowLinks()).isFalse();
        assertThat(feature.systemPrompt()).hasSizeLessThanOrEqualTo(6000)
            .contains("<user_input>", "<untrusted_data>", "English", "third person", "person");
        assertThat(validator.validate(feature.mockResponse(), "stop", feature.output(), "").ok()).isTrue();
        assertThat(feature.fallback().apply(null)).isEqualTo(ClarifyOutput.NONE);
    }

    @Test
    void disabledAndDatabaseFreeContextsHaveNoFeatureE80() {
        ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ClarifyFeatureConfiguration.class);
        runner.run(context -> assertThat(context).hasSingleBean(AiFeature.class)
            .hasSingleBean(AssistantProperties.class));
        runner.withPropertyValues("snoozeshare.assistant.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(AiFeature.class));
        runner.withPropertyValues("spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc."
            + "DataSourceAutoConfiguration").run(context -> assertThat(context).doesNotHaveBean(AiFeature.class));
    }

    @Test
    void everyListAndStringBoundaryIsValidatedE79() throws Exception {
        Map<String, int[]> caps = Map.of("questions", new int[]{3, 300}, "timeline", new int[]{6, 150},
            "claims", new int[]{6, 200}, "evidence", new int[]{4, 150}, "missing", new int[]{5, 150},
            "hostPoints", new int[]{4, 200});
        for (Map.Entry<String, int[]> cap : caps.entrySet()) {
            var tree = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(feature.mockResponse());
            tree.withArray(cap.getKey()).removeAll();
            for (int i = 0; i < cap.getValue()[0]; i++) {
                tree.withArray(cap.getKey()).add("a".repeat(cap.getValue()[1]));
            }
            assertValid(tree.toString(), true);
            tree.withArray(cap.getKey()).add("extra");
            assertValid(tree.toString(), false);
            tree.withArray(cap.getKey()).removeAll().add("a".repeat(cap.getValue()[1] + 1));
            assertValid(tree.toString(), false);
            tree.withArray(cap.getKey()).removeAll().add("");
            assertValid(tree.toString(), false);
            tree.withArray(cap.getKey()).removeAll().addNull();
            assertValid(tree.toString(), false);
            tree.putNull(cap.getKey());
            assertValid(tree.toString(), false);
        }
        var tree = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(feature.mockResponse());
        tree.put("summary", "s".repeat(500));
        assertValid(tree.toString(), true);
        tree.put("summary", "s".repeat(501));
        assertValid(tree.toString(), false);
        tree.put("summary", "");
        assertValid(tree.toString(), false);
        tree.putNull("summary");
        assertValid(tree.toString(), false);
    }

    @Test
    void missingFieldsAndUnknownFieldsFailButWhitespaceQuestionsPassSchemaE79() throws Exception {
        for (String field : List.of("enough", "questions", "summary", "timeline", "claims", "evidence",
                "missing", "hostPoints")) {
            var tree = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(feature.mockResponse());
            tree.remove(field);
            assertValid(tree.toString(), false);
        }
        var tree = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(feature.mockResponse());
        tree.put("unknown", true);
        assertValid(tree.toString(), false);
        tree.remove("unknown");
        tree.withArray("questions").removeAll().add(" ");
        assertValid(tree.toString(), true);
    }

    private void assertValid(String json, boolean expected) {
        assertThat(validator.validate(json, "stop", feature.output(), "").ok()).as(json).isEqualTo(expected);
    }

    /** The tuning kit's selected v3 file is the committed prompt; R15 requires them to stay identical. */
    @Test
    void committedPromptMatchesTheSelectedTuningKitFile() throws IOException {
        Path file = Path.of("..", "tools", "prompt-tuning", "ticket-clarify", "prompts",
            "v3-snoozy-examples.txt");
        assumeTrue(Files.exists(file));
        String kit = Files.readString(file).replace("\r\n", "\n").trim();
        assertThat(ClarifyPrompt.TEXT.trim()).isEqualTo(kit);
        assertThat(kit).contains("Set `enough` to false while another question round is available.");
        assertThat(kit).contains("never put other text around the marker.");
    }
}
