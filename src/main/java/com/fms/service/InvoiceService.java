package com.fms.service;

import com.fms.domain.AppConfig;
import com.fms.domain.OrderInfo;
import com.fms.domain.OrderLine;
import com.fms.domain.OrderType;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Creates invoice PDFs. Collects the order, its due date or validity, the
 * payments so far and the settings into an {@link InvoiceDocument}, and lets
 * {@link InvoicePdf} draw it. Port of GenerateInvoicePDF() from the old pages,
 * with a redesigned layout.
 */
@Service
public class InvoiceService {

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private CurrencyService currency;

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter NOTE_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final OrderService orders;
    private final ConfigService configService;
    private final StorageService storage;
    private final PaymentService payments;
    private final org.springframework.context.MessageSource messages;
    private final InvoicePdf pdf = new InvoicePdf();

    /** Every fixed word printed on the PDFs (invoice, credit note, purchase order, pick list). */
    private static final String[] PDF_KEYS = {
        "pdf.fx.currency", "pdf.fx.rate", "pdf.fx.summary", "pdf.fx.taxable", "pdf.fx.exempt", "pdf.fx.vat", "pdf.fx.total",
        "pdf.no", "pdf.date", "pdf.description", "pdf.qty", "pdf.unitPrice", "pdf.disc", "pdf.amount", "pdf.notes",
        "pdf.vat", "pdf.subtotal", "pdf.total", "pdf.paid", "pdf.balanceDue", "pdf.buyer", "pdf.seller", "pdf.tin",
        "pdf.footer", "pdf.page", "pdf.pickList", "pdf.pickHint", "pdf.pickHintNoShelf", "pdf.saleNo", "pdf.client",
        "pdf.from", "pdf.preparedBy", "pdf.code", "pdf.product", "pdf.shelf", "pdf.picked", "pdf.notOnShelf",
        "pdf.pickSummary", "pdf.pickedBy", "pdf.checkedBy", "pdf.dateTime"
    };

    public InvoiceService(OrderService orders, ConfigService configService, StorageService storage,
                          PaymentService payments, org.springframework.context.MessageSource messages) {
        this.orders = orders;
        this.configService = configService;
        this.storage = storage;
        this.payments = payments;
        this.messages = messages;
    }

    /** The language chosen for PDFs in Settings. */
    private java.util.Locale pdfLocale() {
        return java.util.Locale.forLanguageTag(configService.get().getInvoiceLang());
    }

    /** A text in the PDF language; {0}, {1}... filled from args. */
    private String t(String key, String english, Object... args) {
        return messages.getMessage(key, args.length == 0 ? null : args, english, pdfLocale());
    }

    /** All fixed PDF words in the PDF language (also used for pick lists). */
    public java.util.Map<String, String> pdfLabels() {
        java.util.Map<String, String> m = new java.util.HashMap<>();
        java.util.Locale loc = pdfLocale();
        for (String k : PDF_KEYS) {
            String v = messages.getMessage(k, null, null, loc);
            if (v != null) {
                m.put(k, v);
            }
        }
        return m;
    }

