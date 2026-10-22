package com.fms.web;

import com.fms.domain.AppConfig;
import com.fms.domain.AppUser;
import com.fms.service.AccessService;
import com.fms.service.ConfigService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The menu: a few top sections, each with a row of tab buttons for its pages
 * (shown at the top of every page of the section). Only pages the user may open
 * and modules that are switched on are listed. Sections are ordered by daily use.
 */
@Component("nav")
public class NavService {

    /** A page of a section: label key and address (without the context path). */
    public record Tab(String key, String url) {
        String path() { int q = url.indexOf('?'); return q < 0 ? url : url.substring(0, q); }
    }

    public record Section(String key, List<Tab> tabs) {
        public String url() { return tabs.get(0).url(); }
    }

    /** Where a page is: its section and tab (both may be null, e.g. on the dashboard). */
    public record Here(Section section, Tab tab) { }

    private final AccessService perm;
    private final ConfigService config;

    public NavService(AccessService perm, ConfigService config) {
        this.perm = perm;
        this.config = config;
    }

    private static final class B {
        final String key; final List<Tab> tabs = new ArrayList<>();
        B(String key) { this.key = key; }
        B tab(boolean show, String key, String url) { if (show) tabs.add(new Tab(key, url)); return this; }
    }

    public List<Section> sections(AppUser u) {
        AppConfig c = config.get();
        boolean admin = u != null && u.isAdmin();
        List<B> all = new ArrayList<>();
        all.add(new B("nav.sec.sales")
                .tab(perm.can("SALES"), "nav.new.SALE", "/sales/new")
                .tab(c.isModPos() && perm.edit("SALES"), "pos.title", "/pos")
                .tab(perm.can("SALES"), "nav.list.SALE", "/sales")
                .tab(perm.can("OFFERS"), "nav.new.OFFER", "/offers/new")
                .tab(perm.can("OFFERS"), "nav.list.OFFER", "/offers")
                .tab(perm.can("SALES"), "nav.returns.SALE", "/sales/returns")
                .tab(perm.can("INVOICES"), "nav.invoices.sales", "/invoices/sales")
                .tab(perm.can("INVOICES"), "nav.invoices.offers", "/invoices/offers")
                .tab(perm.can("DEBTS"), "nav.debts.sales", "/debts/sales")
                .tab(perm.can("DEBTS"), "ag.receivable", "/debts/aging/sales")
                .tab(c.isModStatements() && perm.can("DEBTS"), "st.clients", "/statements/clients")
                .tab(c.isModReminders() && perm.can("DEBTS"), "rem.title", "/reminders"));
        all.add(new B("nav.sec.purchases")
                .tab(perm.can("PURCHASES"), "nav.new.PURCHASE", "/purchases/new")
                .tab(perm.can("PURCHASES"), "nav.list.PURCHASE", "/purchases")
                .tab(perm.can("PURCHASES"), "nav.returns.PURCHASE", "/purchases/returns")
                .tab(perm.can("PURCHASE_ORDERS"), "nav.po", "/purchase-orders")
                .tab(perm.can("REPORTS"), "nav.report.supply", "/reports/warehouse?mode=supply")
                .tab(perm.can("PURCHASE_INVOICES"), "nav.invoices.purchases", "/invoices/purchases")
                .tab(perm.can("DEBTS"), "nav.debts.purchases", "/debts/purchases")
                .tab(perm.can("DEBTS"), "ag.payable", "/debts/aging/purchases")
                .tab(c.isModStatements() && perm.can("DEBTS"), "st.suppliers", "/statements/suppliers"));
        all.add(new B("nav.sec.stock")
                .tab(admin, "nav.admin.products", "/admin/products")
                .tab(perm.can("REPORTS"), "nav.report.warehouse", "/reports/warehouse")
                .tab(perm.can("STOCK"), "nav.admin.stock", "/stock")
                .tab(c.isModStockCount() && perm.can("STOCK"), "sc.title", "/stock-count")
                .tab(c.isModLocations() && perm.can("STOCK"), "loc.stockTitle", "/locations/stock")
                .tab(c.isModLocations() && perm.can("STOCK"), "loc.transfers", "/locations/transfers")
                .tab(c.isModLocations() && perm.can("STOCK"), "loc.title", "/locations")
                .tab(c.isModTracking() && perm.can("STOCK"), "trk.expiryTitle", "/tracking/expiry")
                .tab(c.isModTracking() && perm.can("STOCK"), "trk.serialTitle", "/tracking/serials")
                .tab(perm.can("PRODUCTION"), "nav.production", "/production")
                .tab(perm.can("PRODUCTION"), "pd.bom", "/production/bom")
                .tab(c.isUseShelves() && admin, "nav.admin.shelves", "/shelves")
                .tab(admin, "nav.admin.classes", "/classes")
                .tab(c.isModPriceLists() && admin, "pl.title", "/admin/price-lists"));
        all.add(new B("nav.cash")
                .tab(perm.can("CASH"), "cash.tab.overview", "/cash")
                .tab(perm.can("CASH"), "cash.tab.movements", "/cash/movements")
                .tab(perm.edit("CASH"), "cash.newIn", "/cash/entry?dir=IN")
                .tab(perm.edit("CASH"), "cash.newOut", "/cash/entry?dir=OUT")
                .tab(perm.edit("CASH"), "cash.transfer", "/cash/transfer")
                .tab(perm.edit("CASH"), "cash.reconcile", "/cash/reconcile")
                .tab(perm.can("CASH"), "cash.tab.flow", "/cash/flow")
                .tab(perm.can("CASH"), "cash.tab.analytics", "/cash/analytics")
                .tab(perm.can("CASH"), "cash.tab.accounts", "/cash/accounts")
                .tab(c.isModCurrencies() && admin, "fx.title", "/admin/rates")
                .tab(c.isModCurrencies() && perm.can("REPORTS"), "fx.diffTitle", "/reports/fx-differences"));
        all.add(new B("nav.sec.accounting")
                .tab(perm.can("ACCOUNTING"), "acc.tab.journal", "/accounting")
                .tab(perm.can("ACCOUNTING"), "acc.tab.ledger", "/accounting/ledger")
                .tab(perm.can("ACCOUNTING"), "acc.tab.trial", "/accounting/trial")
                .tab(perm.can("ACCOUNTING"), "acc.tab.statements", "/accounting/statements")
                .tab(perm.can("ACCOUNTING"), "acc.tab.official", "/accounting/official")
                .tab(perm.can("ACCOUNTING"), "acc.tab.vat", "/accounting/vat")
                .tab(perm.can("ACCOUNTING"), "acc.tab.accounts", "/accounting/accounts")
                .tab(perm.can("TAXES"), "nav.taxes", "/taxes")
                .tab(perm.can("ASSETS"), "nav.assets", "/assets")
                .tab(perm.can("BUDGETS"), "bud.tab.budget", "/budgets")
                .tab(perm.can("BUDGETS"), "bud.tab.compare", "/budgets/compare")
                .tab(perm.can("BUDGETS"), "bud.tab.forecast", "/budgets/forecast")
                .tab(perm.can("COST_CENTERS"), "nav.ccReport", "/cost-centers/report"));
        all.add(new B("nav.payroll")
                .tab(perm.can("PAYROLL"), "pr.tab.runs", "/payroll")
                .tab(perm.can("PAYROLL"), "pr.tab.employees", "/payroll/employees")
                .tab(perm.edit("PAYROLL"), "pr.tab.settings", "/payroll/settings"));
        all.add(new B("nav.reports")
                .tab(perm.can("STATISTICS"), "nav.statistics", "/statistics")
                .tab(perm.can("REPORTS"), "nav.report.sales", "/reports/sales")
                .tab(perm.can("REPORTS"), "nav.report.purchases", "/reports/purchases")
                .tab(perm.can("REPORTS"), "nav.report.products", "/reports/products"));
        all.add(new B("nav.administration")
                .tab(admin, "nav.admin.clients", "/admin/clients")
                .tab(admin, "nav.admin.merchants", "/admin/merchants")
                .tab(admin, "nav.admin.companies", "/admin/companies")
                .tab(admin, "nav.admin.users", "/admin/users")
                .tab(admin, "nav.admin.settings", "/settings")
                .tab(admin, "nav.admin.email", "/admin/email")
                .tab(admin, "nav.admin.backup", "/admin/backup")
                .tab(admin, "nav.admin.audit", "/admin/audit")
                .tab(c.isModCostCenters() && admin, "nav.cc", "/cost-centers"));
        List<Section> out = new ArrayList<>();
        for (B b : all) if (!b.tabs.isEmpty()) out.add(new Section(b.key, List.copyOf(b.tabs)));
        return out;
    }

    /**
     * The section and tab of a page address (path + query): the tab whose address matches best —
     * the same address, else the longest one the page is under (e.g. /sales/00000012 is under /sales).
     */
    public Here here(List<Section> sections, String pathAndQuery) {
        if (pathAndQuery == null) return new Here(null, null);
        int q = pathAndQuery.indexOf('?');
        String path = q < 0 ? pathAndQuery : pathAndQuery.substring(0, q);
        Section bestS = null; Tab bestT = null; int best = -1;
        for (Section s : sections) {
            for (Tab t : s.tabs()) {
                String p = t.path();
                if (!(path.equals(p) || path.startsWith(p + "/"))) continue;
                int score = p.length() * 4 + (t.url().equals(pathAndQuery) ? 2 : 0) + (t.url().indexOf('?') < 0 ? 1 : 0);
                if (score > best) { best = score; bestS = s; bestT = t; }
            }
        }
        return new Here(bestS, bestT);
    }
}
