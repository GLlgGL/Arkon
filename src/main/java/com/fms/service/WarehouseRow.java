package com.fms.service;

import java.math.BigDecimal;

/** One product in the warehouse report. */
public record WarehouseRow(int nr, String product, String unit, long purchased, long sold, long adjusted,
                           long remaining, int quantityLimit, BigDecimal salePrice, BigDecimal value) {

    /**
     * "For supply" rule from the original WarehouseReport (BindGrid1): the product
     * needs restocking when what remains is below its quantity limit, or less than
     * {@code margin} units above it.
     */
    public boolean needsSupply(int margin) {
        return remaining < quantityLimit || (remaining - quantityLimit) < margin;
    }
}
