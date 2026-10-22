package com.fms.service;

import com.fms.domain.*;
import com.fms.repo.MoneyMovementRepository;
import java.math.BigDecimal;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bank statement import (CSV from the bank) into a Cash & Bank account. Each line is
 * matched to a movement already in Arkon, to an unpaid invoice it pays, or added as money
 * in / out; lines once imported are recognised and never imported twice.
 */
@Service
public class BankImportService {

    /** The file: its columns and rows. */
    public record Csv(List<String> header, List<List<String>> rows) implements java.io.Serializable { }
    /** Which column is what (-1 = none). Either amount, or in / out. */
    public record Mapping(int date, int description, int amount, int in, int out) implements java.io.Serializable { }
    /** One statement line with its suggestion. action: DONE (imported before), MATCH, PAY, NEW. */
    public record Line(int index, LocalDate date, String description, BigDecimal amount, String ref,
                       String action, Long movementId, String orderType, String orderCode, String note) implements java.io.Serializable {
        public boolean in() { return amount.signum() > 0; }
    }
    /** What to do with a line: MATCH, PAY, NEW (+ category), IGNORE. */
    public record Decision(int index, String action, MoneyCategory category) { }

    private final MoneyMovementRepository moves;
    private final CashService cash;
    private final PaymentService payments;
    private final OrderService orders;

    public BankImportService(MoneyMovementRepository moves, CashService cash, PaymentService payments, OrderService orders) {
        this.moves = moves; this.cash = cash; this.payments = payments; this.orders = orders;
    }

    // ------------------------------------------------------------------ reading the file

    public static Csv parse(byte[] bytes) {
        String text = decode(bytes);
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        List<String> lines = new ArrayList<>(Arrays.asList(text.split("\\r?\\n")));
        lines.removeIf(String::isBlank);
        if (lines.size() < 2) throw new OrderException("The file has no lines.");
        char sep = separator(lines.get(0));
        List<List<String>> rows = new ArrayList<>();
        for (String l : lines) rows.add(split(l, sep));
        // the header is the first row that has a date-like or "date" column... usually the first row
        int h = 0;
        for (int i = 0; i < Math.min(15, rows.size()); i++) {
            String joined = String.join(" ", rows.get(i)).toLowerCase(Locale.ROOT);
            if (joined.contains("dat") && (joined.contains("shum") || joined.contains("amount") || joined.contains("debi") || joined.contains("kredi")
                    || joined.contains("credit") || joined.contains("vler"))) { h = i; break; }
        }
        return new Csv(rows.get(h), new ArrayList<>(rows.subList(h + 1, rows.size())));
    }

