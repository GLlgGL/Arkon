package com.fms.service;

import com.fms.domain.OrderLine;
import com.fms.domain.OrderType;
import java.math.BigDecimal;
import java.util.List;

/** Everything the order page and the invoice need about one order. */
public record OrderView(
        OrderType type,
        String code,
        List<OrderLine> lines,
        PartyInfo provider,
        PartyInfo buyer,
        BigDecimal valueNoVat,
        BigDecimal vatValue,
        BigDecimal valueWithVat) {

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public String createdBy() {
        return lines.isEmpty() ? null : lines.get(0).getCreatedBy();
    }
}
