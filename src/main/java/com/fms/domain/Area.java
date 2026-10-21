package com.fms.domain;

import java.util.EnumMap;
import java.util.Map;

/**
 * Areas of IMS that access can be given to: no access, view only, or full.
 * Each role has defaults; an administrator can change them per user.
 * Administration (users, settings, backup, audit, registers) stays for administrators.
 */
public enum Area {
    PURCHASES, SALES, OFFERS, INVOICES, PURCHASE_INVOICES, PURCHASE_ORDERS,
    REPORTS, STATISTICS, DEBTS, STOCK, CASH, COST_CENTERS, PAYROLL, ASSETS, PRODUCTION, TAXES, ACCOUNTING, BUDGETS;

    public enum Level { NONE, VIEW, FULL;
        public boolean atLeast(Level l) { return ordinal() >= l.ordinal(); }
    }

    /** Defaults per role (administrators always have full access). */
    public static Map<Area, Level> defaults(AppUser u) {
        Map<Area, Level> m = new EnumMap<>(Area.class);
        for (Area a : values()) {
            m.put(a, u.isAdmin() ? Level.FULL : u.isAccountant() ? Level.VIEW : Level.NONE);
        }
        if (u.isAccountant()) {
            m.put(ACCOUNTING, Level.FULL);              // bookkeeping is the accountant's job
        }
        if (!u.isAdmin() && !u.isAccountant()) {      // regular user: sells, buys, makes offers
            m.put(PURCHASES, Level.FULL);
            m.put(SALES, Level.FULL);
            m.put(OFFERS, Level.FULL);
            m.put(INVOICES, Level.FULL);
        }
        return m;
    }

    /** Which area a URL belongs to (null = no area rules). Longest prefixes first. */
    public static Area forPath(String path) {
        String[][] map = {
            {"/invoices/purchases", "PURCHASE_INVOICES"}, {"/invoices", "INVOICES"},
            {"/purchase-orders", "PURCHASE_ORDERS"}, {"/purchases", "PURCHASES"}, {"/sales", "SALES"}, {"/offers", "OFFERS"},
            {"/reports", "REPORTS"}, {"/statistics", "STATISTICS"}, {"/debts", "DEBTS"},
            {"/stock", "STOCK"}, {"/shelves", "STOCK"}, {"/cash", "CASH"}, {"/cost-centers", "COST_CENTERS"},
            {"/payroll", "PAYROLL"}, {"/assets", "ASSETS"}, {"/production", "PRODUCTION"}, {"/taxes", "TAXES"},
            {"/accounting", "ACCOUNTING"}, {"/statements", "DEBTS"}, {"/reminders", "DEBTS"}, {"/stock-count", "STOCK"}, {"/budgets", "BUDGETS"}, {"/pos", "SALES"}};
        for (String[] e : map) {
            if (path.equals(e[0]) || path.startsWith(e[0] + "/")) {
                return valueOf(e[1]);
            }
        }
        return null;
    }
}
