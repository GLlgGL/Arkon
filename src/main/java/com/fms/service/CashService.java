package com.fms.service;

import com.fms.domain.AppUser;
import com.fms.domain.CashAccount;
import com.fms.domain.MoneyCategory;
import com.fms.domain.MoneyMovement;
import com.fms.domain.NotFoundException;
import com.fms.domain.OrderType;
import com.fms.domain.Payment;
import com.fms.repo.CashAccountRepository;
import com.fms.repo.MoneyMovementRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cash & Bank: accounts, balances, every money movement in or out.
 *
 * Payments and refunds recorded on sales and purchases become movements
 * automatically. Other income and expenses are entered by hand. Transfers
 * move money between accounts; reconciling records the difference between
 * what was counted (or the bank statement) and what IMS expected.
 */
@Service
public class CashService {

    private static final LocalDate EVER = LocalDate.of(1900, 1, 1);

    public record AccountBalance(CashAccount account, BigDecimal balance) {
    }

    /** A movement with the account balance right after it (for the ledger). */
    public record LedgerRow(MoneyMovement movement, BigDecimal balance) {
    }

    public record Ledger(List<LedgerRow> rows, BigDecimal opening, BigDecimal in, BigDecimal out, BigDecimal closing,
                         boolean singleAccount) {
    }

    public record Flow(Map<MoneyCategory, BigDecimal> in, Map<MoneyCategory, BigDecimal> out, BigDecimal totalIn,
                       BigDecimal totalOut, BigDecimal net, List<String> labels, List<BigDecimal> seriesIn,
                       List<BigDecimal> seriesOut) {
    }

    private final CashAccountRepository accounts;
    private final MoneyMovementRepository moves;
    private final AuditService audit;

    public CashService(CashAccountRepository accounts, MoneyMovementRepository moves, AuditService audit) {
        this.accounts = accounts;
        this.moves = moves;
        this.audit = audit;
    }

    // ------------------------------------------------------------- accounts

    @Transactional(readOnly = true)
    public List<CashAccount> allAccounts() {
        return accounts.findAllByOrderByKindAscNameAsc();
    }

    @Transactional(readOnly = true)
    public List<CashAccount> activeAccounts() {
        return accounts.findByActiveTrueOrderByKindAscNameAsc();
    }

    public CashAccount account(Long id) {
        return accounts.findById(id).orElseThrow(() -> new NotFoundException("Account not found"));
    }

    /** Balance of an account on a date (end of that day): opening balance + all movements up to it. */
    public BigDecimal balance(CashAccount a, LocalDate onDate) {
        if (onDate.isBefore(a.getOpeningDate())) {
            return BigDecimal.ZERO;
        }
        return Money.round(a.getOpeningBalance().add(moves.netBefore(a.getId(), onDate.plusDays(1))));
    }

    @Transactional(readOnly = true)
    public List<AccountBalance> balances() {
        LocalDate today = LocalDate.now();
        return allAccounts().stream().map(a -> new AccountBalance(a, balance(a, today))).toList();
    }

    @Transactional
    public CashAccount saveAccount(Long id, String name, CashAccount.Kind kind, String bankName, String iban,
                                   BigDecimal opening, LocalDate openingDate, boolean active, boolean isDefault) {
        if (name == null || name.isBlank()) {
            throw new OrderException("Enter a name for the account.");
        }
        if (id == null ? accounts.existsByName(name.trim()) : accounts.existsByNameAndIdNot(name.trim(), id)) {
            throw new OrderException("An account with this name already exists.");
        }
        CashAccount a = id == null ? new CashAccount() : account(id);
        a.setName(name);
        a.setKind(kind == null ? CashAccount.Kind.CASH : kind);
        a.setBankName(bankName);
        a.setIban(iban);
        a.setOpeningBalance(opening);
        a.setOpeningDate(openingDate);
        a.setActive(active);
        a.setDefaultAccount(isDefault && active);
        if (a.isDefaultAccount()) {
            // only one default per kind
            for (CashAccount other : accounts.findAll()) {
                if (other.getKind() == a.getKind() && other.isDefaultAccount() && !other.getId().equals(a.getId())) {
                    other.setDefaultAccount(false);
                    accounts.save(other);
                }
            }
        }
        CashAccount saved = accounts.save(a);
        audit.log(id == null ? "CREATE" : "UPDATE", "Cash account", saved.getName(),
                saved.getKind() + ", opening " + Money.format(saved.getOpeningBalance()));
        return saved;
    }

