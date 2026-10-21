package com.fms.service;

import java.util.function.Supplier;

/**
 * The location a stock operation happens at (Locations module). Set while a
 * sale, purchase, adjustment or count is processed; stock checks and new
 * lines and adjustments use it. Null = not set (main location / no module).
 */
public final class StockLocation {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private StockLocation() {
    }

    public static Long get() {
        return CURRENT.get();
    }

    /** Runs work at a location, then restores the previous one. */
    public static <T> T at(Long locationId, Supplier<T> work) {
        Long before = CURRENT.get();
        CURRENT.set(locationId);
        try {
            return work.get();
        } finally {
            if (before == null) CURRENT.remove(); else CURRENT.set(before);
        }
    }

    public static void run(Long locationId, Runnable work) {
        at(locationId, () -> { work.run(); return null; });
    }
}
