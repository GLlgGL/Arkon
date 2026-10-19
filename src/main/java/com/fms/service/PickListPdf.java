package com.fms.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Pick list for the warehouse: which items of a sale to take from which
 * shelf, with a tick box per line and signature lines.
 */
public final class PickListPdf {

    /** One line to pick. shelf null = not on a shelf. */
    public record Row(String code, String product, int quantity, String unit, String shelf) {
    }

    public record PickList(String saleNumber, LocalDateTime date, String client, String company,
                           String preparedBy, List<Row> rows) {
    }

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color NAVY = new Color(0x0b, 0x47, 0x79);
    private static final Color AMBER = new Color(0xff, 0xa1, 0x00);
    private static final Color INK = new Color(0x1f, 0x29, 0x33);
    private static final Color MUTED = new Color(0x6b, 0x77, 0x85);
    private static final Color LINE = new Color(0xd5, 0xdb, 0xe1);
    private static final Color ZEBRA = new Color(0xf6, 0xf8, 0xfa);
    private static final Color SHELF_BG = new Color(0xff, 0xf4, 0xdc);

    private final BaseFont regular = InvoicePdf.font("/fonts/Lato-Regular.ttf", BaseFont.HELVETICA);
    private final BaseFont bold = InvoicePdf.font("/fonts/Lato-Bold.ttf", BaseFont.HELVETICA_BOLD);
    private final BaseFont black = InvoicePdf.font("/fonts/Lato-Black.ttf", BaseFont.HELVETICA_BOLD);

    private Font f(BaseFont b, float size, Color c) {
        return new Font(b, size, Font.NORMAL, c);
    }

