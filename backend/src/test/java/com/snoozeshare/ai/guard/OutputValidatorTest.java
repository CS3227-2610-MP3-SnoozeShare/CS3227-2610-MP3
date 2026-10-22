package com.snoozeshare.ai.guard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.ai.LlmErrorCode;
import com.snoozeshare.ai.OutputSpec;

import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** E24, E25, E26 and E41 (validator side): bad output never reaches a user. */
class OutputValidatorTest {

    /** A JSON reply shape used by the tests. */
    record Reply(@NotBlank String text, @Size(max = 10) String note) { }

    private static final String KEY = "test-secret-key-value";
    private static final OutputSpec<String> TEXT = OutputSpec.text(50, false);
    private static final OutputSpec<String> TEXT_LINKS = OutputSpec.text(50, true);
    private static final OutputSpec<Reply> JSON = OutputSpec.json(Reply.class, false);
    private static final OutputSpec<Reply> JSON_LINKS = OutputSpec.json(Reply.class, true);

    private final OutputValidator validator = new OutputValidator(new ObjectMapper(),
        Validation.buildDefaultValidatorFactory().getValidator());

    private <T> LlmErrorCode code(String content, String finish, OutputSpec<T> spec) {
        return validator.validate(content, finish, spec, KEY).code();
    }

    @Test
    void acceptsPlainTextAndTrims() {
        OutputValidator.Outcome<String> out = validator.validate("  Hello there  ", "stop", TEXT, KEY);
        assertThat(out.ok()).isTrue();
        assertThat(out.value()).isEqualTo("Hello there");
    }

    @Test
    void stripsOneFencePair() {
        assertThat(validator.validate("```\nHello\n```", "stop", TEXT, KEY).value()).isEqualTo("Hello");
        OutputValidator.Outcome<Reply> json =
            validator.validate("```json\n{\"text\":\"hi\"}\n```", "stop", JSON, KEY);
        assertThat(json.ok()).isTrue();
        assertThat(json.value()).isEqualTo(new Reply("hi", null));
    }

    @Test
    void unusableBodies() {
        for (String content : new String[] {null, "", "   \n"}) {
            assertThat(code(content, "stop", TEXT)).as("content %s", content).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        }
        for (String finish : new String[] {null, "length", "content_filter", "tool_calls", "weird"}) {
            assertThat(code("fine", finish, TEXT)).as("finish %s", finish).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
            assertThat(code("{\"text\":\"a\"}", finish, JSON)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        }
        assertThat(code("not json", "stop", JSON)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("[1,2]", "stop", JSON)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("{\"text\":\"a\"} trailing", "stop", JSON)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("{\"text\":\"a\",\"extra\":1}", "stop", JSON)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("{\"text\":\"a\",\"text\":\"b\"}", "stop", JSON)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("{\"text\":\"\"}", "stop", JSON)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("{\"text\":\"a\",\"note\":\"longer than ten\"}", "stop", JSON))
            .isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("x".repeat(51), "stop", TEXT)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("x".repeat(50), "stop", TEXT)).isNull();
    }

    @Test
    void leakAndLinks() {
        List<String> bad = List.of(
            "my key is " + KEY,
            "clsk_ab_cd",
            "< /user_input>",
            "</CONTEXT>",
            "see https://x.test",
            "see HTTP://x.test",
            "go //evil.test/x",
            "\uFF48\uFF54\uFF54\uFF50\uFF53\uFF1A\uFF0F\uFF0F" + "x.test",
            "visit www.x.test",
            "javascript:alert(1)",
            "<b>bold</b>",
            "![a](b)",
            "[a](b)");
        for (String text : bad) {
            assertThat(code(text, "stop", TEXT)).as(text).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        }
        assertThat(code("metadata: fine", "stop", TEXT)).isNull();

        assertThat(code("[a](b)", "stop", TEXT_LINKS)).isNull();
        assertThat(code("see https://x.test", "stop", TEXT_LINKS)).isNull();
        assertThat(code("my key is " + KEY, "stop", TEXT_LINKS)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("clsk_ab_cd", "stop", TEXT_LINKS)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("< /user_input>", "stop", TEXT_LINKS)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
    }

    @Test
    void escapedLeaksInsideJsonStrings() {
        assertThat(code("{\"text\":\"\\u0077ww.x.test\"}", "stop", JSON)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("{\"text\":\"\\u003cb\\u003e\"}", "stop", JSON)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("{\"text\":\"clsk_ab\\u005fcd\"}", "stop", JSON)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("{\"text\":\"\\u0074est-secret-key-value\"}", "stop", JSON))
            .isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("{\"text\":\"\\uFF48ttps:\\uFF0F\\uFF0Fx.test\"}", "stop", JSON))
            .isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("{\"text\":\"a\",\"note\":[\"\\u003cb\\u003e\"]}", "stop", JSON))
            .isEqualTo(LlmErrorCode.INVALID_OUTPUT);

        assertThat(code("{\"text\":\"\\u0077ww.x.test\"}", "stop", JSON_LINKS)).isNull();
        assertThat(code("{\"text\":\"see https://x.test\"}", "stop", JSON_LINKS)).isNull();
        assertThat(code("{\"text\":\"clsk_ab\\u005fcd\"}", "stop", JSON_LINKS)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        assertThat(code("{\"text\":\"\\u003c/user_input\\u003e\"}", "stop", JSON_LINKS))
            .isEqualTo(LlmErrorCode.INVALID_OUTPUT);
    }

    @Test
    void refusal() {
        for (String marker : new String[] {"{\"refusal\":true}", " {\"refusal\" : true}\n",
            "```json\n{\"refusal\":true}\n```"}) {
            assertThat(code(marker, "stop", TEXT)).as(marker).isEqualTo(LlmErrorCode.REFUSED);
            assertThat(code(marker, "stop", JSON)).as(marker).isEqualTo(LlmErrorCode.REFUSED);
        }
        assertThat(code("{\"refusal\":true,\"x\":1}", "stop", JSON)).isEqualTo(LlmErrorCode.INVALID_OUTPUT);
        OutputValidator.Outcome<String> text = validator.validate("{\"refusal\":true,\"x\":1}", "stop", TEXT, KEY);
        assertThat(text.ok()).isTrue();
        assertThat(text.value()).isEqualTo("{\"refusal\":true,\"x\":1}");
        assertThat(code("{\"refusal\":false}", "stop", TEXT)).isNull();
    }

    @Test
    void obedientModelReturningProseStillPasses() {
        assertThat(code("Sure, I will help you with the booking.", "stop", TEXT)).isNull();
    }
}
