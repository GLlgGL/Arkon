package com.fms.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellCopyPolicy;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * The VAT sales and purchase books in the tax office's own Excel format
 * (Libri i Shitjeve / Libri i Blerjeve, version 4). The original templates
 * are filled in: header, one row per invoice from row 13 (rows are added when
 * there are more than ten), and the template's own formulas for VAT and totals.
 * Amounts are whole lek, as in the template.
 */
@Component
public class VatBookExcel {

    private static final int FIRST = 12;          // row 13 (0-based)
    private static final int TEMPLATE_ROWS = 10;  // rows 13..22 in the template

    public byte[] sales(String company, String tin, int year, int month, List<AccountingService.VatRow> rows) {
        return fill("vat-books/SalesBookV4.xlsx", company, tin, year, month, rows, true);
    }

    public byte[] purchases(String company, String tin, int year, int month, List<AccountingService.VatRow> rows) {
        return fill("vat-books/PurchaseBookV4.xlsx", company, tin, year, month, rows, false);
    }

    private byte[] fill(String template, String company, String tin, int year, int month, List<AccountingService.VatRow> rows, boolean sales) {
        try (InputStream in = new ClassPathResource(template).getInputStream(); XSSFWorkbook wb = new XSSFWorkbook(in)) {
            XSSFSheet sh = wb.getSheetAt(0);
            set(sh, 1, 2, company);
            set(sh, 2, 2, tin);
            setNum(sh, 3, 2, year);
            setNum(sh, 4, 2, month);
            set(sh, 6, 2, rows.isEmpty() ? "PO" : "");
            if (!sales) {                          // the template carries sample amounts in AB13:AB15
                for (int r = FIRST; r < FIRST + 3; r++) clear(sh, r, col("AB"));
            }
            int n = Math.max(rows.size(), TEMPLATE_ROWS);
            int extra = n - TEMPLATE_ROWS;
            if (extra > 0) {                       // make room: push the totals and the notes down
                sh.shiftRows(FIRST + TEMPLATE_ROWS, sh.getLastRowNum(), extra, true, false);
                XSSFRow model = sh.getRow(FIRST + TEMPLATE_ROWS - 1);
                for (int r = FIRST + TEMPLATE_ROWS; r < FIRST + n; r++) {
                    XSSFRow nr = sh.createRow(r);
                    nr.copyRowFrom(model, new CellCopyPolicy());
                }
            }
            for (int i = 0; i < n; i++) {
                int r = FIRST + i;
                int x = r + 1;                     // the Excel row number in formulas
                for (int c = 0; c <= (sales ? col("W") : col("AI")); c++) clear(sh, r, c);
                rowFormulas(sh, r, x, sales);
                if (i >= rows.size()) continue;
                AccountingService.VatRow v = rows.get(i);
                set(sh, r, 0, v.number());
                set(sh, r, 1, "");
                Cell d = cell(sh, r, 2);
                d.setCellValue(Date.from(v.date().atStartOfDay(ZoneId.systemDefault()).toInstant()));
                set(sh, r, 3, v.party());
                set(sh, r, 4, "");
                set(sh, r, 5, v.tin() == null ? "" : v.tin());
                setNum(sh, r, column(v, sales), lek(v.noVat()));
            }
            int total = FIRST + n;                 // the "Shuma totale" row
            String last = String.valueOf(FIRST + n);
            for (int c = col("G"); c <= (sales ? col("W") : col("AI")); c++) {
                String L = letter(c);
                cell(sh, total, c).setCellFormula("SUM(" + L + (FIRST + 1) + ":" + L + last + ")");
            }
            wb.setForceFormulaRecalculation(true);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The template's formulas on a data row (x = Excel row number). */
    private void rowFormulas(XSSFSheet sh, int r, int x, boolean sales) {
        if (sales) {
            cell(sh, r, col("G")).setCellFormula("ROUND(SUM(H" + x + ":W" + x + "),0)");
            cell(sh, r, col("M")).setCellFormula("ROUND(L" + x + "*0.2,0)");
            cell(sh, r, col("O")).setCellFormula("ROUND(N" + x + "*0.1,0)");
            cell(sh, r, col("Q")).setCellFormula("ROUND(P" + x + "*0.06,0)");
            cell(sh, r, col("S")).setCellFormula("ROUND(R" + x + "*0.2,0)");
        } else {
            cell(sh, r, col("G")).setCellFormula("ROUND(SUM(H" + x + ":AI" + x + "),0)");
            cell(sh, r, col("M")).setCellFormula("ROUND(L" + x + "*0.2,0)");
            cell(sh, r, col("O")).setCellFormula("ROUND(N" + x + "*0.1,0)");
            cell(sh, r, col("Q")).setCellFormula("ROUND(P" + x + "*0.06,0)");
            cell(sh, r, col("S")).setCellFormula("ROUND(R" + x + "*0.2,0)");
            cell(sh, r, col("U")).setCellFormula("ROUND(T" + x + "*0.2,0)");
            cell(sh, r, col("W")).setCellFormula("ROUND(V" + x + "*0.1,0)");
            cell(sh, r, col("Y")).setCellFormula("ROUND(X" + x + "*0.06,0)");
            cell(sh, r, col("AA")).setCellFormula("ROUND(Z" + x + "*0.2,0)");
            cell(sh, r, col("AC")).setCellFormula("ROUND(AB" + x + "*IF($C$4 >= 2022, 0, 0.06),0)");
        }
    }

    /** Which column an invoice's value (without VAT) goes in. */
    private static int column(AccountingService.VatRow v, boolean sales) {
        if (sales) {
            return switch (v.rate()) {
                case 20 -> col("L");
                case 10 -> col("N");
                case 6 -> col("P");
                default -> switch (v.zeroColumn()) {
                    case "ZERO" -> col("K");       // Furnizime me 0%
                    case "EXEMPT" -> col("H");     // Shitjet e përjashtuara
                    case "NOVAT" -> col("I");      // Shitjet pa TVSH
                    default -> col("J");           // Eksporte mallrash
                };
            };
        }
        return switch (v.purchaseKind() == null ? "LOCAL" : v.purchaseKind()) {
            case "IMPORT" -> switch (v.rate()) {               // Importe mallra
                case 20 -> col("L");
                case 10 -> col("N");
                case 6 -> col("P");
                default -> col("K");                           // import mallra të përjashtuara
            };
            case "INVEST_LOCAL" -> v.rate() == 0 ? col("I") : col("Z");     // investime brenda vendit
            case "INVEST_IMPORT" -> v.rate() == 0 ? col("J") : col("R");    // importe të investimit
            case "FARMER" -> col("AB");                        // nga fermerët vendas
            default -> switch (v.rate()) {                     // nga furnitorë vendas
                case 20 -> col("T");
                case 10 -> col("V");
                case 6 -> col("X");
                default -> col("H");                           // të përjashtuara / pa TVSH
            };
        };
    }

    private static long lek(BigDecimal v) {
        return v == null ? 0 : v.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static Cell cell(XSSFSheet sh, int r, int c) {
        Row row = sh.getRow(r) == null ? sh.createRow(r) : sh.getRow(r);
        Cell cell = row.getCell(c);
        return cell == null ? row.createCell(c) : cell;
    }

    private static void set(XSSFSheet sh, int r, int c, String v) { cell(sh, r, c).setCellValue(v); }

    private static void setNum(XSSFSheet sh, int r, int c, double v) { cell(sh, r, c).setCellValue(v); }

    private static void clear(XSSFSheet sh, int r, int c) {
        Row row = sh.getRow(r);
        if (row != null && row.getCell(c) != null) row.getCell(c).setBlank();
    }

    /** Column letters to a 0-based index (A = 0, AB = 27). */
    static int col(String letters) {
        int n = 0;
        for (char ch : letters.toCharArray()) n = n * 26 + (ch - 'A' + 1);
        return n - 1;
    }

    static String letter(int c) {
        StringBuilder sb = new StringBuilder();
        for (int n = c + 1; n > 0; n = (n - 1) / 26) sb.insert(0, (char) ('A' + (n - 1) % 26));
        return sb.toString();
    }
}
