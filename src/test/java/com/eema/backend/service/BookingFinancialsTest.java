package com.eema.backend.service;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingFinancialsTest {
    private final BookingFinancials financials = new BookingFinancials();

    @Test
    void calculatesCommissionAndActualFeePayout() {
        BigDecimal commission = financials.commission(new BigDecimal("100.00"), new BigDecimal("20.00"));
        assertEquals(new BigDecimal("20.00"), commission);
        assertEquals(new BigDecimal("79.51"),
                financials.ownerPayout(new BigDecimal("100.00"), commission, new BigDecimal("0.49")));
    }

    @Test
    void roundsMoneyExplicitlyAndEnforcesInvariants() {
        assertEquals(new BigDecimal("12.35"), financials.money(new BigDecimal("12.345")));
        assertThrows(IllegalArgumentException.class,
                () -> financials.commission(BigDecimal.ONE, new BigDecimal("100.01")));
        assertThrows(IllegalArgumentException.class,
                () -> financials.ownerPayout(BigDecimal.TEN, new BigDecimal("9.00"), new BigDecimal("2.00")));
    }
}