    public void write(PickList pl, OutputStream out) {
        Document doc = new Document(PageSize.A4, 40, 40, 40, 50);
        try {
            PdfWriter.getInstance(doc, out);
            doc.addTitle("Pick list " + pl.saleNumber());
            doc.open();

            // Header: title left, sale number right
            PdfPTable head = new PdfPTable(new float[] {60, 40});
            head.setWidthPercentage(100);
            PdfPCell title = new PdfPCell();
            title.setBorder(Rectangle.BOTTOM);
            title.setBorderColorBottom(AMBER);
            title.setBorderWidthBottom(3);
            title.setPaddingBottom(10);
            title.addElement(new Paragraph("PICK LIST", f(black, 24, NAVY)));
            title.addElement(new Paragraph("Take each item from the shelf shown and tick it off.", f(regular, 9.5f, MUTED)));
            head.addCell(title);
            PdfPCell no = new PdfPCell();
            no.setBorder(Rectangle.BOTTOM);
            no.setBorderColorBottom(AMBER);
            no.setBorderWidthBottom(3);
            no.setPaddingBottom(10);
            Paragraph label = new Paragraph("Sale no.", f(regular, 9, MUTED));
            label.setAlignment(Element.ALIGN_RIGHT);
            Paragraph number = new Paragraph(pl.saleNumber(), f(black, 22, INK));
            number.setAlignment(Element.ALIGN_RIGHT);
            no.addElement(label);
            no.addElement(number);
            head.addCell(no);
            doc.add(head);

            // Details
            PdfPTable info = new PdfPTable(new float[] {25, 25, 25, 25});
            info.setWidthPercentage(100);
            info.setSpacingBefore(12);
            infoCell(info, "Client", pl.client());
            infoCell(info, "Date", pl.date().format(DATE_TIME));
            infoCell(info, "From", pl.company());
            infoCell(info, "Prepared by", pl.preparedBy());
            doc.add(info);

            // Items
            PdfPTable t = new PdfPTable(new float[] {6, 15, 37, 14, 18, 10});
            t.setWidthPercentage(100);
            t.setSpacingBefore(18);
            t.setHeaderRows(1);
            for (String h : new String[] {"#", "CODE", "PRODUCT", "QTY", "SHELF", "PICKED"}) {
                PdfPCell c = new PdfPCell(new Phrase(h, f(bold, 9, Color.WHITE)));
                c.setBackgroundColor(NAVY);
                c.setBorder(Rectangle.NO_BORDER);
                c.setPadding(8);
                c.setHorizontalAlignment(h.equals("PRODUCT") ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
                t.addCell(c);
            }
            int n = 1;
            int units = 0;
            for (Row r : pl.rows()) {
                Color bg = n % 2 == 0 ? ZEBRA : Color.WHITE;
                body(t, String.valueOf(n), f(regular, 10, MUTED), Element.ALIGN_CENTER, bg);
                body(t, r.code() == null ? "–" : r.code(), f(regular, 10, INK), Element.ALIGN_CENTER, bg);
                body(t, r.product(), f(bold, 11, INK), Element.ALIGN_LEFT, bg);
                body(t, r.quantity() + (r.unit() == null ? "" : " " + r.unit()), f(black, 12, INK), Element.ALIGN_CENTER, bg);
                PdfPCell shelf = cell(r.shelf() == null ? "Not on a shelf" : r.shelf(),
                        r.shelf() == null ? f(regular, 9.5f, MUTED) : f(black, 15, NAVY), Element.ALIGN_CENTER, SHELF_BG);
                t.addCell(shelf);
                // empty tick box
                PdfPTable box = new PdfPTable(1);
                box.setTotalWidth(16);
                box.setLockedWidth(true);
                PdfPCell sq = new PdfPCell(new Phrase(" "));
                sq.setFixedHeight(16);
                sq.setBorderColor(MUTED);
                sq.setBorderWidth(1.2f);
                box.addCell(sq);
                PdfPCell tick = new PdfPCell(box);
                tick.setBackgroundColor(bg);
                tick.setBorder(Rectangle.BOTTOM);
                tick.setBorderColorBottom(LINE);
                tick.setHorizontalAlignment(Element.ALIGN_CENTER);
                tick.setVerticalAlignment(Element.ALIGN_MIDDLE);
                tick.setPadding(8);
                t.addCell(tick);
                units += r.quantity();
                n++;
            }
            doc.add(t);

            Paragraph sum = new Paragraph(pl.rows().size() + " lines, " + units + " units in total", f(bold, 10, INK));
            sum.setAlignment(Element.ALIGN_RIGHT);
            sum.setSpacingBefore(8);
            doc.add(sum);

            // Signatures
            PdfPTable sign = new PdfPTable(new float[] {30, 5, 30, 5, 30});
            sign.setWidthPercentage(100);
            sign.setSpacingBefore(55);
            sign.setKeepTogether(true);
            sign.addCell(signature("Picked by"));
            sign.addCell(blank());
            sign.addCell(signature("Checked by"));
            sign.addCell(blank());
            sign.addCell(signature("Date and time"));
            doc.add(sign);
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not create the pick list PDF", e);
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
    }

    private void infoCell(PdfPTable t, String label, String value) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingRight(8);
        c.addElement(new Paragraph(label.toUpperCase(), f(bold, 7.5f, MUTED)));
        c.addElement(new Paragraph(value == null ? "–" : value, f(bold, 10.5f, INK)));
        t.addCell(c);
    }

    private PdfPCell cell(String text, Font font, int align, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(LINE);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPaddingTop(9);
        c.setPaddingBottom(10);
        c.setPaddingLeft(8);
        c.setPaddingRight(8);
        return c;
    }

    private void body(PdfPTable t, String text, Font font, int align, Color bg) {
        t.addCell(cell(text, font, align, bg));
    }

    private PdfPCell signature(String who) {
        PdfPCell c = new PdfPCell(new Phrase(who, f(regular, 9, MUTED)));
        c.setBorder(Rectangle.TOP);
        c.setBorderColorTop(MUTED);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingTop(6);
        return c;
    }

    private PdfPCell blank() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }
}
