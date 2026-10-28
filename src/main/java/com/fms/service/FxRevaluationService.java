package com.fms.service;

import com.fms.domain.*;
import com.fms.repo.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Year-end exchange rate adjustment: what is still held or owed in EUR / USD on a date
 * (cash and bank accounts, open client and supplier invoices) is valued at that date's
 * rate; the difference to its value in the books is an (unrealised) exchange gain (769) or
 * loss (669). By default the entry is reversed the next day, so the difference realised
 * when an invoice is later paid (booked against the invoice's own rate) isn't counted twice.
 */
@Service
public class FxRevaluationService {

    /** One item: kind CASH / BANK / RECEIVABLE / PAYABLE. diff > 0: the item is worth more in lek. */
    public record Item(String key, String kind, String label, String currency, BigDecimal fx, BigDecimal rate,
                       BigDecimal book, BigDecimal value, BigDecimal diff) {
        public boolean liability() { return "PAYABLE".equals(kind); }
        /** + gain / − loss for the company. */
        public BigDecimal result() { return liability() ? diff.negate() : diff; }
    }

    private final CashService cash;
    private final MoneyMovementRepository moves;
    private final OrderInfoRepository infos;
    private final OrderService orders;
    private final PaymentService payments;
    private final CurrencyService currency;
    private final AccountingService accounting;
    private final AccAccountRepository accounts;
    private final FxRevalRepository revals;
    private final PeriodLock lock;
    private final AuditService audit;

    public FxRevaluationService(CashService cash, MoneyMovementRepository moves, OrderInfoRepository infos, OrderService orders,
                                PaymentService payments, CurrencyService currency, AccountingService accounting, AccAccountRepository accounts,
                                FxRevalRepository revals, PeriodLock lock, AuditService audit) {
        this.cash = cash; this.moves = moves; this.infos = infos; this.orders = orders; this.payments = payments; this.currency = currency;
        this.accounting = accounting; this.accounts = accounts; this.revals = revals; this.lock = lock; this.audit = audit;
    }

    /** The lek value of a foreign account's opening balance (at the rate of its opening date). */
    public BigDecimal openingBase(CashAccount a) {
        if (!a.isForeign() || a.getOpeningBalance() == null) return Money.nz(a.getOpeningBalance());
        BigDecimal r = currency.rateOn(a.getCurrency(), a.getOpeningDate());
        return r == null ? a.getOpeningBalance() : Money.round(a.getOpeningBalance().multiply(r));
    }

    private BigDecimal earlier(String key, LocalDate date) {
        BigDecimal s = BigDecimal.ZERO;
        for (FxReval r : revals.findByItemKeyAndRevalDateBeforeAndReversalIdIsNull(key, date)) s = s.add(r.getDiff());
        return s;
    }

    /** What would be adjusted on a date, at these rates (missing = the date's rate). */
    @Transactional(readOnly = true)
    public List<Item> preview(LocalDate date, Map<String, BigDecimal> rates) {
        List<Item> out = new ArrayList<>();
        // cash and bank accounts in EUR / USD
        for (CashAccount a : cash.allAccounts()) {
            if (!a.isForeign() || (a.getOpeningDate() != null && a.getOpeningDate().isAfter(date))) continue;
            BigDecimal rate = rate(rates, a.getCurrency(), date);
            BigDecimal fx = cash.balance(a, date);
            String key = "ACC:" + a.getId();
            BigDecimal book = Money.round(openingBase(a).add(moves.netBefore(a.getId(), date.plusDays(1))).add(earlier(key, date)));
            BigDecimal value = Money.round(fx.multiply(rate));
            out.add(new Item(key, a.isBank() ? "BANK" : "CASH", a.getName(), a.getCurrency(), fx, rate, book, value, value.subtract(book)));
        }
        // invoices in EUR / USD still open on the date
        for (OrderInfo i : infos.findAll()) {
            if (!i.isForeign() || i.getOrderType() == OrderType.OFFER) continue;
            OrderView o = orders.load(i.getOrderType(), i.getOrderCode());
            if (o.isEmpty() || o.lines().get(0).getCreatedAt().toLocalDate().isAfter(date)) continue;
            BigDecimal total = o.lines().stream().filter(l -> !l.getCreatedAt().toLocalDate().isAfter(date))
                    .map(OrderLine::getValueWithVat).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal paid = BigDecimal.ZERO;
            for (Payment p : payments.list(i.getOrderType(), i.getOrderCode())) {
                if (p.getPaidOn().isAfter(date)) continue;
                paid = p.isRefund() ? paid.subtract(p.getAmount()) : paid.add(p.getAmount());
            }
            BigDecimal open = Money.round(total.subtract(paid));
            if (open.signum() <= 0) continue;
            BigDecimal rate = rate(rates, i.getCurrency(), date);
            BigDecimal fx = open.divide(i.getFxRate(), 2, RoundingMode.HALF_UP);
            String key = i.getOrderType().name() + ":" + i.getOrderCode();
            BigDecimal book = Money.round(open.add(earlier(key, date)));
            BigDecimal value = Money.round(fx.multiply(rate));
            String party = i.getOrderType() == OrderType.SALE ? o.lines().get(0).getBuyerName() : o.lines().get(0).getProviderName();
            out.add(new Item(key, i.getOrderType() == OrderType.SALE ? "RECEIVABLE" : "PAYABLE",
                    i.getOrderCode() + " – " + party, i.getCurrency(), fx, rate, book, value, value.subtract(book)));
        }
        return out;
    }

    private BigDecimal rate(Map<String, BigDecimal> rates, String cur, LocalDate date) {
        BigDecimal r = rates == null ? null : rates.get(cur);
        if (r != null && r.signum() > 0) return r;
        return currency.requireRate(cur, date);
    }

    /** Posts the adjustment as a journal entry (and, if asked, its reversal the next day). */
    @Transactional
    public int post(LocalDate date, Map<String, BigDecimal> rates, boolean reverse, AppUser user) {
        lock.check(date);
        if (revals.existsByRevalDate(date)) throw new OrderException("This date was already adjusted. Undo it first to run it again.");
        List<Item> items = preview(date, rates).stream().filter(x -> x.diff().signum() != 0).toList();
        if (items.isEmpty()) throw new OrderException("Nothing to adjust on this date.");
        Map<Long, BigDecimal[]> lines = new LinkedHashMap<>();    // account -> [debit, credit]
        for (Item x : items) {
            Long acc = id(switch (x.kind()) { case "BANK" -> "BANK"; case "CASH" -> "CASH"; case "RECEIVABLE" -> "CLIENTS"; default -> "SUPPLIERS"; });
            Long gainLoss = id(x.result().signum() > 0 ? "FX_GAIN" : "FX_LOSS");
            BigDecimal d = x.diff().abs();
            boolean itemUp = x.diff().signum() > 0;                 // the item's lek value goes up (asset: debit; liability: credit)
            if (x.liability()) {
                add(lines, acc, itemUp ? null : d, itemUp ? d : null);
                add(lines, gainLoss, itemUp ? d : null, itemUp ? null : d);
            } else {
                add(lines, acc, itemUp ? d : null, itemUp ? null : d);
                add(lines, gainLoss, itemUp ? null : d, itemUp ? d : null);
            }
        }
        List<AccountingService.ManualLine> ml = new ArrayList<>();
        lines.forEach((acc, v) -> ml.add(new AccountingService.ManualLine(acc, v[0].signum() == 0 ? null : v[0], v[1].signum() == 0 ? null : v[1], null)));
        AccEntry entry = accounting.saveManual(null, date, "Rivlerësim i kursit të këmbimit / Exchange rate adjustment " + date, ml, user);
        Long reversalId = null;
        if (reverse) {
            List<AccountingService.ManualLine> rev = new ArrayList<>();
            for (var l : ml) rev.add(new AccountingService.ManualLine(l.accountId(), l.credit(), l.debit(), null));
            reversalId = accounting.saveManual(null, date.plusDays(1), "Anulim i rivlerësimit të kursit / Exchange rate adjustment reversed " + date, rev, user).getId();
        }
        for (Item x : items) {
            FxReval r = new FxReval();
            r.setRevalDate(date); r.setItemKey(x.key()); r.setLabel(x.label()); r.setCurrency(x.currency());
            r.setFxAmount(x.fx()); r.setRate(x.rate()); r.setBook(x.book()); r.setDiff(x.diff());
            r.setEntryId(entry.getId()); r.setReversalId(reversalId);
            revals.save(r);
        }
        audit.log("FX_REVAL", "Accounting", date.toString(), items.size() + " items" + (reverse ? ", reversed next day" : ""));
        return items.size();
    }

    @Transactional
    public void undo(LocalDate date) {
        List<FxReval> rs = revals.findByRevalDateOrderByIdAsc(date);
        if (rs.isEmpty()) throw new OrderException("Nothing to undo on this date.");
        lock.check(date);
        Set<Long> entries = new HashSet<>();
        for (FxReval r : rs) { if (r.getEntryId() != null) entries.add(r.getEntryId()); if (r.getReversalId() != null) entries.add(r.getReversalId()); }
        for (Long e : entries) accounting.deleteManual(e);
        revals.deleteAll(rs);
        audit.log("FX_REVAL_UNDO", "Accounting", date.toString(), null);
    }

    @Transactional(readOnly = true)
    public List<FxReval> history() { return revals.findAllByOrderByRevalDateDescIdAsc(); }

    private Long id(String key) {
        return accounts.findBySystemKey(key).map(AccAccount::getId)
                .orElseThrow(() -> new OrderException("The chart of accounts has no account for " + key + "."));
    }

    private static void add(Map<Long, BigDecimal[]> m, Long acc, BigDecimal debit, BigDecimal credit) {
        BigDecimal[] v = m.computeIfAbsent(acc, k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
        if (debit != null) v[0] = v[0].add(debit);
        if (credit != null) v[1] = v[1].add(credit);
    }
}
