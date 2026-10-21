package com.fms.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Period lock: when on, nothing dated on or before the lock date can be
 * added, changed or deleted (orders, payments, Cash & Bank, journal entries,
 * stock adjustments, payroll, tax payments). Used after a month or year has
 * been declared.
 */
@Component
public class PeriodLock {

    private final ConfigService config;

    public PeriodLock(ConfigService config) {
        this.config = config;
    }

    /** The lock date, or null when no period is locked. */
    public LocalDate lockedUntil() {
        var c = config.get();
        return c.isModPeriodLock() ? c.getLockDate() : null;
    }

    public boolean isLocked(LocalDate d) {
        LocalDate l = lockedUntil();
        return l != null && d != null && !d.isAfter(l);
    }

    /** Refuses a change dated in a locked period. */
    public void check(LocalDate d) {
        if (isLocked(d)) {
            throw new OrderException("This date is in a locked period (up to "
                    + lockedUntil().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "). Unlock it in Settings to make changes.");
        }
    }
}
