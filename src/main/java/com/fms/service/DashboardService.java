package com.fms.service;

import com.fms.domain.AppUser;
import com.fms.domain.OrderInfo;
import com.fms.domain.OrderType;
import com.fms.repo.OrderInfoRepository;
import com.fms.repo.Totals;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The figures in the strip above the Home tiles. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    public record Strip(Totals salesToday, BigDecimal unpaidTotal, long unpaidOrders, long overdueOrders,
                        BigDecimal overdueTotal, long toRestock, long openOffers) {
    }

    private final ReportService reports;
    private final PaymentService payments;
    private final OrderInfoRepository infos;
    private final com.fms.repo.OrderLineRepository lines;

    public DashboardService(ReportService reports, PaymentService payments, OrderInfoRepository infos,
                            com.fms.repo.OrderLineRepository lines) {
        this.reports = reports;
        this.payments = payments;
        this.infos = infos;
        this.lines = lines;
    }

    public Strip strip(AppUser user) {
        Totals today = reports.totals(OrderType.SALE, user, Period.today());

        // Unpaid sales: everything for administrators, own sales for users.
        var debts = payments.debts(OrderType.SALE);
        var mine = debts.orders().stream()
                .filter(r -> user.isAdmin() || user.getUsername().equals(r.createdBy()))
                .toList();
        BigDecimal unpaid = mine.stream().map(PaymentService.DebtRow::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);
        var overdue = mine.stream().filter(r -> r.status() == PaymentService.Status.OVERDUE).toList();
        BigDecimal overdueTotal = overdue.stream().map(PaymentService.DebtRow::remaining).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Products below their quantity limit (same rule as For supply, margin 0).
        long restock = reports.warehouse(Period.custom(reports.firstActivityDate(), java.time.LocalDate.now())).stream()
                .filter(r -> r.quantityLimit() > 0 && r.remaining() < r.quantityLimit())
                .count();

        // Open offers that haven't expired or been converted.
        Set<String> offerCodes = lines.orderTotals(OrderType.OFFER).stream()
                .filter(r -> user.isAdmin() || user.getUsername().equals(r[5]))
                .map(r -> (String) r[0]).collect(Collectors.toSet());
        var infoByCode = infos.findByOrderType(OrderType.OFFER).stream()
                .collect(Collectors.toMap(OrderInfo::getOrderCode, i -> i, (a, b) -> a));
        long open = offerCodes.stream().filter(c -> {
            OrderInfo i = infoByCode.get(c);
            return i == null || "Open".equals(i.getOfferStatusLabel());
        }).count();

        return new Strip(today, Money.round(unpaid), mine.size(), overdue.size(), Money.round(overdueTotal), restock, open);
    }
}
