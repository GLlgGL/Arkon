package com.fms.service;

import com.fms.domain.*;
import com.fms.repo.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Accounting (kontabilitet): chart of accounts, journal, ledger, trial
 * balance, statements and VAT books.
 *
 * Automatic entries are built from what IMS records (sales, purchases,
 * payments, Cash & Bank, payroll, depreciation, stock adjustments, tax
 * payments, opening balances). "Update automatic entries" rebuilds them for a
 * period from the current data, so edits and deletions are always reflected.
 * Manual entries are never touched by it.
 */
@Service
public class AccountingService {

    private static final LocalDate EVER = LocalDate.of(1900, 1, 1);

    public record LedgerRow(AccLine line, BigDecimal balance) { }
    public record Ledger(AccAccount account, BigDecimal opening, List<LedgerRow> rows, BigDecimal debit, BigDecimal credit, BigDecimal closing) { }
    public record TrialRow(AccAccount account, BigDecimal opening, BigDecimal debit, BigDecimal credit, BigDecimal closing) { }
    public record Trial(List<TrialRow> rows, BigDecimal debit, BigDecimal credit, BigDecimal openingDr, BigDecimal openingCr,
                        BigDecimal closingDr, BigDecimal closingCr) { }
    public record StatementRow(AccAccount account, BigDecimal amount) { }
    public record Statements(List<StatementRow> income, List<StatementRow> expenses, BigDecimal totalIncome, BigDecimal totalExpenses,
                             BigDecimal result, List<StatementRow> assets, List<StatementRow> liabilities, List<StatementRow> equity,
                             BigDecimal totalAssets, BigDecimal totalLiabilities, BigDecimal totalEquity, BigDecimal resultToDate) {
        public BigDecimal getLiabilitiesAndEquity() { return totalLiabilities.add(totalEquity).add(resultToDate); }
    }
    /** rate: VAT % of the invoice; zeroColumn: for 0% sales, EXPORT / ZERO / EXEMPT / NOVAT (the client's setting). */
    public record VatRow(LocalDate date, String number, String party, String tin, BigDecimal noVat, BigDecimal vat, BigDecimal total, boolean isReturn,
                         int rate, String zeroColumn, String purchaseKind) { }
    public record VatBook(OrderType type, YearMonth month, List<VatRow> rows, BigDecimal noVat, BigDecimal vat, BigDecimal total) { }
    public record ManualLine(Long accountId, BigDecimal debit, BigDecimal credit, String note) { }

    private final AccAccountRepository accounts;
    private final AccEntryRepository entries;
    private final OrderLineRepository lines;
    private final PaymentRepository payments;
    private final MoneyMovementRepository moves;
    private final CashAccountRepository cashAccounts;
    private final PayrollRunRepository runs;
    private final PayrollService payroll;
    private final AssetRepository assets;
    private final AssetService assetService;
    private final StockAdjustmentRepository adjustments;
    private final TaxPaymentRepository taxPayments;
    private final ProductRepository products;
    private final PricingService pricing;
    private final PartyService parties;
    private final ConfigService config;
    private final AuditService audit;
    @org.springframework.beans.factory.annotation.Autowired
    private DocText doc;
    @org.springframework.beans.factory.annotation.Autowired
    private PeriodLock lock;
    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.repo.ClientRepository clientRepo;
    @org.springframework.beans.factory.annotation.Autowired
    private com.fms.repo.OrderInfoRepository infoRepo;

    public AccountingService(AccAccountRepository accounts, AccEntryRepository entries, OrderLineRepository lines,
                             PaymentRepository payments, MoneyMovementRepository moves, CashAccountRepository cashAccounts,
                             PayrollRunRepository runs, PayrollService payroll, AssetRepository assets, AssetService assetService,
                             StockAdjustmentRepository adjustments, TaxPaymentRepository taxPayments, ProductRepository products,
                             PricingService pricing, PartyService parties, ConfigService config, AuditService audit) {
        this.accounts = accounts; this.entries = entries; this.lines = lines; this.payments = payments; this.moves = moves;
        this.cashAccounts = cashAccounts; this.runs = runs; this.payroll = payroll; this.assets = assets; this.assetService = assetService;
        this.adjustments = adjustments; this.taxPayments = taxPayments; this.products = products; this.pricing = pricing;
        this.parties = parties; this.config = config; this.audit = audit;
    }

    // ------------------------------------------------------- chart of accounts

    @Transactional(readOnly = true)
    public List<AccAccount> accounts() {
        return accounts.findAllByOrderByCodeAsc();
    }