    @Transactional
    public void deleteAccount(Long id) {
        CashAccount a = account(id);
        if (moves.existsByAccountId(id)) {
            throw new OrderException("This account has movements, so it can't be deleted. Make it inactive instead.");
        }
        accounts.delete(a);
        audit.log("DELETE", "Cash account", a.getName(), null);
    }

    /** Account for a payment: the one chosen, or the default cash/bank account for the method. */
    public CashAccount accountFor(Long accountId, Payment.Method method) {
        if (accountId != null) {
            return accounts.findById(accountId).filter(CashAccount::isActive).orElse(null);
        }
        CashAccount.Kind kind = method == Payment.Method.CASH ? CashAccount.Kind.CASH : CashAccount.Kind.BANK;
        return accounts.findFirstByKindAndDefaultAccountTrueAndActiveTrue(kind)
                .or(() -> accounts.findFirstByKindAndActiveTrueOrderByIdAsc(kind))
                .orElse(null);
    }

    // --------------------------------------------------- payments -> movements

    /** Called when a payment or refund is recorded on a sale or purchase. */
    @Transactional
    public void recordPayment(Payment p) {
        CashAccount a = p.getAccountId() == null ? null : accounts.findById(p.getAccountId()).orElse(null);
        if (a == null) {
            return;
        }
        boolean sale = p.getOrderType() == OrderType.SALE;
        MoneyCategory c = sale ? (p.isRefund() ? MoneyCategory.SALE_REFUND : MoneyCategory.SALE_PAYMENT)
                               : (p.isRefund() ? MoneyCategory.PURCHASE_REFUND : MoneyCategory.PURCHASE_PAYMENT);
        MoneyMovement m = new MoneyMovement();
        m.setAccount(a);
        m.setMovedOn(p.getPaidOn());
        m.setCategory(c);
        m.setAmount(p.getAmount());
        m.setDescription(p.getNote());
        m.setPaymentId(p.getId());
        m.setOrderType(p.getOrderType());
        m.setOrderCode(p.getOrderCode());
        m.setCreatedBy(p.getCreatedBy());
        moves.save(m);
    }

    @Transactional
    public void deletePayment(Long paymentId) {
        moves.deleteAll(moves.findByPaymentId(paymentId));
    }

    @Transactional
    public void deleteOrder(OrderType type, String code) {
        moves.deleteOrder(type, code);
    }

    // --------------------------------------------------------- manual entries

    @Transactional
    public MoneyMovement addEntry(Long accountId, LocalDate date, MoneyCategory category, BigDecimal amount,
                                  String description, String refNo, AppUser user) {
        return addEntry(accountId, date, category, amount, description, refNo, null, user);
    }

    @Transactional
    public MoneyMovement addEntry(Long accountId, LocalDate date, MoneyCategory category, BigDecimal amount,
                                  String description, String refNo, Long costCenterId, AppUser user) {
        return addEntry(accountId, date, category, amount, description, refNo, costCenterId, null, user);
    }

