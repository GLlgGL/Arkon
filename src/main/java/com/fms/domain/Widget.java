package com.fms.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard widgets. Each needs view access to one of its areas; users only
 * see (and can only add) widgets they're allowed to use. Texts: widget.*.
 */
public enum Widget {
    QUICK_ACTIONS(Area.SALES, Area.PURCHASES, Area.OFFERS),
    SALES_TODAY(Area.SALES),
    SALES_MONTH(Area.SALES),
    SALES_TREND(Area.STATISTICS),
    TOP_PRODUCTS(Area.STATISTICS),
    RECENT_SALES(Area.SALES),
    UNPAID_CLIENTS(Area.DEBTS, Area.SALES),
    UNPAID_MERCHANTS(Area.DEBTS),
    OPEN_OFFERS(Area.OFFERS),
    RETURNS_MONTH(Area.SALES),
    RESTOCK(Area.STOCK, Area.REPORTS),
    OPEN_PURCHASE_ORDERS(Area.PURCHASE_ORDERS),
    CASH_BALANCES(Area.CASH),
    TAX_OPEN(Area.TAXES);

    private final Area[] areas;

    Widget(Area... areas) {
        this.areas = areas;
    }

    public Area[] getAreas() { return areas; }

    /** Default dashboard per role. */
    public static List<Widget> defaults(AppUser u) {
        List<Widget> l = new ArrayList<>();
        if (u.isAccountant()) {
            l.addAll(List.of(SALES_MONTH, UNPAID_CLIENTS, UNPAID_MERCHANTS, CASH_BALANCES, TAX_OPEN, SALES_TREND, TOP_PRODUCTS, RETURNS_MONTH));
        } else if (u.isAdmin()) {
            l.addAll(List.of(QUICK_ACTIONS, SALES_TODAY, SALES_MONTH, UNPAID_CLIENTS, OPEN_OFFERS, RESTOCK, SALES_TREND,
                    RECENT_SALES, TOP_PRODUCTS, CASH_BALANCES, TAX_OPEN));
        } else {
            l.addAll(List.of(QUICK_ACTIONS, SALES_TODAY, SALES_MONTH, OPEN_OFFERS, RECENT_SALES, UNPAID_CLIENTS));
        }
        return l;
    }
}
