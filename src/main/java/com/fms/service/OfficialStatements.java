package com.fms.service;

import com.fms.domain.AccAccount;
import com.fms.repo.AccAccountRepository;
import com.fms.repo.AccEntryRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The official financial statements (Law 25/2018): Pasqyra e Pozicionit Financiar, Pasqyra e
 * Performancës (by nature), Pasqyra e Fluksit të Mjeteve Monetare (indirect method) and Pasqyra
 * e Ndryshimeve në Kapital, for a year and the year before, from the journal.
 *
 * The lines and the accounts that feed them come from the official template itself (column F),
 * so the template is the single source of truth. An account can be sent to another line with its
 * "line in the official statements" (e.g. B45, P19).
 */
@Service
public class OfficialStatements {

    public static final String TEMPLATE = "fin-statements/Pasqyrat.xlsx";
    public static final String POS = "Pasqyra e Pozicionit", PERF = "Pasqyra e Performances", CF = "Cash Flow", EQ = "Pasqyra e Kapitalit";

    /** One line of the template: id (B45 / P19), row, label, the account codes it lists, whether it's a total. */
    public record Line(String id, int row, String label, List<String> codes, boolean total, boolean heading) { }
    /** A line with its amounts this year and last year, and the accounts in it. */
    public record Amount(Line line, BigDecimal current, BigDecimal previous, List<String> accounts) { }
    /** Cash flow line (row of the Cash Flow sheet). */
    public record FlowLine(int row, String label, BigDecimal current, BigDecimal previous, boolean total) { }
    /** Changes in equity: rows (template row, label) x columns B..H. */
    public record EquityRow(int row, String label, BigDecimal[] cols, boolean total) {
        public BigDecimal sum() { return Arrays.stream(cols).reduce(BigDecimal.ZERO, BigDecimal::add); }
    }
    public record Report(int year, List<Amount> position, List<Amount> performance, List<FlowLine> cashFlow,
                         List<String> equityHeads, List<EquityRow> equity, List<String> warnings,
                         BigDecimal positionCheck, BigDecimal positionCheckPrev, BigDecimal cashResidual) { }

    private static final LocalDate START = LocalDate.of(1900, 1, 1);
    private static final Pattern CODE = Pattern.compile("\\d{2,5}");

    private final AccAccountRepository accounts;
    private final AccEntryRepository entries;
    private volatile Map<String, List<Line>> template;

    public OfficialStatements(AccAccountRepository accounts, AccEntryRepository entries) {
        this.accounts = accounts;
        this.entries = entries;
    }

    // ------------------------------------------------------------------ the template

