package com.fms.service;

import com.fms.domain.OrderLine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * Excel export (.xlsx). The original Export_Click rendered the GridView HTML
 * and sent it with an .xls name; this writes a real workbook.
 */
@Service
public class ExcelService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] orderLines(String sheetName, List<OrderLine> lines) {
        String[] headers = {"Order no", "Date", "Product", "Provider", "Buyer", "Unit", "Quantity",
                "Unit price", "Discount %", "Value (no VAT)", "VAT", "Value (with VAT)", "User"};
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);
            CellStyle bold = wb.createCellStyle();
            Font f = wb.createFont();
            f.setBold(true);
            bold.setFont(f);
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            Row h = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                h.createCell(i).setCellValue(headers[i]);
                h.getCell(i).setCellStyle(bold);
            }
            int r = 1;
            BigDecimal noVat = BigDecimal.ZERO;
            BigDecimal vat = BigDecimal.ZERO;
            BigDecimal withVat = BigDecimal.ZERO;
            for (OrderLine l : lines) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(l.getOrderCode());
                row.createCell(1).setCellValue(l.getCreatedAt().format(DATE));
                row.createCell(2).setCellValue(l.getProductName());
                row.createCell(3).setCellValue(l.getProviderName());
                row.createCell(4).setCellValue(l.getBuyerName());
                row.createCell(5).setCellValue(l.getUnit() == null ? "" : l.getUnit());
                row.createCell(6).setCellValue(l.getQuantity());
                moneyCell(row, 7, l.getUnitPrice(), money);
                row.createCell(8).setCellValue(Money.nz(l.getDiscountPercent()).doubleValue());
                moneyCell(row, 9, l.getValueNoVat(), money);
                moneyCell(row, 10, l.getVatValue(), money);
                moneyCell(row, 11, l.getValueWithVat(), money);
                row.createCell(12).setCellValue(l.getCreatedBy());
                noVat = noVat.add(Money.nz(l.getValueNoVat()));
                vat = vat.add(Money.nz(l.getVatValue()));
                withVat = withVat.add(Money.nz(l.getValueWithVat()));
            }
            Row total = sheet.createRow(r);
            total.createCell(8).setCellValue("Total");
            total.getCell(8).setCellStyle(bold);
            moneyCell(total, 9, noVat, money);
            moneyCell(total, 10, vat, money);
            moneyCell(total, 11, withVat, money);
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] table(String sheetName, String[] headers, List<Object[]> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);
            CellStyle bold = wb.createCellStyle();
            Font f = wb.createFont();
            f.setBold(true);
            bold.setFont(f);
            Row h = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                h.createCell(i).setCellValue(headers[i]);
                h.getCell(i).setCellStyle(bold);
            }
            int r = 1;
            for (Object[] values : rows) {
                Row row = sheet.createRow(r++);
                for (int i = 0; i < values.length; i++) {
                    Object v = values[i];
                    if (v instanceof Number n) {
                        row.createCell(i).setCellValue(n.doubleValue());
                    } else {
                        row.createCell(i).setCellValue(v == null ? "" : v.toString());
                    }
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** One sheet of a multi-sheet workbook. */
    public record SheetData(String name, String[] headers, List<Object[]> rows) {
    }

    public byte[] workbook(List<SheetData> sheets) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle bold = wb.createCellStyle();
            Font f = wb.createFont();
            f.setBold(true);
            bold.setFont(f);
            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            for (SheetData data : sheets) {
                Sheet sheet = wb.createSheet(data.name());
                Row h = sheet.createRow(0);
                for (int i = 0; i < data.headers().length; i++) {
                    h.createCell(i).setCellValue(data.headers()[i]);
                    h.getCell(i).setCellStyle(bold);
                }
                int r = 1;
                for (Object[] values : data.rows()) {
                    Row row = sheet.createRow(r++);
                    for (int i = 0; i < values.length; i++) {
                        Object v = values[i];
                        if (v instanceof BigDecimal b) {
                            row.createCell(i).setCellValue(b.doubleValue());
                            row.getCell(i).setCellStyle(money);
                        } else if (v instanceof Number n) {
                            row.createCell(i).setCellValue(n.doubleValue());
                        } else {
                            row.createCell(i).setCellValue(v == null ? "" : v.toString());
                        }
                    }
                }
                for (int i = 0; i < data.headers().length; i++) {
                    sheet.autoSizeColumn(i);
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void moneyCell(Row row, int col, BigDecimal value, CellStyle style) {
        row.createCell(col).setCellValue(Money.nz(value).doubleValue());
        row.getCell(col).setCellStyle(style);
    }
}
