package com.fms.domain;

import java.util.EnumMap;
import java.util.Map;

/**
 * Areas of IMS that access can be given to: no access, view only, or full.
 * Each role has defaults; an administrator can change them per user.
 * Administration (users, settings, backup, audit, registers) stays for administrators.
 */
public enum Area {
    // ---- main areas
    PURCHASES, SALES, OFFERS, INVOICES, PURCHASE_INVOICES, PURCHASE_ORDERS,
    REPORTS, STATISTICS, DEBTS, STOCK, CASH, COST_CENTERS, PAYROLL, ASSETS, PRODUCTION, TAXES, ACCOUNTING, BUDGETS,

    // ---- sub-areas: they follow their main area unless the user has a level of their own for them
    SALES_POS(SALES), SALES_RETURNS(SALES), SALES_PAYMENTS(SALES), OFFERS_CONVERT(OFFERS),
    PURCHASE_RETURNS(PURCHASES), PURCHASE_PAYMENTS(PURCHASES),
    DEBTS_STATEMENTS(DEBTS), DEBTS_REMINDERS(DEBTS),
    STOCK_ADJUST(STOCK), STOCK_COUNT(STOCK), STOCK_TRANSFERS(STOCK), STOCK_LOCATIONS(STOCK), STOCK_TRACKING(STOCK), STOCK_SHELVES(STOCK),
    CASH_ENTRIES(CASH), CASH_RECONCILE(CASH), CASH_ACCOUNTS(CASH),
    PAYROLL_EMPLOYEES(PAYROLL), PAYROLL_PAY(PAYROLL), PAYROLL_SETTINGS(PAYROLL),
    ACC_STATEMENTS(ACCOUNTING), ACC_VAT(ACCOUNTING), ACC_CHART(ACCOUNTING), ACC_MANUAL(ACCOUNTING), ACC_CLOSING(ACCOUNTING),
    BUDGET_PLAN(BUDGETS), PRODUCTION_BOM(PRODUCTION);

    private final Area parent;

    Area() { this(null); }
    Area(Area parent) { this.parent = parent; }

    /** The main area of a sub-area (null for a main area). */
    public Area getParent() { return parent; }
    public boolean isSub() { return parent != null; }

    public enum Level { NONE, VIEW, FULL;
        public boolean atLeast(Level l) { return ordinal() >= l.ordinal(); }
    }

    /** Defaults per role (administrators always have full access). Sub-areas: as their main area. */
    public static Map<Area, Level> defaults(AppUser u) {
        Map<Area, Level> m = new EnumMap<>(Area.class);
        for (Area a : values()) {
            if (a.isSub()) continue;
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
        for (Area a : values()) {
            if (a.isSub()) m.put(a, m.get(a.parent));
        }
        return m;
    }

    /** Pages of the sub-areas (checked first): regular expressions on the address. */
    private static final String[][] SUB = {
        {"^/pos(/.*)?$", "SALES_POS"},
        {"^/sales/returns$", "SALES_RETURNS"}, {"^/sales/\\d+/return$", "SALES_RETURNS"},
        {"^/sales/\\d+/payments(/.*)?$", "SALES_PAYMENTS"},
        {"^/offers/\\d+/convert$", "OFFERS_CONVERT"},
        {"^/purchases/returns$", "PURCHASE_RETURNS"}, {"^/purchases/\\d+/return$", "PURCHASE_RETURNS"},
        {"^/purchases/\\d+/payments(/.*)?$", "PURCHASE_PAYMENTS"},
        {"^/statements(/.*)?$", "DEBTS_STATEMENTS"}, {"^/reminders(/.*)?$", "DEBTS_REMINDERS"},
        {"^/stock-count(/.*)?$", "STOCK_COUNT"}, {"^/stock(/.*)?$", "STOCK_ADJUST"},
        {"^/locations/transfers(/.*)?$", "STOCK_TRANSFERS"}, {"^/locations$", "STOCK_LOCATIONS"},
        {"^/tracking(/.*)?$", "STOCK_TRACKING"}, {"^/shelves(/.*)?$", "STOCK_SHELVES"},
        {"^/cash/(entry|transfer|import)(/.*)?$", "CASH_ENTRIES"}, {"^/cash/movements/[^/]+/delete$", "CASH_ENTRIES"},
        {"^/cash/reconcile(/.*)?$", "CASH_RECONCILE"}, {"^/cash/accounts(/.*)?$", "CASH_ACCOUNTS"},
        {"^/payroll/employees(/.*)?$", "PAYROLL_EMPLOYEES"}, {"^/payroll/gross-from-net$", "PAYROLL_EMPLOYEES"},
        {"^/payroll/\\d+/(approve|pay)$", "PAYROLL_PAY"}, {"^/payroll/settings(/.*)?$", "PAYROLL_SETTINGS"},
        {"^/accounting/(statements|official|trial)(/.*)?$", "ACC_STATEMENTS"}, {"^/accounting/vat(/.*)?$", "ACC_VAT"},
        {"^/accounting/accounts(/.*)?$", "ACC_CHART"}, {"^/accounting/entry(/.*)?$", "ACC_MANUAL"},
        {"^/accounting/(sync|close-year)$", "ACC_CLOSING"},
        {"^/budgets$", "BUDGET_PLAN"}, {"^/budgets/(fill|save|copy)(/.*)?$", "BUDGET_PLAN"},
        {"^/production/bom(/.*)?$", "PRODUCTION_BOM"}};
    private static final java.util.List<java.util.Map.Entry<java.util.regex.Pattern, Area>> SUB_PATTERNS = new java.util.ArrayList<>();
    static {
        for (String[] e : SUB) SUB_PATTERNS.add(java.util.Map.entry(java.util.regex.Pattern.compile(e[0]), valueOf(e[1])));
    }

    /** Which area a URL belongs to (null = no area rules): a sub-area page first, else its main area. */
    public static Area forPath(String path) {
        for (var e : SUB_PATTERNS) {
            if (e.getKey().matcher(path).matches()) return e.getValue();
        }
        String[][] map = {
            {"/invoices/purchases", "PURCHASE_INVOICES"}, {"/invoices", "INVOICES"},
            {"/purchase-orders", "PURCHASE_ORDERS"}, {"/purchases", "PURCHASES"}, {"/sales", "SALES"}, {"/offers", "OFFERS"},
            {"/reports", "REPORTS"}, {"/statistics", "STATISTICS"}, {"/debts", "DEBTS"},
            {"/stock", "STOCK"}, {"/shelves", "STOCK"}, {"/labels", "STOCK"}, {"/cash", "CASH"}, {"/cost-centers", "COST_CENTERS"},
            {"/payroll", "PAYROLL"}, {"/assets", "ASSETS"}, {"/production", "PRODUCTION"}, {"/taxes", "TAXES"},
            {"/accounting", "ACCOUNTING"}, {"/statements", "DEBTS"}, {"/reminders", "DEBTS"}, {"/stock-count", "STOCK"}, {"/budgets", "BUDGETS"}, {"/pos", "SALES"}, {"/locations", "STOCK"}, {"/tracking", "STOCK"}};
        for (String[] e : map) {
            if (path.equals(e[0]) || path.startsWith(e[0] + "/")) {
                return valueOf(e[1]);
            }
        }
        return null;
    }
}
