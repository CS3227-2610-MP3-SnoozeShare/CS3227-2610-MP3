package com.snoozeshare.ai.guard;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

/** E6: personal data becomes placeholders, in a fixed order, in linear time. */
class PiiRedactorTest {

    @Test
    void replacesEachKind() {
        assertThat(PiiRedactor.redact("mail a@b.com")).isEqualTo("mail [email]");
        assertThat(PiiRedactor.redact("id S1234567A ok")).isEqualTo("id [id-number] ok");
        assertThat(PiiRedactor.redact("call 91234567 now")).isEqualTo("call [phone] now");
        assertThat(PiiRedactor.redact("call +65 9123 4567 now")).isEqualTo("call [phone] now");
        assertThat(PiiRedactor.redact("card 4111 1111 1111 1111.")).isEqualTo("card [card].");
    }

    @Test
    void cardWinsOverPhone() {
        String out = PiiRedactor.redact("6011 1111 1111 1111");
        assertThat(out).isEqualTo("[card]");
        assertThat(out).doesNotContainPattern("\\d{4}");
    }

    @Test
    void leavesShortNumbersAndPlainTextAlone() {
        assertThat(PiiRedactor.redact("room 12, 3 nights, 250 dollars")).isEqualTo("room 12, 3 nights, 250 dollars");
    }

    @Test
    void noDigitRunOfFourSurvivesInMixedText() {
        String out = PiiRedactor.redact("a@b.com S1234567A 91234567 | 4111 1111 1111 1111 | 6011 1111 1111 1111");
        assertThat(out).doesNotContainPattern("\\d{4}");
    }

    @Test
    void pathologicalInputIsFast() {
        String hostile = "a@a@a@".repeat(100_000 / 6 + 1).substring(0, 100_000);
        PiiRedactor.redact(hostile);
        long start = System.nanoTime();
        PiiRedactor.redact(hostile);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(200));
    }
}
