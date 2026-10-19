package com.fms.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A date range, inclusive of both days. Used by reports and statistics. */
public record Period(String key, String label, LocalDate from, LocalDate to) {

    public LocalDateTime start() {
        return from.atStartOfDay();
    }

    /** Exclusive upper bound for queries. */
    public LocalDateTime end() {
        return to.plusDays(1).atStartOfDay();
    }

    public static Period today() {
        LocalDate d = LocalDate.now();
        return new Period("today", "Today", d, d);
    }

    public static Period thisMonth() {
        LocalDate d = LocalDate.now();
        return new Period("month", "This month", d.withDayOfMonth(1), d.withDayOfMonth(d.lengthOfMonth()));
    }

    public static Period thisYear() {
        LocalDate d = LocalDate.now();
        return new Period("year", "This year", d.withDayOfYear(1), d.withDayOfYear(d.lengthOfYear()));
    }

    public static Period custom(LocalDate from, LocalDate to) {
        LocalDate f = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        LocalDate t = to == null ? LocalDate.now() : to;
        if (t.isBefore(f)) {
            LocalDate x = f;
            f = t;
            t = x;
        }
        return new Period("custom", "Custom range", f, t);
    }

    public static Period of(String key, LocalDate from, LocalDate to) {
        if (key == null) {
            return thisMonth();
        }
        return switch (key) {
            case "today" -> today();
            case "year" -> thisYear();
            case "custom" -> custom(from, to);
            default -> thisMonth();
        };
    }
}
