package com.fms.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Draws an invoice PDF: brand-colour accents from Settings, the Lato font
 * (bundled under /fonts, covers Albanian letters), a clear items table and a
 * totals block. Uses only plain OpenPDF / iText 2 APIs.
 */
public final class InvoicePdf {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Color INK = new Color(0x1f, 0x29, 0x33);
    private static final Color MUTED = new Color(0x6b, 0x77, 0x85);
    private static final Color LINE = new Color(0xe3, 0xe7, 0xec);
    private static final Color ZEBRA = new Color(0xf6, 0xf8, 0xfa);
    private static final Color WHITE = Color.WHITE;

    private final BaseFont regular;
    private final BaseFont bold;
    private final BaseFont black;

    public InvoicePdf() {
        this.regular = font("/fonts/Lato-Regular.ttf", BaseFont.HELVETICA);
        this.bold = font("/fonts/Lato-Bold.ttf", BaseFont.HELVETICA_BOLD);
        this.black = font("/fonts/Lato-Black.ttf", BaseFont.HELVETICA_BOLD);
    }

    /** Loads a bundled TrueType font, or falls back to a built-in one. */
    static BaseFont font(String resource, String fallback) {
        try (InputStream in = InvoicePdf.class.getResourceAsStream(resource)) {
            if (in != null) {
                byte[] bytes = in.readAllBytes();
                String name = resource.substring(resource.lastIndexOf('/') + 1);
                return BaseFont.createFont(name, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
            }
        } catch (IOException | DocumentException ignored) {
            // fall through to the built-in font
        }
        try {
            return BaseFont.createFont(fallback, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("No usable font for invoices", e);
        }
    }

    private Font f(BaseFont base, float size, Color color) {
        return new Font(base, size, Font.NORMAL, color);
    }

    /** Labels of the document being written (one at a time, see synchronized write). */
    private InvoiceDocument current;

    private String L(String key, String english) {
        return current == null ? english : current.label(key, english);
    }

    public synchronized void write(InvoiceDocument inv, OutputStream out) {
        this.current = inv;
        Color brand = inv.brand();
        Color tint = mix(brand, WHITE, 0.92f);
        String cur = " " + inv.currency();

        Document doc = new Document(PageSize.A4, 42, 42, 54, 72);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new Frame(brand));
            doc.addTitle(inv.title() + " " + inv.number());
            doc.addCreator("IMS - Inventory Management System");
            doc.open();

            // ---------------------------------------------------- header
            PdfPTable head = table(new float[] {55, 45});
            PdfPCell left = cell();
            if (inv.logo() != null) {
                Image img = Image.getInstance(inv.logo().toString());
                img.scaleToFit(170, 60);
                PdfPCell logoCell = new PdfPCell(img, false);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setPaddingBottom(10);
                PdfPTable l = table(new float[] {1});
                l.addCell(logoCell);
                l.addCell(partyLines(inv.from(), 15, false));
                left.addElement(l);
            } else {
                left.addElement(partyLinesTable(inv.from(), 15));
            }
            head.addCell(left);

            PdfPCell right = cell();
            right.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph title = new Paragraph(new Chunk(inv.title().toUpperCase(), f(black, 22, brand)));
            title.setAlignment(Element.ALIGN_RIGHT);
            title.setSpacingAfter(8);
            right.addElement(title);
            PdfPTable meta = table(new float[] {50, 50});
            metaRow(meta, L("pdf.no", "No."), inv.number(), true);
            metaRow(meta, L("pdf.date", "Date"), inv.date().format(DATE_TIME), false);
            if (inv.secondDateLabel() != null && inv.secondDate() != null) {
                metaRow(meta, inv.secondDateLabel(), inv.secondDate().format(DATE), false);
            }
            right.addElement(meta);
            head.addCell(right);
            doc.add(head);

            // ------------------------------------- bill to + amount box
            PdfPTable mid = table(new float[] {55, 45});
            mid.setSpacingBefore(22);
            PdfPCell billTo = new PdfPCell();
            billTo.setBorder(Rectangle.LEFT);
            billTo.setBorderColorLeft(brand);
            billTo.setBorderWidthLeft(3);
            billTo.setBackgroundColor(tint);
            billTo.setPadding(12);
            billTo.setPaddingLeft(14);
            billTo.addElement(caption(inv.to().caption(), brand));
            billTo.addElement(partyLinesTable(inv.to(), 13));
            mid.addCell(billTo);

            PdfPCell gap = cell();
            gap.setPaddingLeft(14);
            PdfPTable amount = table(new float[] {1});
            PdfPCell box = new PdfPCell();
            box.setBorder(Rectangle.NO_BORDER);
            box.setBackgroundColor(brand);
            box.setPadding(14);
            Paragraph capt = new Paragraph(new Chunk(inv.amountLabel().toUpperCase(), f(bold, 8.5f, mix(brand, WHITE, 0.82f))));
            capt.setAlignment(Element.ALIGN_RIGHT);
            box.addElement(capt);
            Paragraph big = new Paragraph(Money.format(inv.highlight()) + cur, f(black, 21, WHITE));
            big.setAlignment(Element.ALIGN_RIGHT);
            box.addElement(big);
            if (inv.secondDateLabel() != null && inv.secondDate() != null) {
                Paragraph when = new Paragraph(inv.secondDateLabel() + " " + inv.secondDate().format(DATE), f(regular, 9, mix(brand, WHITE, 0.82f)));
                when.setAlignment(Element.ALIGN_RIGHT);
                box.addElement(when);
            }
            amount.addCell(box);
            gap.addElement(amount);
            mid.addCell(gap);
            doc.add(mid);

            // ---------------------------------------------------- items
            boolean disc = inv.hasDiscounts();
            PdfPTable items = table(disc ? new float[] {6, 40, 13, 15, 9, 17} : new float[] {6, 46, 14, 16, 18});
            items.setSpacingBefore(24);
            items.setHeaderRows(1);
            headCell(items, "#", Element.ALIGN_CENTER, brand);
            headCell(items, L("pdf.description", "DESCRIPTION"), Element.ALIGN_LEFT, brand);
            headCell(items, L("pdf.qty", "QTY"), Element.ALIGN_RIGHT, brand);
            headCell(items, L("pdf.unitPrice", "UNIT PRICE"), Element.ALIGN_RIGHT, brand);
            if (disc) {
                headCell(items, L("pdf.disc", "DISC."), Element.ALIGN_RIGHT, brand);
            }
            headCell(items, L("pdf.amount", "AMOUNT"), Element.ALIGN_RIGHT, brand);

            int n = 1;
            for (InvoiceDocument.Item it : inv.items()) {
                Color bg = n % 2 == 0 ? ZEBRA : WHITE;
                bodyCell(items, String.valueOf(n), Element.ALIGN_CENTER, f(regular, 9, MUTED), bg);
                bodyCell(items, it.description(), Element.ALIGN_LEFT, f(bold, 10, INK), bg);
                bodyCell(items, it.quantity() + (it.unit() == null ? "" : " " + it.unit()), Element.ALIGN_RIGHT, f(regular, 10, INK), bg);
                bodyCell(items, Money.format(it.unitPrice()), Element.ALIGN_RIGHT, f(regular, 10, INK), bg);
                if (disc) {
                    String d = it.discountPercent() == null || it.discountPercent().signum() == 0 ? "–"
                            : it.discountPercent().stripTrailingZeros().toPlainString() + "%";
                    bodyCell(items, d, Element.ALIGN_RIGHT, f(regular, 10, MUTED), bg);
                }
                bodyCell(items, Money.format(it.amount()), Element.ALIGN_RIGHT, f(bold, 10, INK), bg);
                n++;
            }
            doc.add(items);

            // --------------------------------------------------- totals
            PdfPTable totalsWrap = table(new float[] {55, 45});
            totalsWrap.setSpacingBefore(14);
            totalsWrap.setKeepTogether(true);

            PdfPCell noteCell = cell();
            noteCell.setPaddingRight(18);
            if (inv.note() != null && !inv.note().isBlank()) {
                noteCell.addElement(caption(L("pdf.notes", "NOTES"), brand));
                Paragraph note = new Paragraph(inv.note(), f(regular, 9, MUTED));
                note.setLeading(13);
                noteCell.addElement(note);
            }
            totalsWrap.addCell(noteCell);

            PdfPTable totals = table(new float[] {55, 45});
            String vatLabel = inv.vatPercent() == null ? L("pdf.vat", "VAT") : L("pdf.vat", "VAT") + " (" + inv.vatPercent().stripTrailingZeros().toPlainString() + "%)";
            totalRow(totals, L("pdf.subtotal", "Subtotal"), Money.format(inv.subtotal()) + cur, false, brand);
            totalRow(totals, vatLabel, Money.format(inv.vat()) + cur, false, brand);
            totalRow(totals, L("pdf.total", "Total"), Money.format(inv.total()) + cur, true, brand);
            if (inv.paid() != null && inv.paid().signum() > 0) {
                totalRow(totals, L("pdf.paid", "Paid"), "– " + Money.format(inv.paid()) + cur, false, brand);
                totalRow(totals, L("pdf.balanceDue", "Balance due"), Money.format(inv.highlight()) + cur, false, brand);
            }
            PdfPCell tc = cell();
            tc.addElement(totals);
            totalsWrap.addCell(tc);
            doc.add(totalsWrap);

            // ----------------------------------------------- signatures
            PdfPTable sign = table(new float[] {40, 20, 40});
            sign.setSpacingBefore(60);
            sign.setKeepTogether(true);
            sign.addCell(signature(L("pdf.buyer", "Buyer")));
            sign.addCell(cell());
            sign.addCell(signature(L("pdf.seller", "Seller")));
            doc.add(sign);
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Could not create the invoice PDF", e);
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
    }

    // ----------------------------------------------------------- pieces

    private PdfPTable partyLinesTable(InvoiceDocument.Party p, float nameSize) {
        PdfPTable t = table(new float[] {1});
        t.addCell(partyLines(p, nameSize, false));
        return t;
    }

    private PdfPCell partyLines(InvoiceDocument.Party p, float nameSize, boolean withCaption) {
        PdfPCell c = cell();
        if (withCaption && p.caption() != null && nameSize > 14) {
            c.addElement(caption(p.caption(), MUTED));
        }
        Paragraph name = new Paragraph(p.name(), f(black, nameSize, INK));
        name.setSpacingAfter(3);
        c.addElement(name);
        addLine(c, L("pdf.tin", "TIN") + " ", p.tin());
        addLine(c, null, p.address());
        addLine(c, null, p.email());
        addLine(c, null, p.phone());
        return c;
    }

    private void addLine(PdfPCell c, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Phrase ph = new Phrase();
        if (label != null) {
            ph.add(new Chunk(label, f(bold, 9, MUTED)));
        }
        ph.add(new Chunk(value, f(regular, 9, MUTED)));
        Paragraph p = new Paragraph(ph);
        p.setLeading(12.5f);
        c.addElement(p);
    }

    private Paragraph caption(String text, Color color) {
        Paragraph p = new Paragraph(new Chunk(text.toUpperCase(), f(bold, 8, color)));
        p.setSpacingAfter(4);
        return p;
    }

    private void metaRow(PdfPTable t, String label, String value, boolean strong) {
        PdfPCell l = new PdfPCell(new Phrase(label, f(regular, 9, MUTED)));
        l.setBorder(Rectangle.NO_BORDER);
        l.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPaddingRight(10);
        l.setPaddingBottom(3);
        t.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value, f(strong ? black : bold, strong ? 11 : 9.5f, INK)));
        v.setBorder(Rectangle.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPaddingBottom(3);
        t.addCell(v);
    }

