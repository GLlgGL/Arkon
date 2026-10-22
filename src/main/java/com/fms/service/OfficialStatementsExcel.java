package com.fms.service;

import com.fms.domain.AccAccount;
import com.fms.domain.AppConfig;
import com.fms.repo.AccAccountRepository;
import com.fms.repo.AccEntryRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The official statements written into the official template (the accountant's file):
 * cover page, the four statements (this year in column B, last year in D) and the
 * account sheets (balances of classes 1–5, movements of classes 6–7). The template's
 * own totals and checks stay as formulas. Amounts in whole lek.
 */
@Component
public class OfficialStatementsExcel {

    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final OfficialStatements statements;
    private final AccAccountRepository accounts;
    private final AccEntryRepository entries;

    public OfficialStatementsExcel(OfficialStatements statements, AccAccountRepository accounts, AccEntryRepository entries) {
        this.statements = statements; this.accounts = accounts; this.entries = entries;
    }

    @Transactional(readOnly = true)
    public byte[] write(int year, AppConfig c, String company, String tin, String address, LocalDate approved) {
        var rep = statements.report(year);
        try (InputStream in = new ClassPathResource(OfficialStatements.TEMPLATE).getInputStream(); XSSFWorkbook wb = new XSSFWorkbook(in)) {
            // cover
            Sheet k = wb.getSheet("Kapaku");
            set(k, "E2", company + (c.getCoLegalForm() == null ? "" : " " + c.getCoLegalForm()));
            set(k, "E3", tin);
            set(k, "E4", address);
            set(k, "E6", c.getCoFounded() == null ? "" : c.getCoFounded().format(DMY));
            set(k, "E8", c.getCoActivity() == null ? "" : c.getCoActivity());
            set(k, "A30", "Viti   " + year);
            set(k, "G48", "01.01." + year);
            set(k, "G49", "31.12." + year);
            set(k, "G51", (approved == null ? LocalDate.now() : approved).format(DMY));
            for (String sh : List.of(OfficialStatements.POS, OfficialStatements.PERF, OfficialStatements.CF, OfficialStatements.EQ)) {
                set(wb.getSheet(sh), "A1", "Pasqyrat financiare te vitit " + year);
            }
            set(wb.getSheet(OfficialStatements.POS), "A2", company);
            set(wb.getSheet(OfficialStatements.POS), "A3", "NIPT " + tin);
            // balance sheet and income statement: data lines only (totals are the template's formulas)
            fill(wb.getSheet(OfficialStatements.POS), rep.position());
            fill(wb.getSheet(OfficialStatements.PERF), rep.performance());
            // cash flow (B11 stays the template's link to the result)
            Sheet cf = wb.getSheet(OfficialStatements.CF);
            for (var f : rep.cashFlow()) {
                if (f.total() || f.row() == 11) continue;
                num(cf, "B" + f.row(), f.current());
                num(cf, "D" + f.row(), f.previous());
            }
            num(cf, "D11", rep.cashFlow().get(0).previous());
            // changes in equity: data rows, columns B..H
            Sheet eq = wb.getSheet(OfficialStatements.EQ);
            for (var r : rep.equity()) {
                if (r.row() == 24 || r.row() == 37) continue;          // the template sums these
                for (int i = 0; i < 7; i++) num(eq, (char) ('B' + i) + String.valueOf(r.row()), r.cols()[i]);
            }
            // account sheets
            accountSheet(wb.getSheet("LLOGARITE 1-5"), year, false);
            accountSheet(wb.getSheet("LLOGARITE 6-7"), year, true);
            wb.setForceFormulaRecalculation(true);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void fill(Sheet sh, List<OfficialStatements.Amount> amounts) {
        for (var a : amounts) {
            if (a.line().total()) continue;
            Row row = sh.getRow(a.line().row() - 1);
            if (row != null && row.getCell(1) != null && row.getCell(1).getCellType() == CellType.FORMULA) continue;
            num(sh, "B" + a.line().row(), a.current());
            num(sh, "D" + a.line().row(), a.previous());
        }
    }

    /** Sheets listing account codes in B with Debi (D) / Kredi (E): year-end balances, or the year's movements. */
    private void accountSheet(Sheet sh, int year, boolean movements) {
        if (sh == null) return;
        Map<String, BigDecimal> byCode = new HashMap<>();
        Map<Long, AccAccount> byId = new HashMap<>();
        accounts.findAll().forEach(a -> byId.put(a.getId(), a));
        List<Object[]> rows = movements ? entries.totalsByAccountNoClosing(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
                : entries.totalsByAccount(LocalDate.of(1900, 1, 1), LocalDate.of(year, 12, 31));
        for (Object[] r : rows) {
            AccAccount a = byId.get((Long) r[0]);
            if (a != null) byCode.merge(a.getCode(), ((BigDecimal) r[1]).subtract((BigDecimal) r[2]), BigDecimal::add);
        }
        for (int i = 1; i <= sh.getLastRowNum(); i++) {
            Row row = sh.getRow(i);
            if (row == null || row.getCell(1) == null) continue;
            Cell c = row.getCell(1);
            String code = c.getCellType() == CellType.NUMERIC ? String.valueOf((long) c.getNumericCellValue())
                    : c.getCellType() == CellType.STRING ? c.getStringCellValue().trim() : "";
            if (!code.matches("\\d{2,5}")) continue;
            BigDecimal v = BigDecimal.ZERO;
            for (var e : byCode.entrySet()) if (e.getKey().startsWith(code)) v = v.add(e.getValue());
            num(sh, "D" + (i + 1), v.signum() > 0 ? v : null);
            num(sh, "E" + (i + 1), v.signum() < 0 ? v.negate() : null);
        }
    }

    private static Cell cell(Sheet sh, String ref) {
        var r = new org.apache.poi.ss.util.CellReference(ref);
        Row row = sh.getRow(r.getRow()) == null ? sh.createRow(r.getRow()) : sh.getRow(r.getRow());
        Cell c = row.getCell(r.getCol());
        return c == null ? row.createCell(r.getCol()) : c;
    }

    private static void set(Sheet sh, String ref, String v) { if (sh != null) cell(sh, ref).setCellValue(v == null ? "" : v); }

    private static void num(Sheet sh, String ref, BigDecimal v) {
        if (sh == null) return;
        Cell c = cell(sh, ref);
        if (v == null || v.signum() == 0) c.setBlank();
        else c.setCellValue(v.setScale(0, RoundingMode.HALF_UP).doubleValue());
    }
}
