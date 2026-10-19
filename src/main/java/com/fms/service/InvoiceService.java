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

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter NOTE_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final OrderService orders;
    private final ConfigService configService;
    private final StorageService storage;
    private final PaymentService payments;
    private final InvoicePdf pdf = new InvoicePdf();

    public InvoiceService(OrderService orders, ConfigService configService, StorageService storage,
                          PaymentService payments) {
        this.orders = orders;
        this.configService = configService;
        this.storage = storage;
        this.payments = payments;
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

        InvoiceDocument doc = document(order, config);
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

        List<InvoiceDocument.Item> items = order.lines().stream().map(InvoiceService::item).toList();

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
            title = "Offer";
            toCaption = "Offer to";
            secondLabel = "Valid until";
            secondDate = info.getValidUntil();
            amountLabel = "Offer total";
            note = (secondDate == null ? "" : "This offer is valid until " + secondDate.format(NOTE_DATE) + ". ")
                    + "Prices include the discounts shown. We look forward to working with you.";
        } else {
            title = type == OrderType.SALE ? "Sale invoice" : "Purchase invoice";
            toCaption = "Bill to";
            secondLabel = "Due date";
            secondDate = info.getDueDate();
            PaymentService.Balance b = payments.balance(order, info);
            paid = b.paid();
            if (b.remaining().signum() == 0) {
                amountLabel = "Paid in full";
            } else {
                amountLabel = b.paid().signum() > 0 ? "Balance due" : "Amount due";
                highlight = b.remaining();
            }
            String due = secondDate == null ? "" : "Payment due by " + secondDate.format(NOTE_DATE) + ". ";
            note = type == OrderType.SALE
                    ? due + "Please quote invoice number " + order.code() + " with your payment. Thank you for your business!"
                    : due + "Purchase of goods from " + order.provider().name() + ".";
        }

        return new InvoiceDocument(title, order.code(), order.lines().get(0).getCreatedAt(), secondLabel, secondDate,
                party("From", order.provider()), party(toCaption, order.buyer()), items,
                order.valueNoVat(), order.vatValue(), order.valueWithVat(), vatPercent,
                paid, amountLabel, highlight, config.getCurrency(), config.getBrandColor(),
                storage.logoPath(config.getLogoFile()), note);
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
}