    /**
     * Writes the invoice for an order to the invoices folder (replacing any
     * earlier version of the same order) and returns the file name.
     */
    public String generate(OrderType type, String code) {
        OrderView order = orders.load(type, code);
        if (order.isEmpty()) {
            throw new OrderException("Add at least one product before creating the invoice.");
        }
        AppConfig config = configService.get();

        // Purchases are filed under the merchant, sales and offers under the client.
        String party = type == OrderType.PURCHASE ? order.provider().name() : order.buyer().name();
        String date = order.lines().get(0).getCreatedAt().format(FILE_DATE);
        String fileName = StorageService.safeFileName(party + "_" + date + "_" + code + "_" + type.getLabel() + ".pdf");

        InvoiceDocument doc = inCurrency(document(order, config), currency.ofOrder(type, code));
        Path dir = storage.invoiceDir(type);
        try {
            removeOlderVersions(dir, code, type);
            try (OutputStream out = Files.newOutputStream(dir.resolve(fileName))) {
                pdf.write(doc, out);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return fileName;
    }

    InvoiceDocument document(OrderView order, AppConfig config) {
        OrderType type = order.type();
        OrderInfo info = orders.info(type, order.code(), order.lines().get(0).getCreatedAt().toLocalDate());

        String returned = t("pdf.returned", "Return");
        List<InvoiceDocument.Item> items = order.lines().stream().map(l -> {
            InvoiceDocument.Item i = item(l);
            return l.isReturn() ? new InvoiceDocument.Item(returned + ": " + i.description(), i.unit(), i.quantity(),
                    i.unitPrice(), i.discountPercent(), i.amount()) : i;
        }).toList();

        // VAT % from the saved lines, so old invoices stay right after a VAT change.
        BigDecimal vatPercent = order.valueNoVat().signum() == 0 ? config.getVatPercent()
                : order.vatValue().multiply(HUNDRED).divide(order.valueNoVat(), 1, RoundingMode.HALF_UP);

        String title;
        String toCaption;
        String secondLabel;
        java.time.LocalDate secondDate;
        BigDecimal paid = null;
        String amountLabel;
        BigDecimal highlight = order.valueWithVat();
        String note;

        if (type == OrderType.OFFER) {
            title = t("pdf.title.OFFER", "Offer");
            toCaption = t("pdf.offerTo", "Offer to");
            secondLabel = t("pdf.validUntil", "Valid until");
            secondDate = info.getValidUntil();
            amountLabel = t("pdf.offerTotal", "Offer total");
            note = secondDate == null ? t("pdf.note.offerNoDate", "Prices include the discounts shown.")
                    : t("pdf.note.offer", "This offer is valid until {0}. Prices include the discounts shown.", secondDate.format(NOTE_DATE));
        } else {
            title = type == OrderType.SALE ? t("pdf.title.SALE", "Sale invoice") : t("pdf.title.PURCHASE", "Purchase invoice");
            toCaption = t("pdf.billTo", "Bill to");
            secondLabel = t("pdf.dueDate", "Due date");
            secondDate = info.getDueDate();
            PaymentService.Balance b = payments.balance(order, info);
            paid = b.paid();
            if (b.status() == PaymentService.Status.REFUND_DUE) {
                amountLabel = t("pdf.refundDue", "Refund due");
                highlight = b.refundDue();
            } else if (b.remaining().signum() == 0) {
                amountLabel = t("pdf.paidInFull", "Paid in full");
            } else {
                amountLabel = b.paid().signum() > 0 ? t("pdf.balanceDue", "Balance due") : t("pdf.amountDue", "Amount due");
                highlight = b.remaining();
            }
            String due = secondDate == null ? "" : t("pdf.note.due", "Payment due by {0}.", secondDate.format(NOTE_DATE)) + " ";
            note = type == OrderType.SALE
                    ? due + t("pdf.note.sale", "Please quote invoice number {0} with your payment. Thank you for your business!", order.code())
                    : due + t("pdf.note.purchase", "Purchase of goods from {0}.", order.provider().name());
        }

        if (type != OrderType.PURCHASE && info.getVatNote() != null) {
            note = (note == null || note.isBlank() ? "" : note + " ") + info.getVatNote();     // e.g. the VAT exemption wording
        }
        return new InvoiceDocument(title, order.code(), order.lines().get(0).getCreatedAt(), secondLabel, secondDate,
                party(t("pdf.from", "From"), order.provider()), party(toCaption, order.buyer()), items,
                order.valueNoVat(), order.vatValue(), order.valueWithVat(), vatPercent,
                paid, amountLabel, highlight, config.getCurrency(), config.getBrandColor(),
                storage.logoPath(config.getLogoFile()), note, pdfLabels());
    }

    // ------------------------------------------------------------ credit note

    /**
     * PDF for one return: a credit note for a sale (goods a client brought
     * back) or a return note for a purchase (goods sent back to the merchant).
     * Saved with that type's invoices. Returns the file name.
     */
    public String creditNote(OrderType type, String code, int returnNo) {
        OrderView order = orders.load(type, code);
        List<OrderLine> ret = order.lines().stream()
                .filter(l -> l.getReturnNo() != null && l.getReturnNo() == returnNo).toList();
        if (ret.isEmpty()) {
            throw new OrderException("Return not found.");
        }
        AppConfig config = configService.get();
        boolean sale = type == OrderType.SALE;
        List<InvoiceDocument.Item> items = ret.stream().map(l -> {
            BigDecimal d = Money.nz(l.getDiscountPercent());
            BigDecimal listPrice = d.signum() > 0 && d.compareTo(HUNDRED) < 0
                    ? l.getUnitPrice().multiply(HUNDRED).divide(HUNDRED.subtract(d), 2, RoundingMode.HALF_UP) : l.getUnitPrice();
            return new InvoiceDocument.Item(l.getProductName(), l.getUnit(), -l.getQuantity(), listPrice, d, l.getValueNoVat().negate());
        }).toList();
        BigDecimal noVat = ret.stream().map(OrderLine::getValueNoVat).reduce(BigDecimal.ZERO, BigDecimal::add).negate();
        BigDecimal vat = ret.stream().map(OrderLine::getVatValue).reduce(BigDecimal.ZERO, BigDecimal::add).negate();
        BigDecimal total = noVat.add(vat);
        BigDecimal vatPercent = noVat.signum() == 0 ? config.getVatPercent()
                : vat.multiply(HUNDRED).divide(noVat, 1, RoundingMode.HALF_UP);
        OrderLine first = ret.get(0);
        String reason = first.getReturnReason() == null ? "" : " " + t("pdf.reason", "Reason") + ": "
                + t("reason.ret." + first.getReturnReason().name(), first.getReturnReason().name())
                + (first.getReturnNote() == null ? "" : " – " + first.getReturnNote()) + ".";
        // Sale: our company credits the client. Purchase: our company sends goods back to the merchant.
        InvoiceDocument.Party from = sale ? party(t("pdf.from", "From"), order.provider()) : party(t("pdf.from", "From"), order.buyer());
        InvoiceDocument.Party to = sale ? party(t("pdf.billTo", "Bill to"), order.buyer()) : party(t("pdf.supplier", "Supplier"), order.provider());
        InvoiceDocument doc = new InvoiceDocument(
                sale ? t("pdf.title.creditNote", "Credit note") : t("pdf.title.returnNote", "Return note"),
                code + "-" + returnNo, first.getCreatedAt(), null, null, from, to,
                items, noVat, vat, total, vatPercent, null,
                sale ? t("pdf.creditTotal", "Credit total") : t("pdf.returnTotal", "Return total"), total,
                config.getCurrency(), config.getBrandColor(), storage.logoPath(config.getLogoFile()),
                (sale ? t("pdf.note.credit", "Credit for goods returned from sale {0}.", code)
                      : t("pdf.note.returnNote", "Goods returned to {0} from purchase {1}.", order.provider().name(), code)) + reason,
                pdfLabels());
        doc = inCurrency(doc, currency.ofOrder(type, code));
        String partyName = sale ? order.buyer().name() : order.provider().name();
        String fileName = StorageService.safeFileName(partyName + "_" + first.getCreatedAt().format(FILE_DATE)
                + "_" + code + (sale ? "_CreditNote-" : "_ReturnNote-") + returnNo + ".pdf");
        try (OutputStream out = Files.newOutputStream(storage.invoiceDir(type).resolve(fileName))) {
            pdf.write(doc, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return fileName;
    }

    /** File name of a return's PDF, if it exists (for links in the returns list). */
    public String creditNoteFile(OrderType type, String code, int returnNo) {
        String suffix = "_" + code + (type == OrderType.SALE ? "_CreditNote-" : "_ReturnNote-") + returnNo + ".pdf";
        try (Stream<Path> files = Files.list(storage.invoiceDir(type))) {
            return files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(suffix)).findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    // ------------------------------------------------------- purchase order

    /** PDF of a purchase order (a request to the merchant); written to out. */
    public void purchaseOrder(com.fms.domain.PurchaseOrder po, PartyInfo company, PartyInfo merchant, OutputStream out) {
        AppConfig config = configService.get();
        BigDecimal rate = config.getVatPercent();
        List<InvoiceDocument.Item> items = po.getLines().stream().map(l -> new InvoiceDocument.Item(l.getProductName(),
                l.getUnit(), l.getQuantity(), l.getUnitPrice(), BigDecimal.ZERO,
                Money.round(l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity()))))).toList();
        BigDecimal noVat = items.stream().map(InvoiceDocument.Item::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vat = Money.round(noVat.multiply(rate).divide(HUNDRED, 10, RoundingMode.HALF_UP));
        BigDecimal total = noVat.add(vat);
        InvoiceDocument doc = new InvoiceDocument(t("pdf.title.po", "Purchase order"), po.getCode(), po.getCreatedAt(),
                null, null, party(t("pdf.from", "From"), company), party(t("pdf.supplier", "Supplier"), merchant),
                items, noVat, vat, total, rate, null, t("pdf.orderTotal", "Order total"), total,
                config.getCurrency(), config.getBrandColor(), storage.logoPath(config.getLogoFile()),
                t("pdf.note.po", "Please deliver the items above and quote order number {0} on your invoice.", po.getCode()), pdfLabels());
        pdf.write(doc, out);
    }

    /** Shows the price before discount, so "price, discount, amount" add up on the page. */
    private static InvoiceDocument.Item item(OrderLine l) {
        BigDecimal d = Money.nz(l.getDiscountPercent());
        BigDecimal listPrice = l.getUnitPrice();
        if (d.signum() > 0 && d.compareTo(HUNDRED) < 0) {
            listPrice = l.getUnitPrice().multiply(HUNDRED).divide(HUNDRED.subtract(d), 2, RoundingMode.HALF_UP);
        }
        return new InvoiceDocument.Item(l.getProductName(), l.getUnit(), l.getQuantity(), listPrice, d, l.getValueNoVat());
    }

    private static InvoiceDocument.Party party(String caption, PartyInfo p) {
        return new InvoiceDocument.Party(caption, p.name(), p.tin(), p.address(), p.email(), p.phone());
    }

    private void removeOlderVersions(Path dir, String code, OrderType type) throws IOException {
        String suffix = "_" + code + "_" + type.getLabel() + ".pdf";
        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : files.filter(p -> p.getFileName().toString().endsWith(suffix)).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    /**
     * A document of a foreign-currency order: amounts in its currency, plus the
     * rate and the total and VAT in the base currency (required on invoices).
     */
    private InvoiceDocument inCurrency(InvoiceDocument d, CurrencyService.Fx fx) {
        if (!fx.foreign()) return d;
        List<InvoiceDocument.Item> items = d.items().stream().map(i -> new InvoiceDocument.Item(i.description(), i.unit(), i.quantity(),
                fx.of(i.unitPrice()), i.discountPercent(), fx.of(i.amount()))).toList();
        String base = configService.get().getCurrency();
        boolean vatFree = d.vat() == null || d.vat().signum() == 0;
        var summary = new InvoiceDocument.FxSummary(fx.code(), fx.rate(), base,
                vatFree ? BigDecimal.ZERO : d.subtotal(), vatFree ? d.subtotal() : BigDecimal.ZERO, d.vat(), d.total());
        return new InvoiceDocument(d.title(), d.number(), d.date(), d.secondDateLabel(), d.secondDate(), d.from(), d.to(), items,
                fx.of(d.subtotal()), fx.of(d.vat()), fx.of(d.total()), d.vatPercent(), fx.of(d.paid()), d.amountLabel(), fx.of(d.highlight()),
                fx.code(), d.brand(), d.logo(), d.note(), d.labels(), summary);
    }
}