    public AccAccount account(Long id) {
        return accounts.findById(id).orElseThrow(() -> new NotFoundException("Account not found"));
    }

    /** The line of the official statements an account goes to (empty = from the template's list). */
    @Transactional
    public void setStatementLine(Long id, String line) {
        AccAccount a = account(id);
        a.setStmtLine(line);
        accounts.save(a);
    }

    @Transactional
    public AccAccount saveAccount(Long id, String code, String name, AccAccount.Type type, boolean active) {
        if (code == null || code.isBlank() || name == null || name.isBlank() || type == null) {
            throw new OrderException("Enter the code, name and type.");
        }
        if (id == null ? accounts.existsByCode(code.trim()) : accounts.existsByCodeAndIdNot(code.trim(), id)) {
            throw new OrderException("An account with this code already exists.");
        }
        AccAccount a = id == null ? new AccAccount() : account(id);
        a.setCode(code);
        a.setName(name);
        if (a.getSystemKey() == null) {
            a.setType(type);           // accounts used by automatic entries keep their type
        }
        a.setActive(active || a.getSystemKey() != null);
        AccAccount saved = accounts.save(a);
        audit.log(id == null ? "CREATE" : "UPDATE", "Account", saved.getLabel(), null);
        return saved;
    }

    @Transactional
    public void deleteAccount(Long id) {
        AccAccount a = account(id);
        if (a.getSystemKey() != null) {
            throw new OrderException("This account is used by automatic entries and can't be deleted. You can rename it.");
        }
        if (!entries.ledger(id, EVER, LocalDate.now().plusYears(100)).isEmpty()) {
            throw new OrderException("This account has entries. Make it inactive instead.");
        }
        accounts.delete(a);
        audit.log("DELETE", "Account", a.getLabel(), null);
    }

    private AccAccount key(String k) {
        return accounts.findBySystemKey(k).orElseThrow(() -> new OrderException("Account for " + k + " is missing in the chart of accounts."));
    }

    // ------------------------------------------------------------- posting

    /** An entry being built: signed amounts per account (+ debit, − credit). */
    private final class Draft {
        final LocalDate date; final String description; final String source; final String ref;
        final Map<AccAccount, BigDecimal> amounts = new LinkedHashMap<>();
        Draft(LocalDate date, String description, String source, String ref) {
            this.date = date; this.description = description; this.source = source; this.ref = ref;
        }
        Draft add(String accountKey, BigDecimal signed) { return add(key(accountKey), signed); }
        Draft add(AccAccount a, BigDecimal signed) {
            if (signed != null && signed.signum() != 0) amounts.merge(a, Money.round(signed), BigDecimal::add);
            return this;
        }
    }

    private String cashKey(Long cashAccountId, boolean cashMethod) {
        if (cashAccountId != null) {
            var a = cashAccounts.findById(cashAccountId).orElse(null);
            if (a != null) return a.isBank() ? "BANK" : "CASH";
        }
        return cashMethod ? "CASH" : "BANK";
    }

    /** A cash / bank account's opening balance in lek (EUR / USD accounts at the rate of their opening date). */
    private BigDecimal openingBase(com.fms.domain.CashAccount c) {
        if (!c.isForeign()) return c.getOpeningBalance();
        BigDecimal r = currencyService.rateOn(c.getCurrency(), c.getOpeningDate());
        return r == null ? c.getOpeningBalance() : Money.round(c.getOpeningBalance().multiply(r));
    }

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private CurrencyService currencyService;

    /** The entry of one sale or purchase (or one of its returns): the same rule the rebuild uses. */
    private Draft orderDraft(OrderType t, List<OrderLine> g, Map<String, Product> productByName) {
        OrderLine f = g.get(0);
        LocalDate d = g.stream().map(l -> l.getCreatedAt().toLocalDate()).min(Comparator.naturalOrder()).get();
        BigDecimal noVat = sum(g, OrderLine::getValueNoVat), vat = sum(g, OrderLine::getVatValue), total = noVat.add(vat);
        boolean ret = f.isReturn();
        if (t == OrderType.SALE) {
            BigDecimal cogs = BigDecimal.ZERO;
            for (OrderLine l : g) {
                BigDecimal unit = l.getUnitCost();
                if (unit == null) { Product p = productByName.get(l.getProductName()); unit = p == null ? BigDecimal.ZERO : p.getBuyPrice(); }
                cogs = cogs.add(unit.multiply(BigDecimal.valueOf(l.getQuantity())));
            }
            return new Draft(d, doc.t(ret ? "je.saleReturn" : "je.sale", "Sale {0} – {1}", f.getOrderCode(), f.getBuyerName()), "SALE", f.getOrderCode())
                    .add("CLIENTS", total).add("SALES", noVat.negate()).add("VAT_OUTPUT", vat.negate())
                    .add("COGS", cogs).add("INVENTORY", cogs.negate());
        }
        return new Draft(d, doc.t(ret ? "je.purchaseReturn" : "je.purchase", "Purchase {0} – {1}", f.getOrderCode(), f.getProviderName()), "PURCHASE", f.getOrderCode())
                .add("INVENTORY", noVat).add("VAT_INPUT", vat).add("SUPPLIERS", total.negate());
    }

