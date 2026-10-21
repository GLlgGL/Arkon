package com.fms.service;

import com.fms.domain.OrderLine;
import com.fms.domain.OrderType;
import com.fms.domain.Payment;
import com.fms.repo.OrderLineRepository;
import com.fms.repo.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Client and supplier statements (kartela): every invoice, return, payment
 * and refund of one party, with the opening balance and the balance after
 * each line. Balance = what the client owes us (sales) or what we owe the
 * supplier (purchases).
 */
@Service
public class StatementService {

    public record Row(LocalDate date, String document, String kind, BigDecimal charge, BigDecimal credit, BigDecimal balance, String orderCode) { }
    public record Statement(OrderType type, PartyInfo party, LocalDate from, LocalDate to, BigDecimal opening,
                            List<Row> rows, BigDecimal charges, BigDecimal credits, BigDecimal closing) { }

    private final OrderLineRepository lines;
    private final PaymentRepository payments;
    private final PartyService parties;

    public StatementService(OrderLineRepository lines, PaymentRepository payments, PartyService parties) {
        this.lines = lines;
        this.payments = payments;
        this.parties = parties;
    }

    private static String party(OrderType t, OrderLine l) {
        return t == OrderType.SALE ? l.getBuyerName() : l.getProviderName();
    }

    /** Clients (sales) or suppliers (purchases) that have orders. */
    @Transactional(readOnly = true)
    public List<String> parties(OrderType t) {
        TreeSet<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (OrderLine l : all(t)) s.add(party(t, l));
        return new ArrayList<>(s);
    }

    private List<OrderLine> all(OrderType t) {
        return lines.findLines(t, null, LocalDate.of(1900, 1, 1).atStartOfDay(), LocalDate.now().plusYears(50).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public Statement statement(OrderType t, String name, LocalDate from, LocalDate to) {
        // every movement of this party, all time, in date order
        List<Row> events = new ArrayList<>();
        Map<String, List<OrderLine>> groups = new LinkedHashMap<>();
        Set<String> codes = new HashSet<>();
        for (OrderLine l : all(t)) {
            if (!name.equalsIgnoreCase(party(t, l))) continue;
            codes.add(l.getOrderCode());
            groups.computeIfAbsent(l.getOrderCode() + "#" + (l.getReturnNo() == null ? 0 : l.getReturnNo()), k -> new ArrayList<>()).add(l);
        }
        for (List<OrderLine> g : groups.values()) {
            OrderLine f = g.get(0);
            BigDecimal total = g.stream().map(OrderLine::getValueWithVat).reduce(BigDecimal.ZERO, BigDecimal::add);
            LocalDate d = g.stream().map(l -> l.getCreatedAt().toLocalDate()).min(Comparator.naturalOrder()).get();
            boolean ret = f.isReturn();
            events.add(new Row(d, f.getOrderCode() + (ret ? "-" + f.getReturnNo() : ""), ret ? "RETURN" : "INVOICE",
                    total.signum() > 0 ? total : BigDecimal.ZERO, total.signum() < 0 ? total.negate() : BigDecimal.ZERO, null, f.getOrderCode()));
        }
        for (Payment p : payments.findAll()) {
            if (p.getOrderType() != t || !codes.contains(p.getOrderCode())) continue;
            boolean refund = p.isRefund();
            events.add(new Row(p.getPaidOn(), p.getOrderCode(), refund ? "REFUND" : "PAYMENT",
                    refund ? p.getAmount() : BigDecimal.ZERO, refund ? BigDecimal.ZERO : p.getAmount(), null, p.getOrderCode()));
        }
        events.sort(Comparator.comparing(Row::date).thenComparing(r -> "INVOICE".equals(r.kind()) ? 0 : 1));
        BigDecimal opening = BigDecimal.ZERO, run, charges = BigDecimal.ZERO, credits = BigDecimal.ZERO;
        for (Row r : events) {
            if (r.date().isBefore(from)) opening = opening.add(r.charge()).subtract(r.credit());
        }
        run = opening;
        List<Row> rows = new ArrayList<>();
        for (Row r : events) {
            if (r.date().isBefore(from) || r.date().isAfter(to)) continue;
            run = run.add(r.charge()).subtract(r.credit());
            charges = charges.add(r.charge());
            credits = credits.add(r.credit());
            rows.add(new Row(r.date(), r.document(), r.kind(), r.charge(), r.credit(), run, r.orderCode()));
        }
        PartyInfo info;
        try {
            info = t == OrderType.SALE ? parties.buyer(OrderType.SALE, name) : parties.provider(OrderType.PURCHASE, name);
        } catch (RuntimeException e) {
            info = null;
        }
        if (info == null) info = new PartyInfo(name, null, null, null, null, false);
        return new Statement(t, info, from, to, Money.round(opening), rows, Money.round(charges), Money.round(credits), Money.round(run));
    }
}