    /** The lines of the balance sheet (B) and income statement (P), read once from the template. */
    public Map<String, List<Line>> lines() {
        if (template == null) {
            try (InputStream in = new ClassPathResource(TEMPLATE).getInputStream(); XSSFWorkbook wb = new XSSFWorkbook(in)) {
                Map<String, List<Line>> m = new LinkedHashMap<>();
                m.put("B", read(wb.getSheet(POS), "B", 8, 111));
                m.put("P", read(wb.getSheet(PERF), "P", 9, 57));
                template = m;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return template;
    }

    private static List<Line> read(Sheet sh, String tag, int from, int to) {
        List<Line> out = new ArrayList<>();
        for (int r = from; r <= to; r++) {
            Row row = sh.getRow(r - 1);
            if (row == null) continue;
            String label = text(row.getCell(0));
            if (label.isBlank()) continue;
            List<String> codes = new ArrayList<>();
            Matcher m = CODE.matcher(text(row.getCell(5)));
            while (m.find()) codes.add(m.group());
            Cell b = row.getCell(1);
            boolean total = b != null && b.getCellType() == CellType.FORMULA;
            boolean heading = !total && codes.isEmpty() && isHeading(label);
            out.add(new Line(tag + r, r, label.trim(), codes, total, heading));
        }
        return out;
    }

    private static boolean isHeading(String label) {
        String l = label.trim();
        return l.equals(l.toUpperCase()) || l.startsWith("Aktive") || l.startsWith("Detyrime") || l.startsWith("Te drejta")
                || l.startsWith("Inventaret") || l.startsWith("Investime") || l.startsWith("Kapitali dhe") || l.startsWith("Rezerva te tjera")
                || l.startsWith("Te ardhurat nga aktiviteti i") || l.startsWith("Shpenzime financiare") || l.startsWith("Tatimi mbi fitimin") || l.startsWith("Te ardhura te tjera gjitheperfshirese");
    }

    private static String text(Cell c) {
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
            default -> "";
        };
    }

    /** The line an account goes to: its own setting, else the template line that lists the longest matching code. */
    public String lineOf(AccAccount a) {
        if (a.getStmtLine() != null) return a.getStmtLine();
        String code = a.getCode();
        if (code == null || code.isEmpty()) return null;
        char k = code.charAt(0);
        String tag = k == '6' || k == '7' ? "P" : k >= '1' && k <= '5' ? "B" : null;
        if (tag == null) return null;
        String best = null; int len = 0;
        for (Line l : lines().get(tag)) {
            for (String c : l.codes()) {
                if (code.startsWith(c) && c.length() > len) { len = c.length(); best = l.id(); }
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ amounts

    /** Per account: debit − credit, up to a date (all entries) or in a period (without year-end closing). */
    private Map<Long, BigDecimal> balances(LocalDate to) { return net(entries.totalsByAccount(START, to)); }
    private Map<Long, BigDecimal> movements(LocalDate from, LocalDate to) { return net(entries.totalsByAccountNoClosing(from, to)); }

    private static Map<Long, BigDecimal> net(List<Object[]> rows) {
        Map<Long, BigDecimal> m = new HashMap<>();
        for (Object[] r : rows) m.put((Long) r[0], ((BigDecimal) r[1]).subtract((BigDecimal) r[2]));
        return m;
    }

    private record Side(Map<String, BigDecimal> values, Map<String, Set<String>> accounts, List<String> unmapped) { }

    /** Balance sheet at the end of a year: line id -> amount. */
    private Side position(int year, Map<Long, AccAccount> byId) {
        LocalDate end = LocalDate.of(year, 12, 31);
        Map<String, BigDecimal> v = new HashMap<>();
        Map<String, Set<String>> acc = new HashMap<>();
        List<String> unmapped = new ArrayList<>();
        BigDecimal unclosed = BigDecimal.ZERO;             // results still in classes 6 and 7 (credit − debit)
        for (var e : balances(end).entrySet()) {
            AccAccount a = byId.get(e.getKey());
            if (a == null || e.getValue().signum() == 0) continue;
            char k = a.getCode().charAt(0);
            if (k == '6' || k == '7') { unclosed = unclosed.subtract(e.getValue()); continue; }
            String line = lineOf(a);
            if (line == null || !line.startsWith("B")) { unmapped.add(a.getCode() + " " + a.getName()); continue; }
            int row = Integer.parseInt(line.substring(1));
            BigDecimal amount = row <= 57 ? e.getValue() : e.getValue().negate();     // assets: debit − credit
            v.merge(line, amount, BigDecimal::add);
            acc.computeIfAbsent(line, x -> new TreeSet<>()).add(a.getCode());
        }
        BigDecimal result = result(year, byId);
        v.merge("B106", result, BigDecimal::add);                                    // the year's result
        v.merge("B105", unclosed.subtract(result), BigDecimal::add);                 // earlier results not yet closed
        totals("B", v);
        return new Side(v, acc, unmapped);
    }

    /** Income statement of a year: line id -> amount (income +, expenses −). */
    private Side performance(int year, Map<Long, AccAccount> byId) {
        Map<String, BigDecimal> v = new HashMap<>();
        Map<String, Set<String>> acc = new HashMap<>();
        List<String> unmapped = new ArrayList<>();
        for (var e : movements(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)).entrySet()) {
            AccAccount a = byId.get(e.getKey());
            if (a == null || e.getValue().signum() == 0) continue;
            char k = a.getCode().charAt(0);
            if (k != '6' && k != '7') continue;
            String line = lineOf(a);
            if (line == null || !line.startsWith("P")) { unmapped.add(a.getCode() + " " + a.getName()); continue; }
            v.merge(line, e.getValue().negate(), BigDecimal::add);
            acc.computeIfAbsent(line, x -> new TreeSet<>()).add(a.getCode());
        }
        totals("P", v);
        return new Side(v, acc, unmapped);
    }

    private BigDecimal result(int year, Map<Long, AccAccount> byId) {
        BigDecimal r = BigDecimal.ZERO;
        for (var e : movements(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)).entrySet()) {
            AccAccount a = byId.get(e.getKey());
            if (a != null && (a.getCode().charAt(0) == '6' || a.getCode().charAt(0) == '7')) r = r.subtract(e.getValue());
        }
        return r;
    }

    /** The template's totals. */
    private static void totals(String tag, Map<String, BigDecimal> v) {
        if ("B".equals(tag)) {
            put(v, "B33", sum(v, "B", 11, 32));
            put(v, "B55", sum(v, "B", 37, 54));
            put(v, "B57", g(v, "B55").add(g(v, "B33")));
            put(v, "B75", sum(v, "B", 62, 74));
            put(v, "B92", sum(v, "B", 78, 91));
            put(v, "B94", g(v, "B75").add(g(v, "B92")));
            put(v, "B107", sum(v, "B", 97, 106));
            put(v, "B109", g(v, "B107").add(g(v, "B108")));
            put(v, "B111", g(v, "B94").add(g(v, "B109")));
        } else {
            put(v, "P42", sum(v, "P", 9, 41));
            put(v, "P47", g(v, "P42").add(sum(v, "P", 43, 46)));
            put(v, "P55", sum(v, "P", 50, 54));
            put(v, "P57", g(v, "P47").add(g(v, "P55")));
        }
    }

    private static BigDecimal sum(Map<String, BigDecimal> v, String tag, int from, int to) {
        BigDecimal s = BigDecimal.ZERO;
        for (int r = from; r <= to; r++) s = s.add(g(v, tag + r));
        return s;
    }
    private static BigDecimal g(Map<String, BigDecimal> v, String id) { return v.getOrDefault(id, BigDecimal.ZERO); }
    private static void put(Map<String, BigDecimal> v, String id, BigDecimal x) { v.put(id, x); }
    private static BigDecimal neg(BigDecimal x) { return x.negate(); }

    // ------------------------------------------------------------------ cash flow, equity

    private static final int[] CF_ROWS = {11, 15, 16, 32, 33, 34, 35, 36, 37, 40, 49, 52, 54, 61, 64, 66, 67, 69};

    /** Cash flow (indirect) of a year, from its result and the change in the balance sheet. */
    private Map<Integer, BigDecimal> cashFlow(Side perf, Side pos, Side prev) {
        Map<Integer, BigDecimal> c = new HashMap<>();
        Map<String, BigDecimal> p = pos.values(), q = prev.values(), f = perf.values();
        BigDecimal result = g(f, "P47");
        BigDecimal dep = neg(g(f, "P26")), impair = neg(g(f, "P25"));
        BigDecimal rec = neg(d(p, q, 18, 22));
        BigDecimal inv = neg(d(p, q, 24, 30));
        BigDecimal pay = d(p, q, 64, 68).add(d(p, q, 70, 74));
        BigDecimal emp = d(p, q, 69, 69);
        BigDecimal capex = neg(d(p, q, 36, 54).add(dep).add(impair));
        BigDecimal capital = d(p, q, 97, 99);
        BigDecimal loans = d(p, q, 62, 63).add(d(p, q, 78, 78));
        BigDecimal dividends = d(p, q, 101, 106).subtract(result);        // what left equity besides the result
        BigDecimal ops = result.add(dep).add(impair).add(rec).add(inv).add(pay).add(emp);
        BigDecimal fin = capital.add(loans).add(dividends);
        BigDecimal start = g(q, "B11"), end = g(p, "B11");
        BigDecimal other = end.subtract(start).subtract(ops).subtract(capex).subtract(fin);   // anything not covered above
        ops = ops.add(other);
        c.put(11, result); c.put(15, dep); c.put(16, impair); c.put(32, rec); c.put(33, inv); c.put(34, pay); c.put(35, emp); c.put(36, other);
        c.put(37, ops); c.put(40, capex); c.put(49, capex); c.put(52, capital); c.put(54, loans); c.put(61, dividends); c.put(64, fin);
        c.put(66, ops.add(capex).add(fin)); c.put(67, start); c.put(69, end);
        return c;
    }

    private static BigDecimal d(Map<String, BigDecimal> now, Map<String, BigDecimal> before, int from, int to) {
        return sum(now, "B", from, to).subtract(sum(before, "B", from, to));
    }

    /** Equity columns B..H of the template: capital, premium, revaluation, reserves, FX, retained, result. */
    private static BigDecimal[] equityCols(Map<String, BigDecimal> p) {
        return new BigDecimal[] {g(p, "B97"), g(p, "B98"), g(p, "B99"), sum(p, "B", 101, 103), g(p, "B104"), g(p, "B105"), g(p, "B106")};
    }

    // ------------------------------------------------------------------ the report

    @Transactional(readOnly = true)
    public Report report(int year) {
        Map<Long, AccAccount> byId = new HashMap<>();
        accounts.findAll().forEach(a -> byId.put(a.getId(), a));
        Side pos = position(year, byId), posPrev = position(year - 1, byId), pos2 = position(year - 2, byId);
        Side perf = performance(year, byId), perfPrev = performance(year - 1, byId);
        List<Amount> position = amounts(lines().get("B"), pos, posPrev);
        List<Amount> performance = amounts(lines().get("P"), perf, perfPrev);

        Map<Integer, BigDecimal> cf = cashFlow(perf, pos, posPrev), cfPrev = cashFlow(perfPrev, posPrev, pos2);
        Map<Integer, String> cfLabels = cashFlowLabels();
        List<FlowLine> flow = new ArrayList<>();
        for (int r : CF_ROWS) {
            flow.add(new FlowLine(r, cfLabels.getOrDefault(r, ""), cf.get(r), cfPrev.get(r), r == 37 || r == 49 || r == 64 || r == 66 || r == 69));
        }

        BigDecimal[] e2 = equityCols(pos2.values()), e1 = equityCols(posPrev.values()), e0 = equityCols(pos.values());
        BigDecimal r1 = g(perfPrev.values(), "P47"), r0 = g(perf.values(), "P47"), r2 = result(year - 2, byId);
        List<EquityRow> eq = new ArrayList<>();
        eq.add(new EquityRow(10, "Pozicioni financiar më 31.12." + (year - 2), e2, true));
        eq.addAll(equityYear(e2, e1, r1, r2, 14, 19, 20, 21));
        eq.add(new EquityRow(24, "Pozicioni financiar më 31.12." + (year - 1), e1, true));
        eq.addAll(equityYear(e1, e0, r0, r1, 27, 32, 33, 34));
        eq.add(new EquityRow(37, "Pozicioni financiar më 31.12." + year, e0, true));

        List<String> warnings = new ArrayList<>();
        Set<String> un = new TreeSet<>();
        un.addAll(pos.unmapped()); un.addAll(posPrev.unmapped()); un.addAll(perf.unmapped()); un.addAll(perfPrev.unmapped());
        un.forEach(a -> warnings.add("unmapped:" + a));
        BigDecimal check = g(pos.values(), "B57").subtract(g(pos.values(), "B111"));
        BigDecimal checkPrev = g(posPrev.values(), "B57").subtract(g(posPrev.values(), "B111"));
        return new Report(year, position, performance, flow,
                List.of("Kapitali i nënshkruar", "Primi i lidhur me kapitalin", "Rezerva rivlerësimi", "Rezerva të tjera",
                        "Diferenca nga përkthimi i monedhës", "Fitimet e pashpërndara", "Fitimi/(humbja) e periudhës"),
                eq, warnings, check, checkPrev, cf.get(36));
    }

    /** One year of changes in equity: result, capital issued, distributions, other movements. */
    private static List<EquityRow> equityYear(BigDecimal[] start, BigDecimal[] end, BigDecimal result, BigDecimal lastResult,
                                              int rowResult, int rowIssue, int rowDividends, int rowOther) {
        BigDecimal[] res = zeros(), issue = zeros(), div = zeros(), other = zeros();
        res[6] = result;
        issue[0] = end[0].subtract(start[0]);
        issue[1] = end[1].subtract(start[1]);
        other[2] = end[2].subtract(start[2]);
        other[3] = end[3].subtract(start[3]);
        other[4] = end[4].subtract(start[4]);
        other[6] = lastResult.negate();                    // last year's result moves into retained earnings
        other[5] = lastResult;
        div[5] = end[5].subtract(start[5]).subtract(lastResult);   // what else left retained earnings (distributions)
        return List.of(new EquityRow(rowResult, "Fitimi/(humbja) e periudhës", res, false),
                new EquityRow(rowIssue, "Emetim i kapitalit / ndryshime në kapital", issue, false),
                new EquityRow(rowDividends, "Dividendë të shpërndarë / tërheqje", div, false),
                new EquityRow(rowOther, "Përcaktime të tjera (mbartja e rezultatit, rezerva)", other, false));
    }

    private static BigDecimal[] zeros() { BigDecimal[] a = new BigDecimal[7]; Arrays.fill(a, BigDecimal.ZERO); return a; }

    private static List<Amount> amounts(List<Line> ls, Side now, Side before) {
        List<Amount> out = new ArrayList<>();
        for (Line l : ls) {
            out.add(new Amount(l, now.values().get(l.id()), before.values().get(l.id()),
                    new ArrayList<>(now.accounts().getOrDefault(l.id(), Set.of()))));
        }
        return out;
    }

    private Map<Integer, String> cashFlowLabels() {
        Map<Integer, String> m = new HashMap<>();
        try (InputStream in = new ClassPathResource(TEMPLATE).getInputStream(); XSSFWorkbook wb = new XSSFWorkbook(in)) {
            Sheet sh = wb.getSheet(CF);
            for (int r : CF_ROWS) m.put(r, text(sh.getRow(r - 1).getCell(0)).trim());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return m;
    }
}
