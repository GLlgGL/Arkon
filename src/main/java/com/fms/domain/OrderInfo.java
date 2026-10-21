package com.fms.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Per-order details that aren't per line: payment due date, offer status and conversion. */
@Entity
@Table(name = "ORDER_INFO")
public class OrderInfo {

    public enum OfferStatus { OPEN, ACCEPTED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ORDER_TYPE", nullable = false)
    private OrderType orderType;

    @Column(name = "ORDER_CODE", nullable = false)
    private String orderCode;

    @Column(name = "DUE_DATE")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "OFFER_STATUS")
    private OfferStatus offerStatus;

    @Column(name = "VALID_UNTIL")
    private LocalDate validUntil;

    @Column(name = "CONVERTED_TO")
    private String convertedTo;

    /** Cost centre of the whole order (sales and purchases). */
    @Column(name = "COST_CENTER_ID")
    private Long costCenterId;

    public Long getCostCenterId() { return costCenterId; }
    public void setCostCenterId(Long v) { this.costCenterId = v; }

    /** OPEN offers past their valid-until date count as expired. */
    public boolean isExpired() {
        return offerStatus == OfferStatus.OPEN && validUntil != null && validUntil.isBefore(LocalDate.now());
    }

    /** Status shown in lists: Converted, Accepted, Rejected, Expired or Open. */
    public String getOfferStatusLabel() {
        if (convertedTo != null) return "Converted";
        if (offerStatus == null) return "Open";
        if (isExpired()) return "Expired";
        return switch (offerStatus) {
            case OPEN -> "Open";
            case ACCEPTED -> "Accepted";
            case REJECTED -> "Rejected";
        };
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public OfferStatus getOfferStatus() { return offerStatus; }
    public void setOfferStatus(OfferStatus offerStatus) { this.offerStatus = offerStatus; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public String getConvertedTo() { return convertedTo; }
    public void setConvertedTo(String convertedTo) { this.convertedTo = convertedTo; }

    // ---- Currency of the order (V18); null = base currency
    @Column(name = "CURRENCY")
    private String currency;
    @Column(name = "FX_RATE")
    private java.math.BigDecimal fxRate;

    public String getCurrency() { return currency; }
    public void setCurrency(String c) { this.currency = c; }
    public java.math.BigDecimal getFxRate() { return fxRate; }
    public void setFxRate(java.math.BigDecimal r) { this.fxRate = r; }
    public boolean isForeign() { return currency != null && fxRate != null && fxRate.signum() > 0; }

    // ---- VAT (V20): null rate = the normal rate from Settings
    @Column(name = "VAT_RATE")
    private java.math.BigDecimal vatRate;
    @Column(name = "VAT_NOTE")
    private String vatNote;

    public java.math.BigDecimal getVatRate() { return vatRate; }
    public void setVatRate(java.math.BigDecimal r) { this.vatRate = r; }
    public String getVatNote() { return vatNote; }
    public void setVatNote(String n) { this.vatNote = n == null || n.isBlank() ? null : n.trim(); }

    // ---- Purchases (V25): type for the purchase book, and the supplier's invoice number
    @Column(name = "PURCHASE_KIND")
    private String purchaseKind;
    @Column(name = "SUPPLIER_INVOICE")
    private String supplierInvoice;

    public String getPurchaseKind() { return purchaseKind == null ? "LOCAL" : purchaseKind; }
    public void setPurchaseKind(String k) { this.purchaseKind = k == null || k.isBlank() ? null : k; }
    public String getSupplierInvoice() { return supplierInvoice; }
    public void setSupplierInvoice(String v) { this.supplierInvoice = v == null || v.isBlank() ? null : v.trim(); }
}