    private void headCell(PdfPTable t, String text, int align, Color brand) {
        PdfPCell c = new PdfPCell(new Phrase(new Chunk(text, f(bold, 8.5f, WHITE))));
        c.setBackgroundColor(brand);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPaddingTop(7);
        c.setPaddingBottom(8);
        c.setPaddingLeft(8);
        c.setPaddingRight(8);
        t.addCell(c);
    }

    private void bodyCell(PdfPTable t, String text, int align, Font font, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(LINE);
        c.setBorderWidthBottom(0.6f);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPaddingTop(7);
        c.setPaddingBottom(8);
        c.setPaddingLeft(8);
        c.setPaddingRight(8);
        t.addCell(c);
    }

    private void totalRow(PdfPTable t, String label, String value, boolean grand, Color brand) {
        PdfPCell l = new PdfPCell(new Phrase(label, grand ? f(black, 11, WHITE) : f(regular, 9.5f, MUTED)));
        PdfPCell v = new PdfPCell(new Phrase(value, grand ? f(black, 12, WHITE) : f(bold, 10, INK)));
        for (PdfPCell c : new PdfPCell[] {l, v}) {
            c.setBorder(grand ? Rectangle.NO_BORDER : Rectangle.BOTTOM);
            c.setBorderColorBottom(LINE);
            c.setBorderWidthBottom(0.6f);
            c.setPaddingTop(grand ? 8 : 6);
            c.setPaddingBottom(grand ? 9 : 7);
            c.setPaddingLeft(10);
            c.setPaddingRight(10);
            if (grand) {
                c.setBackgroundColor(brand);
            }
        }
        l.setHorizontalAlignment(Element.ALIGN_LEFT);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(l);
        t.addCell(v);
    }