    /** The entry of one payment or refund: the same rule the rebuild uses. */
    private Draft paymentDraft(Payment p) {
        String cash = cashKey(p.getAccountId(), p.getMethod() == Payment.Method.CASH);
        BigDecimal a = p.getAmount();
        boolean sale = p.getOrderType() == OrderType.SALE;
        BigDecimal moneyIn = sale ^ p.isRefund() ? a : a.negate();       // + money into our account (at the order's rate)
        BigDecimal real = p.getRealAmount();
        BigDecimal realIn = sale ^ p.isRefund() ? real : real.negate();
        return new Draft(p.getPaidOn(), doc.t("je." + (p.isRefund() ? "refund" : "payment") + (sale ? "Sale" : "Purchase"), "Payment {0}", p.getOrderCode()),
                "PAYMENT", p.getOrderCode())
                .add(cash, realIn).add(sale ? "CLIENTS" : "SUPPLIERS", moneyIn.negate())
                .add(realIn.compareTo(moneyIn) > 0 ? "FX_GAIN" : "FX_LOSS", moneyIn.subtract(realIn));
    }

    /** One line of a document's accounting entry, for the order page. */
    public record DocLine(String code, String name, BigDecimal debit, BigDecimal credit) { }
    /** One accounting entry of a document (the order itself, a return, a payment). */
    public record DocEntry(LocalDate date, String description, List<DocLine> lines, BigDecimal debit, BigDecimal credit) { }

    /**
     * The accounting entries of one sale or purchase, built live from its lines and payments
     * with the same rules as the journal, so they show while the order is being made.
     */
    @Transactional(readOnly = true)
    public List<DocEntry> documentEntries(OrderType t, String code) {
        if (t == OrderType.OFFER || code == null) return List.of();
        Map<String, Product> productByName = new HashMap<>();
        Map<String, List<OrderLine>> groups = new LinkedHashMap<>();
        for (OrderLine l : lines.findByOrderTypeAndOrderCodeOrderByCreatedAtAscIdAsc(t, code)) {
            groups.computeIfAbsent(String.valueOf(l.getReturnNo() == null ? 0 : l.getReturnNo()), k -> new ArrayList<>()).add(l);
            productByName.computeIfAbsent(l.getProductName(), n -> products.findByName(n).orElse(null));
        }
        List<Draft> drafts = new ArrayList<>();
        groups.values().forEach(g -> drafts.add(orderDraft(t, g, productByName)));
        payments.findByOrderTypeAndOrderCodeOrderByPaidOnAscIdAsc(t, code).forEach(p -> drafts.add(paymentDraft(p)));
        List<DocEntry> out = new ArrayList<>();
        for (Draft d : drafts) {
            List<DocLine> ls = new ArrayList<>();
            BigDecimal dr = BigDecimal.ZERO, cr = BigDecimal.ZERO;
            for (var e : d.amounts.entrySet()) {
                BigDecimal v = e.getValue();
                if (v.signum() == 0) continue;
                ls.add(new DocLine(e.getKey().getCode(), e.getKey().getName(), v.signum() > 0 ? v : null, v.signum() < 0 ? v.negate() : null));
                if (v.signum() > 0) dr = dr.add(v); else cr = cr.add(v.negate());
            }
            ls.sort(Comparator.comparing((DocLine l) -> l.debit() == null).thenComparing(DocLine::code));   // debits first
            out.add(new DocEntry(d.date, d.description, ls, dr, cr));
        }
        return out;
    }