    /** @param origin PAYROLL or TAX when a module makes the movement */
    @Transactional
    public MoneyMovement addEntry(Long accountId, LocalDate date, MoneyCategory category, BigDecimal amount,
                                  String description, String refNo, Long costCenterId, String origin, AppUser user) {
        if (category == null || !category.isManual()) {
            throw new OrderException("Choose a category.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new OrderException("Enter an amount greater than 0.");
        }
        CashAccount a = account(accountId);
        MoneyMovement m = new MoneyMovement();
        m.setAccount(a);
        m.setMovedOn(date == null ? LocalDate.now() : date);
        m.setCategory(category);
        m.setAmount(Money.round(amount));
        m.setDescription(description);
        m.setRefNo(refNo);
        m.setCostCenterId(costCenterId);
        m.setOrigin(origin);
        m.setCreatedBy(user.getUsername());
        MoneyMovement saved = moves.save(m);
        audit.log(category.isIn() ? "MONEY_IN" : "MONEY_OUT", "Cash & bank", a.getName(),
                category + " " + Money.format(saved.getAmount()) + (saved.getDescription() == null ? "" : ": " + saved.getDescription()));
        return saved;
    }

    /** Moves money between two accounts (e.g. depositing cash into the bank). */
    @Transactional
    public void transfer(Long fromId, Long toId, LocalDate date, BigDecimal amount, String note, AppUser user) {
        if (fromId == null || toId == null || fromId.equals(toId)) {
            throw new OrderException("Choose two different accounts.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new OrderException("Enter an amount greater than 0.");
        }
        CashAccount from = account(fromId);
        CashAccount to = account(toId);
        LocalDate d = date == null ? LocalDate.now() : date;
        MoneyMovement out = new MoneyMovement();
        out.setAccount(from);
        out.setMovedOn(d);
        out.setCategory(MoneyCategory.TRANSFER_OUT);
        out.setAmount(Money.round(amount));
        out.setDescription((note == null || note.isBlank() ? "" : note.trim() + " – ") + "→ " + to.getName());
        out.setCreatedBy(user.getUsername());
        out = moves.save(out);
        out.setTransferId(out.getId());
        moves.save(out);
        MoneyMovement in = new MoneyMovement();
        in.setAccount(to);
        in.setMovedOn(d);
        in.setCategory(MoneyCategory.TRANSFER_IN);
        in.setAmount(Money.round(amount));
        in.setDescription((note == null || note.isBlank() ? "" : note.trim() + " – ") + "← " + from.getName());
        in.setTransferId(out.getId());
        in.setCreatedBy(user.getUsername());
        moves.save(in);
        audit.log("TRANSFER", "Cash & bank", from.getName() + " → " + to.getName(), Money.format(amount));
    }

    /**
     * Reconcile: what was actually counted (cash) or what the bank statement
     * says on a date. The difference from IMS's balance is recorded as a
     * correction. Returns the difference (counted - expected).
     */
    @Transactional
    public BigDecimal reconcile(Long accountId, LocalDate date, BigDecimal counted, String note, AppUser user) {
        if (counted == null) {
            throw new OrderException("Enter the counted balance.");
        }
        CashAccount a = account(accountId);
        LocalDate d = date == null ? LocalDate.now() : date;
        BigDecimal expected = balance(a, d);
        BigDecimal diff = Money.round(counted.subtract(expected));
        if (diff.signum() != 0) {
            MoneyMovement m = new MoneyMovement();
            m.setAccount(a);
            m.setMovedOn(d);
            m.setCategory(diff.signum() > 0 ? MoneyCategory.ADJUSTMENT_IN : MoneyCategory.ADJUSTMENT_OUT);
            m.setAmount(diff.abs());
            m.setDescription((note == null || note.isBlank() ? "" : note.trim() + " – ") + "IMS " + Money.format(expected)
                    + " / " + Money.format(counted));
            m.setCreatedBy(user.getUsername());
            moves.save(m);
        }
        audit.log("RECONCILE", "Cash & bank", a.getName(), "expected " + Money.format(expected) + ", counted "
                + Money.format(counted) + ", difference " + Money.format(diff));
        return diff;
    }

    /** Deletes a hand-entered movement, or both halves of a transfer. Payments are deleted on their order. */
    @Transactional
    public void deleteMovement(Long id) {
        MoneyMovement m = moves.findById(id).orElseThrow(() -> new NotFoundException("Movement not found"));
        if (m.getPaymentId() != null) {
            throw new OrderException("This movement comes from a payment. Delete the payment on its order instead.");
        }
        if (m.getTransferId() != null) {
            moves.deleteAll(moves.findByTransferId(m.getTransferId()));
        } else {
            moves.delete(m);
        }
        audit.log("DELETE_MOVEMENT", "Cash & bank", m.getAccount().getName(),
                m.getCategory() + " " + Money.format(m.getAmount()) + " of " + m.getMovedOn());
    }

    // ---------------------------------------------------------------- reading

    @Transactional(readOnly = true)
    public List<MoneyMovement> recent() {
        return moves.findTop12ByOrderByMovedOnDescIdDesc();
    }

    /** Movements in a period, with a running balance when one account is chosen. */
    @Transactional(readOnly = true)
    public Ledger ledger(Long accountId, LocalDate from, LocalDate to, MoneyCategory category, String search) {
        LocalDate f = from == null ? EVER : from;
        LocalDate t = to == null ? LocalDate.now().plusYears(50) : to;
        String q = search == null ? "" : search.trim().toLowerCase();
        boolean single = accountId != null;
        BigDecimal opening = BigDecimal.ZERO;
        if (single) {
            CashAccount a = account(accountId);
            opening = f.isAfter(a.getOpeningDate()) ? balance(a, f.minusDays(1)) : a.getOpeningBalance();
        }
        BigDecimal running = opening;
        BigDecimal in = BigDecimal.ZERO;
        BigDecimal out = BigDecimal.ZERO;
        List<LedgerRow> rows = new ArrayList<>();
        for (MoneyMovement m : moves.between(accountId, f, t)) {
            running = running.add(m.getSigned());       // running balance counts every movement
            boolean shown = (category == null || m.getCategory() == category)
                    && (q.isEmpty() || contains(m.getDescription(), q) || contains(m.getRefNo(), q)
                        || contains(m.getOrderCode(), q) || contains(m.getAccount().getName(), q));
            if (shown) {
                rows.add(new LedgerRow(m, running));
                if (m.isIn()) in = in.add(m.getAmount()); else out = out.add(m.getAmount());
            }
        }
        return new Ledger(rows, Money.round(opening), Money.round(in), Money.round(out), Money.round(running), single);
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase().contains(q);
    }

    /** Cash flow for a period: totals per category, and in/out per day (or month for long periods). */
    @Transactional(readOnly = true)
    public Flow flow(LocalDate from, LocalDate to) {
        boolean byDay = !to.isAfter(from.plusDays(62));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(byDay ? "dd/MM" : "MM/yyyy");
        Map<MoneyCategory, BigDecimal> in = new LinkedHashMap<>();
        Map<MoneyCategory, BigDecimal> out = new LinkedHashMap<>();
        TreeMap<LocalDate, BigDecimal[]> buckets = new TreeMap<>();
        for (LocalDate d = byDay ? from : from.withDayOfMonth(1); !d.isAfter(to); d = byDay ? d.plusDays(1) : d.plusMonths(1)) {
            buckets.put(d, new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (MoneyMovement m : moves.between(null, from, to)) {
            // transfers move money between own accounts: not income or expense
            if (m.getCategory() == MoneyCategory.TRANSFER_IN || m.getCategory() == MoneyCategory.TRANSFER_OUT) {
                continue;
            }
            (m.isIn() ? in : out).merge(m.getCategory(), m.getAmount(), BigDecimal::add);
            LocalDate key = byDay ? m.getMovedOn() : m.getMovedOn().withDayOfMonth(1);
            BigDecimal[] b = buckets.computeIfAbsent(key, k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            b[m.isIn() ? 0 : 1] = b[m.isIn() ? 0 : 1].add(m.getAmount());
        }
        BigDecimal ti = in.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal to2 = out.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> labels = new ArrayList<>();
        List<BigDecimal> si = new ArrayList<>();
        List<BigDecimal> so = new ArrayList<>();
        buckets.forEach((d, b) -> { labels.add(d.format(fmt)); si.add(b[0]); so.add(b[1]); });
        return new Flow(in, out, Money.round(ti), Money.round(to2), Money.round(ti.subtract(to2)), labels, si, so);
    }

    // -------------------------------------------------------------- analytics

    /** One category in a comparison: amount in period A (current) and B (compared with). */
    public record CatRow(MoneyCategory category, BigDecimal a, BigDecimal b, BigDecimal diff, BigDecimal pct) {
    }

    public record Comparison(LocalDate fromA, LocalDate toA, LocalDate fromB, LocalDate toB,
                             BigDecimal inA, BigDecimal inB, BigDecimal outA, BigDecimal outB,
                             BigDecimal netA, BigDecimal netB, List<CatRow> rows) {
        public BigDecimal inPct() { return pct(inA, inB); }
        public BigDecimal outPct() { return pct(outA, outB); }
        public BigDecimal netPct() { return pct(netA, netB); }
    }

    /** Month-by-month: in/out/net for 12 months, the same months a year earlier, and the total balance at each month end. */
    public record Monthly(List<String> labels, List<BigDecimal> in, List<BigDecimal> out, List<BigDecimal> net,
                          List<BigDecimal> inPrev, List<BigDecimal> outPrev, List<BigDecimal> netPrev, List<BigDecimal> balance) {
    }

    /** Change in percent from b to a; null when b is zero (no basis to compare). */
    static BigDecimal pct(BigDecimal a, BigDecimal b) {
        if (b == null || b.signum() == 0) {
            return null;
        }
        return a.subtract(b).multiply(BigDecimal.valueOf(100)).divide(b.abs(), 1, java.math.RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public Comparison compare(LocalDate fromA, LocalDate toA, LocalDate fromB, LocalDate toB) {
        Flow a = flow(fromA, toA);
        Flow b = flow(fromB, toB);
        List<CatRow> rows = new ArrayList<>();
        for (MoneyCategory c : MoneyCategory.values()) {
            if (c == MoneyCategory.TRANSFER_IN || c == MoneyCategory.TRANSFER_OUT) {
                continue;
            }
            BigDecimal va = (c.isIn() ? a.in() : a.out()).getOrDefault(c, BigDecimal.ZERO);
            BigDecimal vb = (c.isIn() ? b.in() : b.out()).getOrDefault(c, BigDecimal.ZERO);
            if (va.signum() != 0 || vb.signum() != 0) {
                rows.add(new CatRow(c, Money.round(va), Money.round(vb), Money.round(va.subtract(vb)), pct(va, vb)));
            }
        }
        return new Comparison(fromA, toA, fromB, toB, a.totalIn(), b.totalIn(), a.totalOut(), b.totalOut(),
                a.net(), b.net(), rows);
    }

    @Transactional(readOnly = true)
    public Monthly monthly(LocalDate lastMonth) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
        List<String> labels = new ArrayList<>();
        List<BigDecimal> in = new ArrayList<>(), out = new ArrayList<>(), net = new ArrayList<>();
        List<BigDecimal> inP = new ArrayList<>(), outP = new ArrayList<>(), netP = new ArrayList<>(), bal = new ArrayList<>();
        List<CashAccount> all = allAccounts();
        for (int i = 11; i >= 0; i--) {
            LocalDate m = lastMonth.withDayOfMonth(1).minusMonths(i);
            LocalDate end = m.plusMonths(1).minusDays(1);
            Flow f = flow(m, end);
            Flow p = flow(m.minusYears(1), end.minusYears(1));
            labels.add(m.format(fmt));
            in.add(f.totalIn()); out.add(f.totalOut()); net.add(f.net());
            inP.add(p.totalIn()); outP.add(p.totalOut()); netP.add(p.net());
            bal.add(Money.round(all.stream().map(a -> balance(a, end)).reduce(BigDecimal.ZERO, BigDecimal::add)));
        }
        return new Monthly(labels, in, out, net, inP, outP, netP, bal);
    }
}
