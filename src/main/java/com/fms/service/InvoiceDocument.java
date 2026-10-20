package com.fms.service;

import java.awt.Color;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything printed on one invoice, independent of the database and Spring,
 * so the PDF layout (InvoicePdf) can be rendered and checked on its own.
 *
 * @param title          e.g. "Sale invoice", "Purchase invoice", "Offer"
 * @param secondDateLabel "Due date" or "Valid until"; null to leave out
 * @param amountLabel    caption of the highlighted amount, e.g. "Amount due"
 * @param highlight      the highlighted amount (balance due, or the total)
 * @param paid           payments so far; null when not applicable
 * @param note           short text under the totals (terms, validity); may be null
 */
public record InvoiceDocument(
        String title,
        String number,
        LocalDateTime date,
        String secondDateLabel,
        LocalDate secondDate,
        Party from,
        Party to,
        List<Item> items,
        BigDecimal subtotal,
        BigDecimal vat,
        BigDecimal total,
        BigDecimal vatPercent,
        BigDecimal paid,
        String amountLabel,
        BigDecimal highlight,
        String currency,
        Color brand,
        Path logo,
        String note,
        java.util.Map<String, String> labels) {

    /** Fixed word on the PDF in the invoice language, or the English default. */
    public String label(String key, String english) {
        return labels == null ? english : labels.getOrDefault(key, english);
    }

    public record Party(String caption, String name, String tin, String address, String email, String phone) {
    }

    public record Item(String description, String unit, int quantity, BigDecimal unitPrice,
                       BigDecimal discountPercent, BigDecimal amount) {
    }

    public boolean hasDiscounts() {
        return items.stream().anyMatch(i -> i.discountPercent() != null && i.discountPercent().signum() > 0);
    }
}
