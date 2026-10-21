package com.snoozeshare.domain.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class SettlementCalculatorTest {

    // --- split ---

    @Test
    void fullEscrowToHost() {
        SettlementBreakdown result = SettlementCalculator.split(
                new BigDecimal("300.00"), BigDecimal.ZERO);
        assertThat(result.escrow()).isEqualByComparingTo("300.00");
        assertThat(result.guestRefund()).isEqualByComparingTo("0.00");
        assertThat(result.hostGross()).isEqualByComparingTo("300.00");
        assertThat(result.fee()).isEqualByComparingTo("9.00");
        assertThat(result.hostNet()).isEqualByComparingTo("291.00");
    }

    @Test
    void fullRefundToGuest() {
        SettlementBreakdown result = SettlementCalculator.split(
                new BigDecimal("300.00"), new BigDecimal("300.00"));
        assertThat(result.guestRefund()).isEqualByComparingTo("300.00");
        assertThat(result.hostGross()).isEqualByComparingTo("0.00");
        assertThat(result.fee()).isEqualByComparingTo("0.00");
        assertThat(result.hostNet()).isEqualByComparingTo("0.00");
    }

    @Test
    void partialRefund() {
        SettlementBreakdown result = SettlementCalculator.split(
                new BigDecimal("200.00"), new BigDecimal("50.00"));
        assertThat(result.hostGross()).isEqualByComparingTo("150.00");
        assertThat(result.fee()).isEqualByComparingTo("4.50");
        assertThat(result.hostNet()).isEqualByComparingTo("145.50");
    }

    @Test
    void roundingHalfUp() {
        // 100.00 * 0.03 = 3.00 (exact), but test a value that needs rounding
        // hostGross = 33.33, fee = 33.33 * 0.03 = 0.9999 -> 1.00
        SettlementBreakdown result = SettlementCalculator.split(
                new BigDecimal("100.00"), new BigDecimal("66.67"));
        assertThat(result.hostGross()).isEqualByComparingTo("33.33");
        assertThat(result.fee()).isEqualByComparingTo("1.00");
        assertThat(result.hostNet()).isEqualByComparingTo("32.33");
    }

    @Test
    void negativeEscrowRejected() {
        assertThatThrownBy(() -> SettlementCalculator.split(
                new BigDecimal("-1.00"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroEscrowRejected() {
        assertThatThrownBy(() -> SettlementCalculator.split(
                BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeRefundRejected() {
        assertThatThrownBy(() -> SettlementCalculator.split(
                new BigDecimal("100.00"), new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refundExceedingEscrowRejected() {
        assertThatThrownBy(() -> SettlementCalculator.split(
                new BigDecimal("100.00"), new BigDecimal("100.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refundWithMoreThanTwoDecimalsRejected() {
        assertThatThrownBy(() -> SettlementCalculator.split(
                new BigDecimal("100.00"), new BigDecimal("10.123")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- platformFee ---

    @Test
    void platformFeeIsThreePercent() {
        assertThat(SettlementCalculator.platformFee(new BigDecimal("100.00")))
                .isEqualByComparingTo("3.00");
    }

    // --- hostNet ---

    @Test
    void hostNetIsGrossMinusFee() {
        assertThat(SettlementCalculator.hostNet(new BigDecimal("100.00")))
                .isEqualByComparingTo("97.00");
    }
}
