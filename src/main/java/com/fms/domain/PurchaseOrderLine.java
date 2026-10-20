package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** One product on a purchase order. */
@Entity
@Table(name = "PURCHASE_ORDER_LINE")
public class PurchaseOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "PO_ID")
    private PurchaseOrder order;

    @Column(name = "PRODUCT_NAME", nullable = false)
    private String productName;

    private String unit;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "UNIT_PRICE", nullable = false)
    private BigDecimal unitPrice;

    public Long getId() { return id; }
    public PurchaseOrder getOrder() { return order; }
    public void setOrder(PurchaseOrder order) { this.order = order; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
