package com.fms.service;

import java.math.BigDecimal;

/**
 * Price calculation for one order line. Sent to the browser as JSON to
 * preview a line, and recomputed on the server when the line is saved.
 *
 * @param available      units in stock (purchased minus sold); null for purchases
 * @param quantity       quantity the values are calculated for
 * @param exceedsStock   true when a sale or offer asks for more than is in stock;
 *                       such a line is refused when saved
 * @param remainingAfter units left in stock after this line (sales and offers); null for purchases
 * @param overLimit      true when this line would leave less than the product's quantity limit
 *                       in stock (warning only; the limit is the minimum stock to keep)
 */
public record Quote(
        String productName,
        String unit,
        BigDecimal basePrice,
        BigDecimal discountPercent,
        BigDecimal unitPrice,
        int requestedQuantity,
        int quantity,
        Long available,
        Long remainingAfter,
        int quantityLimit,
        boolean exceedsStock,
        boolean overLimit,
        BigDecimal valueNoVat,
        BigDecimal vatValue,
        BigDecimal valueWithVat) {
}
