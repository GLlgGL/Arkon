package com.fms.repo;

import java.math.BigDecimal;

/** Report header figures for a period. */
public record Totals(Long orders, Long quantity, BigDecimal valueNoVat, BigDecimal vatValue, BigDecimal valueWithVat) {

    public static Totals empty() {
        return new Totals(0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** Aggregates over zero rows come back as nulls; normalise them to zero. */
    public Totals orZero() {
        return new Totals(
                orders == null ? 0L : orders,
                quantity == null ? 0L : quantity,
                valueNoVat == null ? BigDecimal.ZERO : valueNoVat,
                vatValue == null ? BigDecimal.ZERO : vatValue,
                valueWithVat == null ? BigDecimal.ZERO : valueWithVat);
    }
}