    private static String decode(byte[] b) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(b)).toString();
        } catch (CharacterCodingException e) {
            return new String(b, Charset.forName("windows-1252"));
        }
    }

    private static char separator(String line) {
        int semi = count(line, ';'), comma = count(line, ','), tab = count(line, '\t');
        return tab >= semi && tab >= comma && tab > 0 ? '\t' : semi >= comma ? ';' : ',';
    }

    private static int count(String s, char c) { int n = 0; boolean q = false; for (char x : s.toCharArray()) { if (x == '"') q = !q; else if (x == c && !q) n++; } return n; }

    static List<String> split(String line, char sep) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (q && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; } else q = !q;
            } else if (c == sep && !q) { out.add(cur.toString().trim()); cur.setLength(0); }
            else cur.append(c);
        }
        out.add(cur.toString().trim());
        return out;
    }

    /** Guesses the columns from the headers (Albanian and English). */
    public static Mapping guess(List<String> header) {
        int date = -1, desc = -1, amount = -1, in = -1, out = -1;
        for (int i = 0; i < header.size(); i++) {
            String h = header.get(i).toLowerCase(Locale.ROOT);
            if (date < 0 && (h.contains("dat") || h.contains("date"))) date = i;
            else if (in < 0 && (h.contains("kredi") || h.contains("credit") || h.contains("hyrj") || h.equals("in"))) in = i;
            else if (out < 0 && (h.contains("debi") || h.contains("debit") || h.contains("dalj") || h.equals("out"))) out = i;
            else if (amount < 0 && (h.contains("shum") || h.contains("amount") || h.contains("vler"))) amount = i;
            else if (desc < 0 && (h.contains("pershkrim") || h.contains("përshkrim") || h.contains("desc") || h.contains("detaj") || h.contains("narr")
                    || h.contains("pershkr") || h.contains("referenc") || h.contains("qellim") || h.contains("qëllim") || h.contains("details"))) desc = i;
        }
        if (in >= 0 && out >= 0) amount = -1;
        return new Mapping(date, desc, amount, in, out);
    }

    static BigDecimal number(String s) {
        if (s == null) return null;
        String t = s.replace(" ", "").replace("\u00a0", "").replace("ALL", "").replace("EUR", "").replace("USD", "").replace("Lek", "").trim();
        if (t.isEmpty()) return null;
        boolean neg = t.startsWith("(") && t.endsWith(")");
        t = t.replace("(", "").replace(")", "");
        if (t.matches("-?\\d{1,3}(\\.\\d{3})+(,\\d+)?") || t.matches("-?\\d+,\\d{1,2}")) t = t.replace(".", "").replace(',', '.');   // 1.234,56 / 12,50
        else t = t.replace(",", "");                                                                                                    // 1,234.56
        try { BigDecimal v = new BigDecimal(t); return neg ? v.negate() : v; } catch (NumberFormatException e) { return null; }
    }

    private static final List<DateTimeFormatter> DATES = List.of(
            DateTimeFormatter.ofPattern("d.M.yyyy"), DateTimeFormatter.ofPattern("d/M/yyyy"), DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("d.M.yy"), DateTimeFormatter.ofPattern("d/M/yy"), DateTimeFormatter.ofPattern("yyyy/M/d"));

    static LocalDate date(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim().split("[ T]")[0];
        for (DateTimeFormatter f : DATES) { try { return LocalDate.parse(t, f); } catch (Exception ignored) { } }
        return null;
    }

    /** The statement lines (with an amount and a date) of the file, in + / out −. */
    public static List<Line> lines(Csv csv, Mapping m, Long accountId) {
        List<Line> out = new ArrayList<>();
        int i = 0;
        for (List<String> r : csv.rows()) {
            i++;
            LocalDate d = m.date() >= 0 && m.date() < r.size() ? date(r.get(m.date())) : null;
            BigDecimal amt;
            if (m.amount() >= 0) amt = m.amount() < r.size() ? number(r.get(m.amount())) : null;
            else {
                BigDecimal a = m.in() >= 0 && m.in() < r.size() ? number(r.get(m.in())) : null;
                BigDecimal b = m.out() >= 0 && m.out() < r.size() ? number(r.get(m.out())) : null;
                amt = a != null && a.signum() != 0 ? a.abs() : b != null && b.signum() != 0 ? b.abs().negate() : null;
            }
            if (d == null || amt == null || amt.signum() == 0) continue;
            String desc = m.description() >= 0 && m.description() < r.size() ? r.get(m.description()) : "";
            out.add(new Line(i, d, desc, amt.setScale(2, java.math.RoundingMode.HALF_UP), ref(accountId, d, amt, desc), "NEW", null, null, null, null));
        }
        return out;
    }

    /** A fingerprint of a statement line in one account (date, amount, text). */
    static String ref(Long accountId, LocalDate d, BigDecimal amt, String desc) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest((accountId + "|" + d + "|" + amt.stripTrailingZeros().toPlainString() + "|" + desc.trim().toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h, 0, 20);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------ suggestions

    /** For each line: already imported, a movement already in Arkon (same direction and amount, within 5 days), an invoice it pays, or new. */
    @Transactional(readOnly = true)
    public List<Line> suggest(Long accountId, List<Line> lines) {
        if (lines.isEmpty()) return lines;
        LocalDate min = lines.stream().map(Line::date).min(Comparator.naturalOrder()).get().minusDays(5);
        LocalDate max = lines.stream().map(Line::date).max(Comparator.naturalOrder()).get().plusDays(5);
        List<MoneyMovement> pool = new ArrayList<>(moves.findByAccountIdAndMovedOnBetween(accountId, min, max));
        pool.removeIf(mv -> mv.getBankRef() != null);
        var unpaidSales = payments.debts(OrderType.SALE).orders();
        var unpaidPurchases = payments.debts(OrderType.PURCHASE).orders();
        Set<String> usedOrders = new HashSet<>();
        List<Line> out = new ArrayList<>();
        for (Line l : lines) {
            if (moves.existsByBankRef(l.ref())) { out.add(with(l, "DONE", null, null, null, null)); continue; }
            MoneyMovement hit = null;
            for (MoneyMovement mv : pool) {
                if (mv.isIn() == l.in() && mv.getAccountAmount().compareTo(l.amount().abs()) == 0
                        && Math.abs(java.time.temporal.ChronoUnit.DAYS.between(mv.getMovedOn(), l.date())) <= 5) {
                    if (hit == null || Math.abs(java.time.temporal.ChronoUnit.DAYS.between(mv.getMovedOn(), l.date()))
                            < Math.abs(java.time.temporal.ChronoUnit.DAYS.between(hit.getMovedOn(), l.date()))) hit = mv;
                }
            }
            if (hit != null) {
                pool.remove(hit);
                out.add(with(l, "MATCH", hit.getId(), null, null, hit.getDescription()));
                continue;
            }
            var candidates = l.in() ? unpaidSales : unpaidPurchases;
            PaymentService.DebtRow inv = null;
            for (var d : candidates) {
                if (d.remaining().compareTo(l.amount().abs()) == 0 && !usedOrders.contains(d.code())) {
                    String t = l.description().toLowerCase(Locale.ROOT);
                    boolean named = t.contains(d.code()) || t.contains(d.party().toLowerCase(Locale.ROOT));
                    if (inv == null || named) inv = d;
                    if (named) break;
                }
            }
            if (inv != null) {
                usedOrders.add(inv.code());
                out.add(with(l, "PAY", null, l.in() ? "SALE" : "PURCHASE", inv.code(), inv.party()));
            } else {
                out.add(l);
            }
        }
        return out;
    }

    private static Line with(Line l, String action, Long mv, String type, String code, String note) {
        return new Line(l.index(), l.date(), l.description(), l.amount(), l.ref(), action, mv, type, code, note);
    }

    // ------------------------------------------------------------------ saving

    public record Done(int matched, int paid, int created, int ignored) { }

    /** Applies the chosen actions, all or nothing. Each saved line keeps its fingerprint so it isn't imported again. */
    @Transactional
    public Done apply(Long accountId, List<Line> lines, Map<Integer, Decision> decisions, AppUser user) {
        int m = 0, p = 0, c = 0, ig = 0;
        for (Line l : lines) {
            if ("DONE".equals(l.action())) continue;
            Decision d = decisions.get(l.index());
            if (d == null || "IGNORE".equals(d.action())) { ig++; continue; }
            if (moves.existsByBankRef(l.ref())) continue;
            switch (d.action()) {
                case "MATCH" -> {
                    MoneyMovement mv = moves.findById(l.movementId()).orElseThrow(() -> new OrderException("A matched movement no longer exists; read the file again."));
                    mv.setBankRef(l.ref());
                    moves.save(mv);
                    m++;
                }
                case "PAY" -> {
                    OrderType t = OrderType.valueOf(l.orderType());
                    var order = orders.load(t, l.orderCode());
                    if (order.isEmpty()) throw new OrderException("Order " + l.orderCode() + " no longer exists; read the file again.");
                    Payment pay = payments.add(order, l.date(), l.amount().abs(), Payment.Method.BANK, l.description(), Payment.Kind.PAYMENT, accountId, user);
                    for (MoneyMovement mv : moves.findByPaymentId(pay.getId())) { mv.setBankRef(l.ref()); moves.save(mv); }
                    p++;
                }
                case "NEW" -> {
                    MoneyCategory cat = d.category();
                    if (cat == null || !cat.isManual() || cat.isIn() != l.in()) {
                        throw new OrderException("Choose a category for each new line (money in or out).");
                    }
                    MoneyMovement mv = cash.addEntry(accountId, l.date(), cat, l.amount().abs(), l.description(), null, user);
                    mv.setBankRef(l.ref());
                    moves.save(mv);
                    c++;
                }
                default -> ig++;
            }
        }
        return new Done(m, p, c, ig);
    }
}
