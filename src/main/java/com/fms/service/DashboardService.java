package com.fms.service;

import com.fms.domain.AppUser;
import com.fms.domain.Area;
import com.fms.domain.OrderInfo;
import com.fms.domain.OrderLine;
import com.fms.domain.OrderType;
import com.fms.domain.UserDashboard;
import com.fms.domain.Widget;
import com.fms.repo.OrderInfoRepository;
import com.fms.repo.OrderLineRepository;
import com.fms.repo.Totals;
import com.fms.repo.UserDashboardRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The personal dashboard: which widgets a user has chosen (or their role's
 * default), limited to what they're allowed to see, and each widget's data.
 */
@Service
public class DashboardService {

    /** This month so far compared with the same days of last month. */
    public record MonthCompare(BigDecimal now, BigDecimal before, BigDecimal pct, long orders) {
    }

    public record Named(String name, BigDecimal value) {
    }

    public record Trend(List<String> labels, List<BigDecimal> values) {
    }

    private final UserDashboardRepository layouts;
    private final AccessService access;
    private final ReportService reports;
    private final PaymentService payments;
    private final OrderInfoRepository infos;
    private final OrderLineRepository lines;
    private final OrderService orders;
    private final CashService cash;
    private final TaxService taxes;
    private final PurchaseOrderService purchaseOrders;

    public DashboardService(UserDashboardRepository layouts, AccessService access, ReportService reports,
                            PaymentService payments, OrderInfoRepository infos, OrderLineRepository lines,
                            OrderService orders, CashService cash, TaxService taxes, PurchaseOrderService purchaseOrders) {
        this.layouts = layouts;
        this.access = access;
        this.reports = reports;
        this.payments = payments;
        this.infos = infos;
        this.lines = lines;
        this.orders = orders;
        this.cash = cash;
        this.taxes = taxes;
        this.purchaseOrders = purchaseOrders;
    }

    // ----------------------------------------------------------------- layout

    /** Widgets this user may use. */
    public List<Widget> allowed(AppUser u) {
        Map<Area, Area.Level> lv = access.levels(u);
        return Arrays.stream(Widget.values())
                .filter(w -> Arrays.stream(w.getAreas()).anyMatch(a -> lv.get(a).atLeast(Area.Level.VIEW)))
                .toList();
    }

    /** The user's dashboard in order (their own, or the role default), only allowed widgets. */
    @Transactional(readOnly = true)
    public List<Widget> layout(AppUser u) {
        List<Widget> ok = allowed(u);
        List<Widget> chosen = layouts.findById(u.getUsername())
                .map(d -> Arrays.stream(d.getWidgets().split(",")).filter(s -> !s.isBlank()).map(s -> {
                    try { return Widget.valueOf(s.trim()); } catch (IllegalArgumentException e) { return null; }
                }).filter(java.util.Objects::nonNull).toList())
                .orElse(Widget.defaults(u));
        return chosen.stream().filter(ok::contains).distinct().toList();
    }

    public boolean isCustom(AppUser u) {
        return layouts.existsById(u.getUsername());
    }

    @Transactional
    public void save(AppUser u, List<String> keys) {
        Set<Widget> ok = Set.copyOf(allowed(u));
        String csv = keys.stream().map(k -> {
            try { return Widget.valueOf(k); } catch (IllegalArgumentException e) { return null; }
        }).filter(w -> w != null && ok.contains(w)).distinct().map(Enum::name).collect(Collectors.joining(","));
        UserDashboard d = layouts.findById(u.getUsername()).orElseGet(UserDashboard::new);
        d.setUsername(u.getUsername());
        d.setWidgets(csv);
        layouts.save(d);
    }

    @Transactional
    public void reset(AppUser u) {
        layouts.deleteById(u.getUsername());
    }

    // ------------------------------------------------------------------- data