    /**
     * Rebuilds the automatic entries of a period from the current data.
     * Returns how many entries were created.
     */
    @Transactional
    public int sync(LocalDate from, LocalDate to, AppUser user) {
        LocalDate locked = lock.lockedUntil();
        if (locked != null && !from.isAfter(locked)) {
            if (!to.isAfter(locked)) {
                lock.check(to);
            }
            from = locked.plusDays(1);          // locked months keep their entries
        }
        entries.deleteAll(entries.automaticBetween(from, to));
        entries.flush();
        List<Draft> drafts = new ArrayList<>();
        var start = from.atStartOfDay();
        var end = to.plusDays(1).atStartOfDay();
        Map<String, Product> productByName = new HashMap<>();
        products.findAll().forEach(p -> productByName.put(p.getName(), p));

        // Sales and purchases, one entry per order and per return
        for (OrderType t : List.of(OrderType.SALE, OrderType.PURCHASE)) {
            Map<String, List<OrderLine>> groups = new LinkedHashMap<>();
            for (OrderLine l : lines.findLines(t, null, start, end)) {
                groups.computeIfAbsent(l.getOrderCode() + "#" + (l.getReturnNo() == null ? 0 : l.getReturnNo()), k -> new ArrayList<>()).add(l);
            }
            for (List<OrderLine> g : groups.values()) {
                drafts.add(orderDraft(t, g, productByName));
            }
        }

        // Payments and refunds
        for (Payment p : payments.findAll()) {
            if (p.getPaidOn().isBefore(from) || p.getPaidOn().isAfter(to)) continue;
            drafts.add(paymentDraft(p));
        }

        // Cash & Bank: hand entries, transfers and corrections (payments, payroll and tax come from their own sources)
        Set<Long> transfersDone = new HashSet<>();
        for (MoneyMovement m : moves.between(null, from, to)) {
            if (m.getPaymentId() != null || m.getOrigin() != null) continue;
            String acct = m.getAccount().isBank() ? "BANK" : "CASH";
            if (m.getTransferId() != null) {
                if (!transfersDone.add(m.getTransferId())) continue;
                Draft d = new Draft(m.getMovedOn(), doc.t("je.transfer", "Transfer {0}", m.getDescription() == null ? "" : m.getDescription()), "CASH", "T" + m.getTransferId());
                BigDecimal net = BigDecimal.ZERO;
                for (MoneyMovement half : moves.findByTransferId(m.getTransferId())) {
                    d.add(half.getAccount().isBank() ? "BANK" : "CASH", half.getSigned());
                    net = net.add(half.getSigned());
                }
                if (net.signum() != 0) d.add(net.signum() < 0 ? "FX_GAIN" : "FX_LOSS", net.negate());   // exchange gain or loss
                drafts.add(d);
                continue;
            }
            String counter = switch (m.getCategory()) {
                case CAPITAL -> "CAPITAL"; case LOAN_IN, LOAN_REPAYMENT -> "LOANS"; case OTHER_INCOME, ADJUSTMENT_IN -> "OTHER_INCOME";
                case RENT -> "RENT"; case SALARIES -> config.get().isModPayroll() ? "WAGES_PAYABLE" : "WAGES";
                case UTILITIES -> "UTILITIES"; case TRANSPORT -> "TRANSPORT"; case TAXES -> "TAX_EXPENSE"; case BANK_FEES -> "BANK_FEES";
                case OWNER_WITHDRAWAL -> "OWNER"; case ADJUSTMENT_OUT -> "LOSSES"; default -> "OTHER_EXPENSE";
            };
            drafts.add(new Draft(m.getMovedOn(), doc.t("cash.cat." + m.getCategory().name(), m.getCategory().name())
                    + (m.getDescription() == null ? "" : ": " + m.getDescription()), "CASH", m.getRefNo())
                    .add(acct, m.getSigned()).add(counter, m.getSigned().negate()));
        }

        // Payroll: the month's wages (approved or paid), and the payment of net salaries
        for (PayrollRun r : runs.findAll()) {
            if (r.isDraft()) continue;
            YearMonth ym = YearMonth.parse(r.getPeriod());
            LocalDate monthEnd = ym.atEndOfMonth();
            var t = payroll.totals(payroll.lines(r.getId()));
            if (!monthEnd.isBefore(from) && !monthEnd.isAfter(to)) {
                drafts.add(new Draft(monthEnd, doc.t("je.payroll", "Payroll {0}", r.getPeriod()), "PAYROLL", r.getPeriod())
                        .add("WAGES", t.gross()).add("EMPLOYER_CONTRIB", t.erContrib())
                        .add("WAGES_PAYABLE", t.net().negate()).add("CONTRIBUTIONS", t.empContrib().add(t.erContrib()).negate())
                        .add("PIT", t.tax().negate()));
            }
            if ("PAID".equals(r.getStatus()) && r.getPaidOn() != null && !r.getPaidOn().isBefore(from) && !r.getPaidOn().isAfter(to)) {
                drafts.add(new Draft(r.getPaidOn(), doc.t("je.salariesPaid", "Salaries paid {0}", r.getPeriod()), "PAYROLL", r.getPeriod())
                        .add("WAGES_PAYABLE", t.net()).add(moduleMoneyAccount("PAYROLL", r.getPeriod()), t.net().negate()));
            }
        }

        // Tax payments
        for (TaxPayment tp : taxPayments.findAll()) {
            if (tp.getPaidOn().isBefore(from) || tp.getPaidOn().isAfter(to)) continue;
            Draft d = new Draft(tp.getPaidOn(), doc.t("VAT".equals(tp.getKind()) ? "je.vatPaid" : "je.payrollTaxesPaid", "Tax paid {0}", tp.getPeriod()), "TAX", tp.getPeriod())
                    .add(moduleMoneyAccount("TAX", tp.getPeriod()), tp.getAmount().negate());
            if ("VAT".equals(tp.getKind())) {
                d.add("VAT_OUTPUT", tp.getAmount());
            } else {
                var run = runs.findByPeriod(tp.getPeriod()).orElse(null);
                BigDecimal share = BigDecimal.ZERO;
                if (run != null) {
                    var t = payroll.totals(payroll.lines(run.getId()));
                    if (t.taxesDue().signum() > 0) share = t.tax().divide(t.taxesDue(), 6, java.math.RoundingMode.HALF_UP);
                }
                BigDecimal pit = Money.round(tp.getAmount().multiply(share));
                d.add("PIT", pit).add("CONTRIBUTIONS", tp.getAmount().subtract(pit));
            }
            drafts.add(d);
        }

        // Depreciation, one entry per month for all assets
        Map<YearMonth, BigDecimal> dep = new TreeMap<>();
        for (Asset a : assets.findAll()) {
            for (var m : assetService.schedule(a)) {
                LocalDate me = m.month().atEndOfMonth();
                if (!me.isBefore(from) && !me.isAfter(to)) dep.merge(m.month(), m.depreciation(), BigDecimal::add);
            }
        }
        dep.forEach((m, v) -> drafts.add(new Draft(m.atEndOfMonth(), doc.t("je.depreciation", "Depreciation {0}", m.toString()), "DEPRECIATION", m.toString())
                .add("DEPRECIATION", v).add("ACC_DEPRECIATION", v.negate())));

        // Stock adjustments at average cost (production moves goods between stock and stock: not posted)
        for (StockAdjustment a : adjustments.findAll()) {
            LocalDate d = a.getCreatedAt().toLocalDate();
            if (d.isBefore(from) || d.isAfter(to) || !a.getReason().isManual()) continue;
            Product p = productByName.get(a.getProductName());
            if (p == null) continue;
            BigDecimal v = pricing.averageCost(p).multiply(BigDecimal.valueOf(Math.abs(a.getQuantity())));
            Draft dr = new Draft(d, doc.t("je.stock", "Stock {0}: {1} {2}", doc.t("reason." + a.getReason().name(), a.getReason().getLabel()),
                    String.valueOf(a.getQuantity()), a.getProductName()),
                    "STOCK", a.getRefNo());
            if (a.getQuantity() > 0) dr.add("INVENTORY", v).add("OTHER_INCOME", v.negate());
            else dr.add("LOSSES", v).add("INVENTORY", v.negate());
            drafts.add(dr);
        }

        // Opening balances of cash registers and bank accounts
        for (CashAccount c : cashAccounts.findAll()) {
            if (c.getOpeningBalance().signum() == 0 || c.getOpeningDate().isBefore(from) || c.getOpeningDate().isAfter(to)) continue;
            drafts.add(new Draft(c.getOpeningDate(), doc.t("je.opening", "Opening balance {0}", c.getName()), "OPENING", c.getName())
                    .add(c.isBank() ? "BANK" : "CASH", openingBase(c)).add("OPENING", openingBase(c).negate()));
        }

        int n = 0;
        for (Draft d : drafts) {
            if (save(d, user.getUsername()) != null) n++;
        }
        audit.log("SYNC", "Accounting", from + " – " + to, n + " automatic entries");
        return n;
    }