    private PdfPCell signature(String who) {
        PdfPCell c = new PdfPCell(new Phrase(who, f(regular, 9, MUTED)));
        c.setBorder(Rectangle.TOP);
        c.setBorderColorTop(new Color(0x9a, 0xa5, 0xb1));
        c.setBorderWidthTop(0.8f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPaddingTop(6);
        return c;
    }

    private static PdfPTable table(float[] widths) {
        PdfPTable t = new PdfPTable(widths.length);
        try {
            t.setWidths(widths);
        } catch (DocumentException e) {
            throw new IllegalStateException(e);
        }
        t.setWidthPercentage(100);
        t.getDefaultCell().setBorder(Rectangle.NO_BORDER);
        return t;
    }

    private static PdfPCell cell() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(0);
        return c;
    }

    /** a blended towards b by t (0 = a, 1 = b). */
    static Color mix(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Brand bar at the top of every page and a footer with the page number. */
    private final class Frame extends PdfPageEventHelper {
        private final Color brand;

        Frame(Color brand) {
            this.brand = brand;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Rectangle page = document.getPageSize();
            cb.saveState();
            cb.setColorFill(brand);
            cb.rectangle(0, page.getHeight() - 8, page.getWidth(), 8);
            cb.fill();
            cb.setColorStroke(LINE);
            cb.setLineWidth(0.6f);
            cb.moveTo(document.leftMargin(), 46);
            cb.lineTo(page.getWidth() - document.rightMargin(), 46);
            cb.stroke();
            cb.restoreState();
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(L("pdf.footer", "Generated automatically by IMS – Inventory Management System"), f(regular, 8, MUTED)),
                    document.leftMargin(), 32, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase(L("pdf.page", "Page") + " " + writer.getPageNumber(), f(regular, 8, MUTED)),
                    page.getWidth() - document.rightMargin(), 32, 0);
        }
    }

    static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