    /** Data for each widget on the dashboard, by widget name. */
    @Transactional(readOnly = true)
    public Map<String, Object> data(AppUser u, List<Widget> widgets) {
        Map<String, Object> d = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (Widget w : widgets) {
            switch (w) {
                case SALES_TODAY -> d.put(w.name(), safe(reports.totals(OrderType.SALE, u, Period.today())));
                case SALES_MONTH -> {
                    Totals now = safe(reports.totals(OrderType.SALE, u, Period.custom(today.withDayOfMonth(1), today)));
                    LocalDate lmStart = today.withDayOfMonth(1).minusMonths(1);
                    LocalDate lmEnd = lmStart.plusDays(Math.min(today.getDayOfMonth(), lmStart.lengthOfMonth()) - 1);
                    Totals before = safe(reports.totals(OrderType.SALE, u, Period.custom(lmStart, lmEnd)));
                    d.put(w.name(), new MonthCompare(Money.nz(now.valueWithVat()), Money.nz(before.valueWithVat()),
                            CashService.pct(Money.nz(now.valueWithVat()), Money.nz(before.valueWithVat())), now.orders()));
                }
                case SALES_TREND -> {
                    TreeMap<LocalDate, BigDecimal> days = new TreeMap<>();
                    for (int i = 29; i >= 0; i--) days.put(today.minusDays(i), BigDecimal.ZERO);
                    for (OrderLine l : saleLines(u, today.minusDays(29), today)) {
                        days.merge(l.getCreatedAt().toLocalDate(), l.getValueWithVat(), BigDecimal::add);
                    }
                    DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM");
                    d.put(w.name(), new Trend(days.keySet().stream().map(x -> x.format(f)).toList(), new ArrayList<>(days.values())));
                }
                case TOP_PRODUCTS -> {
                    Map<String, BigDecimal> m = new HashMap<>();
                    for (OrderLine l : saleLines(u, today.withDayOfMonth(1), today)) m.merge(l.getProductName(), l.getValueNoVat(), BigDecimal::add);
                    d.put(w.name(), m.entrySet().stream().sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                            .limit(5).map(e -> new Named(e.getKey(), e.getValue())).toList());
                }
                case RECENT_SALES -> d.put(w.name(), orders.list(OrderType.SALE, u, null, null, null, PageRequest.of(0, 6)).getContent());
                case UNPAID_CLIENTS, UNPAID_MERCHANTS -> {
                    var debts = payments.debts(w == Widget.UNPAID_CLIENTS ? OrderType.SALE : OrderType.PURCHASE);
                    var mine = debts.orders().stream().filter(r -> u.isSeesAll() || u.getUsername().equals(r.createdBy())).toList();
                    d.put(w.name(), Map.of(
                            "total", mine.stream().map(PaymentService.DebtRow::remaining).reduce(BigDecimal.ZERO, BigDecimal::add),
                            "count", mine.size(),
                            "overdue", mine.stream().filter(r -> r.status() == PaymentService.Status.OVERDUE).count()));
                }
                case OPEN_OFFERS -> {
                    Set<String> codes = lines.orderTotals(OrderType.OFFER).stream()
                            .filter(r -> u.isSeesAll() || u.getUsername().equals(r[5])).map(r -> (String) r[0]).collect(Collectors.toSet());
                    var byCode = infos.findByOrderType(OrderType.OFFER).stream().collect(Collectors.toMap(OrderInfo::getOrderCode, i -> i, (a, b) -> a));
                    d.put(w.name(), codes.stream().filter(c -> byCode.get(c) == null || "Open".equals(byCode.get(c).getOfferStatusLabel())).count());
                }
                case RETURNS_MONTH -> {
                    var ret = saleLines(u, today.withDayOfMonth(1), today).stream().filter(OrderLine::isReturn).toList();
                    d.put(w.name(), Map.of(
                            "value", ret.stream().map(OrderLine::getValueWithVat).reduce(BigDecimal.ZERO, BigDecimal::add).negate(),
                            "count", ret.stream().map(l -> l.getOrderCode() + "#" + l.getReturnNo()).distinct().count()));
                }
                case RESTOCK -> {
                    var rows = reports.warehouse(Period.custom(reports.firstActivityDate(), today)).stream()
                            .filter(r -> r.quantityLimit() > 0 && r.remaining() < r.quantityLimit()).toList();
                    d.put(w.name(), rows);
                }
                case OPEN_PURCHASE_ORDERS -> d.put(w.name(), purchaseOrders.all().stream().filter(com.fms.domain.PurchaseOrder::isOpen).toList());
                case CASH_BALANCES -> d.put(w.name(), cash.balances().stream().filter(b -> b.account().isActive()).toList());
                case TAX_OPEN -> d.put(w.name(), taxes.overview().stream().filter(r -> r.open().signum() > 0).toList());
                default -> { }
            }
        }
        return d;
    }

    /** No sales in a period gives no totals row: use zeros instead. */
    private static Totals safe(Totals t) {
        return t == null ? Totals.empty() : t.orZero();
    }

    private List<OrderLine> saleLines(AppUser u, LocalDate from, LocalDate to) {
        return lines.findLines(OrderType.SALE, u.isSeesAll() ? null : u.getUsername(), from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }
}