    /** The money account a payroll or tax payment went through (Cash & Bank movement), or the bank. */
    private String moduleMoneyAccount(String origin, String period) {
        for (MoneyMovement m : moves.findAll()) {
            if (origin.equals(m.getOrigin()) && period.equals(m.getRefNo())) return m.getAccount().isBank() ? "BANK" : "CASH";
        }
        return "BANK";
    }

    private AccEntry save(Draft d, String user) {
        AccEntry e = new AccEntry();
        for (var x : d.amounts.entrySet()) {
            BigDecimal v = x.getValue();
            if (v.signum() == 0) continue;
            AccLine l = new AccLine();
            l.setEntry(e);
            l.setAccount(x.getKey());
            l.setDebit(v.signum() > 0 ? v : BigDecimal.ZERO);
            l.setCredit(v.signum() < 0 ? v.negate() : BigDecimal.ZERO);
            e.getLines().add(l);
        }
        if (e.getLines().size() < 2) return null;
        e.setEntryNo(String.format("JE-%06d", entries.nextNo().longValue()));
        e.setEntryDate(d.date);
        e.setDescription(d.description.length() > 300 ? d.description.substring(0, 300) : d.description);
        e.setSource(d.source);
        e.setSourceRef(d.ref);
        e.setCreatedBy(user);
        return entries.save(e);
    }

