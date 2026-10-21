package com.snoozeshare.domain.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DomainValidationTest {

    // --- requireText ---

    @Test
    void requireTextReturnsValue() {
        assertThat(DomainValidation.requireText("hello", "name")).isEqualTo("hello");
    }

    @Test
    void requireTextRejectsNull() {
        assertThatThrownBy(() -> DomainValidation.requireText(null, "name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name");
    }

    @Test
    void requireTextRejectsBlank() {
        assertThatThrownBy(() -> DomainValidation.requireText("  ", "name"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- requireNonNegative ---

    @Test
    void requireNonNegativeAcceptsZero() {
        assertThat(DomainValidation.requireNonNegative(BigDecimal.ZERO, "amount"))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void requireNonNegativeRejectsNegative() {
        assertThatThrownBy(() -> DomainValidation.requireNonNegative(
                new BigDecimal("-1"), "amount"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireNonNegativeRejectsNull() {
        assertThatThrownBy(() -> DomainValidation.requireNonNegative(null, "amount"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- requirePositive ---

    @Test
    void requirePositiveAcceptsPositive() {
        assertThat(DomainValidation.requirePositive(BigDecimal.ONE, "price"))
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void requirePositiveRejectsZero() {
        assertThatThrownBy(() -> DomainValidation.requirePositive(BigDecimal.ZERO, "price"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- requireDateRange ---

    @Test
    void requireDateRangeAcceptsValid() {
        DomainValidation.requireDateRange(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2));
    }

    @Test
    void requireDateRangeRejectsSameDay() {
        assertThatThrownBy(() -> DomainValidation.requireDateRange(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireDateRangeRejectsInverted() {
        assertThatThrownBy(() -> DomainValidation.requireDateRange(
                LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- requireRating ---

    @Test
    void requireRatingAcceptsBoundaries() {
        assertThat(DomainValidation.requireRating(1)).isEqualTo(1);
        assertThat(DomainValidation.requireRating(5)).isEqualTo(5);
    }

    @Test
    void requireRatingRejectsOutOfRange() {
        assertThatThrownBy(() -> DomainValidation.requireRating(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DomainValidation.requireRating(6))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- requireSgd ---

    @Test
    void requireSgdAcceptsSgd() {
        assertThat(DomainValidation.requireSgd("SGD")).isEqualTo("SGD");
    }

    @Test
    void requireSgdRejectsOther() {
        assertThatThrownBy(() -> DomainValidation.requireSgd("USD"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
