package com.fms.repo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One row of the order lists: all lines of one order code added up. */
public record OrderSummary(
        String orderCode,
        LocalDateTime createdAt,
        String providerName,
        String buyerName,
        Long lineCount,
        Long totalQuantity,
        BigDecimal valueNoVat,
        BigDecimal vatValue,
        BigDecimal valueWithVat,
        String createdBy,
        LocalDateTime lastUpdated) {
}