    // ------------------------------------------------------- year-end closing

    /**
     * Closes a year: moves the year's income and expenses into 108 (retained
     * result), dated 31 December. Done again, it replaces the earlier closing.
     * Returns the year's result.
     */
    @Transactional
    public BigDecimal closeYear(int year, AppUser user) {
        LocalDate from = LocalDate.of(year, 1, 1), to = LocalDate.of(year, 12, 31);
        lock.check(to);
        entries.deleteAll(entries.closingOf(String.valueOf(year)));
        entries.flush();
        Draft d = new Draft(to, doc.t("je.closing", "Year-end closing {0}", String.valueOf(year)), "CLOSING", String.valueOf(year));
        BigDecimal result = BigDecimal.ZERO;
        for (Object[] r : entries.totalsByAccountNoClosing(from, to)) {
            AccAccount a = account(((Number) r[0]).longValue());
            if (a.getType() != AccAccount.Type.INCOME && a.getType() != AccAccount.Type.EXPENSE) continue;
            BigDecimal net = new BigDecimal(r[1].toString()).subtract(new BigDecimal(r[2].toString()));
            d.add(a, net.negate());
            result = result.subtract(net);
        }
        d.add("OWNER", result.negate());
        save(d, user.getUsername());
        audit.log("CLOSE_YEAR", "Accounting", String.valueOf(year), "result " + Money.format(result));
        return Money.round(result);
    }

    // ------------------------------------------------------------ manual

    public AccEntry entry(Long id) {
        return entries.findById(id).orElseThrow(() -> new NotFoundException("Entry not found"));
    }

    @Transactional
    public AccEntry saveManual(Long id, LocalDate date, String description, List<ManualLine> ls, AppUser user) {
        if (date == null || description == null || description.isBlank()) {
            throw new OrderException("Enter the date and a description.");
        }
        List<ManualLine> used = ls.stream().filter(l -> l.accountId() != null
                && (Money.nz(l.debit()).signum() != 0 || Money.nz(l.credit()).signum() != 0)).toList();
        if (used.size() < 2) {
            throw new OrderException("An entry needs at least two lines.");
        }
        BigDecimal dr = BigDecimal.ZERO, cr = BigDecimal.ZERO;
        for (ManualLine l : used) {
            if (Money.nz(l.debit()).signum() < 0 || Money.nz(l.credit()).signum() < 0
                    || (Money.nz(l.debit()).signum() > 0 && Money.nz(l.credit()).signum() > 0)) {
                throw new OrderException("Each line has either a debit or a credit, not both, and no negative amounts.");
            }
            dr = dr.add(Money.nz(l.debit()));
            cr = cr.add(Money.nz(l.credit()));
        }
        if (Money.round(dr).compareTo(Money.round(cr)) != 0) {
            throw new OrderException("Debits (" + Money.format(dr) + ") and credits (" + Money.format(cr) + ") must be equal.");
        }
        lock.check(date);
        AccEntry e = id == null ? new AccEntry() : entry(id);
        if (id != null) {
            lock.check(e.getEntryDate());
        }
        if (!e.isManual() && id != null) {
            throw new OrderException("Automatic entries can't be edited; change the source instead.");
        }
        if (id == null) {
            e.setEntryNo(String.format("JE-%06d", entries.nextNo().longValue()));
            e.setSource("MANUAL");
            e.setCreatedBy(user.getUsername());
        }
        e.setEntryDate(date);
        e.setDescription(description.trim());
        e.getLines().clear();
        for (ManualLine l : used) {
            AccLine x = new AccLine();
            x.setEntry(e);
            x.setAccount(account(l.accountId()));
            x.setDebit(Money.round(Money.nz(l.debit())));
            x.setCredit(Money.round(Money.nz(l.credit())));
            x.setNote(l.note() == null || l.note().isBlank() ? null : l.note().trim());
            e.getLines().add(x);
        }
        AccEntry saved = entries.save(e);
        audit.log(id == null ? "CREATE" : "UPDATE", "Journal entry", saved.getEntryNo(), Money.format(dr) + " " + saved.getDescription());
        return saved;
    }

