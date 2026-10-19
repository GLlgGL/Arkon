package com.fms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Money helpers. The original rounded every amount to a whole number with
 * ToString("#,#") and stored that text; here amounts are kept to 2 decimals.
 */
public final class Money {

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** "1,234.50" in a fixed (invariant) format, as the original did. */
    public static String format(BigDecimal value) {
        DecimalFormat f = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return f.format(nz(value));
    }
}
