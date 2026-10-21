package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Money received for a sale, or paid for a purchase. */
@Entity
@Table(name = "PAYMENT")
public class Payment {

    public enum Method {
        CASH("Cash"), BANK("Bank transfer"), CARD("Card"), OTHER("Other");
        private final String label;
        Method(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /** PAYMENT: money in (sale) or out (purchase). REFUND: money paid back to a client after a return. */
    public enum Kind { PAYMENT, REFUND }

    @Enumerated(EnumType.STRING)
    @Column(name = "PAY_KIND", nullable = false)
    private Kind kind = Kind.PAYMENT;

    /** The cash register or bank account the money went into or out of. */
    @Column(name = "ACCOUNT_ID")
    private Long accountId;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Kind getKind() { return kind; }
    public void setKind(Kind kind) { this.kind = kind; }
    public boolean isRefund() { return kind == Kind.REFUND; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ORDER_TYPE", nullable = false)
    private OrderType orderType;

    @Column(name = "ORDER_CODE", nullable = false)
    private String orderCode;

    @Column(name = "PAID_ON", nullable = false)
    private LocalDate paidOn;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAY_METHOD", nullable = false)
    private Method method = Method.CASH;

    private String note;

    @Column(name = "CREATED_BY", nullable = false)
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public LocalDate getPaidOn() { return paidOn; }
    public void setPaidOn(LocalDate paidOn) { this.paidOn = paidOn; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Method getMethod() { return method; }
    public void setMethod(Method method) { this.method = method; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ---- As entered, in the order's currency (V18)
    @Column(name = "FX_CURRENCY")
    private String fxCurrency;
    @Column(name = "FX_AMOUNT")
    private BigDecimal fxAmount;

    public String getFxCurrency() { return fxCurrency; }
    public void setFxCurrency(String c) { this.fxCurrency = c; }
    public BigDecimal getFxAmount() { return fxAmount; }
    public void setFxAmount(BigDecimal a) { this.fxAmount = a; }

    // ---- Exchange difference (V19): rate on the payment day, and + gain / - loss in the base currency
    @Column(name = "PAY_RATE")
    private BigDecimal payRate;
    @Column(name = "FX_DIFF")
    private BigDecimal fxDiff;

    public BigDecimal getPayRate() { return payRate; }
    public void setPayRate(BigDecimal r) { this.payRate = r; }
    public BigDecimal getFxDiff() { return fxDiff; }
    public void setFxDiff(BigDecimal d) { this.fxDiff = d; }
    /** What really moved, in the base currency: the amount at the payment-day rate. */
    public BigDecimal getRealAmount() {
        return payRate != null && fxAmount != null ? fxAmount.multiply(payRate).setScale(2, java.math.RoundingMode.HALF_UP) : getAmount();
    }
}