    @Transactional
    public void deleteManual(Long id) {
        AccEntry e = entry(id);
        if (!e.isManual()) {
            throw new OrderException("Automatic entries are rebuilt from their source; they can't be deleted here.");
        }
        lock.check(e.getEntryDate());
        entries.delete(e);
        audit.log("DELETE", "Journal entry", e.getEntryNo(), e.getDescription());
    }

    // ------------------------------------------------------------- reports

    @Transactional(readOnly = true)
    public List<AccEntry> journal(LocalDate from, LocalDate to, String source) {
        return entries.between(from, to).stream().filter(e -> source == null || source.isBlank() || source.equals(e.getSource())).toList();
    }

    private Map<Long, BigDecimal[]> totals(LocalDate from, LocalDate to) {
        Map<Long, BigDecimal[]> m = new HashMap<>();
        for (Object[] r : entries.totalsByAccount(from, to)) {
            m.put(((Number) r[0]).longValue(), new BigDecimal[] {new BigDecimal(r[1].toString()), new BigDecimal(r[2].toString())});
        }
        return m;
    }

    /** Balance as debit − credit. */
    private static BigDecimal net(BigDecimal[] t) {
        return t == null ? BigDecimal.ZERO : t[0].subtract(t[1]);
    }

    /** The general ledger of every account that has an opening balance or movements in the period. */
    @Transactional(readOnly = true)
    public List<Ledger> ledgerAll(LocalDate from, LocalDate to) {
        List<Ledger> out = new ArrayList<>();
        for (AccAccount a : accounts()) {
            Ledger l = ledger(a.getId(), from, to);
            if (!l.rows().isEmpty() || l.opening().signum() != 0) out.add(l);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Ledger ledger(Long accountId, LocalDate from, LocalDate to) {
        AccAccount a = account(accountId);
        BigDecimal opening = net(totals(EVER, from.minusDays(1)).get(accountId));
        BigDecimal run = opening, dr = BigDecimal.ZERO, cr = BigDecimal.ZERO;
        List<LedgerRow> rows = new ArrayList<>();
        for (AccLine l : entries.ledger(accountId, from, to)) {
            run = run.add(l.getDebit()).subtract(l.getCredit());
            dr = dr.add(l.getDebit());
            cr = cr.add(l.getCredit());
            rows.add(new LedgerRow(l, run));
        }
        return new Ledger(a, opening, rows, dr, cr, run);
    }

    @Transactional(readOnly = true)
    public Trial trial(LocalDate from, LocalDate to) {
        Map<Long, BigDecimal[]> before = totals(EVER, from.minusDays(1));
        Map<Long, BigDecimal[]> period = totals(from, to);
        List<TrialRow> rows = new ArrayList<>();
        BigDecimal dr = BigDecimal.ZERO, cr = BigDecimal.ZERO, oDr = BigDecimal.ZERO, oCr = BigDecimal.ZERO, cDr = BigDecimal.ZERO, cCr = BigDecimal.ZERO;
        for (AccAccount a : accounts()) {
            BigDecimal o = net(before.get(a.getId()));
            BigDecimal[] p = period.getOrDefault(a.getId(), new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal c = o.add(p[0]).subtract(p[1]);
            if (o.signum() == 0 && p[0].signum() == 0 && p[1].signum() == 0) continue;
            rows.add(new TrialRow(a, o, p[0], p[1], c));
            dr = dr.add(p[0]); cr = cr.add(p[1]);
            if (o.signum() > 0) oDr = oDr.add(o); else oCr = oCr.add(o.negate());
            if (c.signum() > 0) cDr = cDr.add(c); else cCr = cCr.add(c.negate());
        }
        return new Trial(rows, dr, cr, oDr, oCr, cDr, cCr);
    }

    /** Income statement for the period, and the balance sheet at its end. */
    @Transactional(readOnly = true)
    public Statements statements(LocalDate from, LocalDate to) {
        Map<Long, BigDecimal[]> period = new HashMap<>();
        for (Object[] r : entries.totalsByAccountNoClosing(from, to)) {
            period.put(((Number) r[0]).longValue(), new BigDecimal[] {new BigDecimal(r[1].toString()), new BigDecimal(r[2].toString())});
        }
        Map<Long, BigDecimal[]> toDate = totals(EVER, to);
        List<StatementRow> inc = new ArrayList<>(), exp = new ArrayList<>(), as = new ArrayList<>(), li = new ArrayList<>(), eq = new ArrayList<>();
        BigDecimal ti = BigDecimal.ZERO, te = BigDecimal.ZERO, ta = BigDecimal.ZERO, tl = BigDecimal.ZERO, teq = BigDecimal.ZERO, resToDate = BigDecimal.ZERO;
        for (AccAccount a : accounts()) {
            BigDecimal p = net(period.get(a.getId()));
            BigDecimal b = net(toDate.get(a.getId()));
            switch (a.getType()) {
                case INCOME -> { if (p.signum() != 0) { inc.add(new StatementRow(a, p.negate())); ti = ti.add(p.negate()); } resToDate = resToDate.subtract(b); }
                case EXPENSE -> { if (p.signum() != 0) { exp.add(new StatementRow(a, p)); te = te.add(p); } resToDate = resToDate.subtract(b); }
                case ASSET -> { if (b.signum() != 0) { as.add(new StatementRow(a, b)); ta = ta.add(b); } }
                case LIABILITY -> { if (b.signum() != 0) { li.add(new StatementRow(a, b.negate())); tl = tl.add(b.negate()); } }
                case EQUITY -> { if (b.signum() != 0) { eq.add(new StatementRow(a, b.negate())); teq = teq.add(b.negate()); } }
            }
        }
        return new Statements(inc, exp, ti, te, ti.subtract(te), as, li, eq, ta, tl, teq, resToDate);
    }

    /** VAT book: every sale (or purchase) and return of the month, with the party's NIPT. */
    @Transactional(readOnly = true)
    public VatBook vatBook(OrderType type, YearMonth m) {
        Map<String, List<OrderLine>> groups = new LinkedHashMap<>();
        for (OrderLine l : lines.findLines(type, null, m.atDay(1).atStartOfDay(), m.plusMonths(1).atDay(1).atStartOfDay())) {
            groups.computeIfAbsent(l.getOrderCode() + "#" + (l.getReturnNo() == null ? 0 : l.getReturnNo()), k -> new ArrayList<>()).add(l);
        }
        List<VatRow> rows = new ArrayList<>();
        Map<String, String> tins = new HashMap<>();
        for (List<OrderLine> g : groups.values()) {
            OrderLine f = g.get(0);
            String party = type == OrderType.SALE ? f.getBuyerName() : f.getProviderName();
            String tin = tins.computeIfAbsent(party, p -> {
                try {
                    PartyInfo i = type == OrderType.SALE ? parties.buyer(OrderType.SALE, p) : parties.provider(OrderType.PURCHASE, p);
                    return i == null ? null : i.tin();
                } catch (RuntimeException e) { return null; }
            });
            BigDecimal nv = sum(g, OrderLine::getValueNoVat), v = sum(g, OrderLine::getVatValue);
            int rate = nv.signum() == 0 ? 0 : v.multiply(BigDecimal.valueOf(100)).divide(nv, 0, java.math.RoundingMode.HALF_UP).intValue();
            String zero = type == OrderType.SALE ? clientRepo.findByName(party).map(com.fms.domain.Client::getVatBook).orElse("EXPORT") : "EXEMPT";
            String kind = "LOCAL", number = f.getOrderCode();
            if (type == OrderType.PURCHASE) {             // the supplier's own invoice number, and the purchase type
                var pi = infoRepo.findByOrderTypeAndOrderCode(OrderType.PURCHASE, f.getOrderCode()).orElse(null);
                if (pi != null) {
                    kind = pi.getPurchaseKind();
                    if (pi.getSupplierInvoice() != null) number = pi.getSupplierInvoice();
                }
            }
            rows.add(new VatRow(g.stream().map(l -> l.getCreatedAt().toLocalDate()).min(Comparator.naturalOrder()).get(),
                    number + (f.isReturn() ? "-" + f.getReturnNo() : ""), party, tin, nv, v, nv.add(v), f.isReturn(), rate, zero, kind));
        }
        rows.sort(Comparator.comparing(VatRow::date).thenComparing(VatRow::number));
        return new VatBook(type, m, rows, rows.stream().map(VatRow::noVat).reduce(BigDecimal.ZERO, BigDecimal::add),
                rows.stream().map(VatRow::vat).reduce(BigDecimal.ZERO, BigDecimal::add),
                rows.stream().map(VatRow::total).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal sum(List<OrderLine> ls, java.util.function.Function<OrderLine, BigDecimal> f) {
        return ls.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
