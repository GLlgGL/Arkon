package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Money in or out of one account. Amount is always positive; direction says which way. */
@Entity
@Table(name = "MONEY_MOVEMENT")
public class MoneyMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ACCOUNT_ID")
    private CashAccount account;

    @Column(name = "MOVED_ON", nullable = false)
    private LocalDate movedOn;

    @Column(nullable = false)
    private String direction;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MoneyCategory category;

    private String description;

    @Column(name = "REF_NO")
    private String refNo;

    @Column(name = "PAYMENT_ID")
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ORDER_TYPE")
    private OrderType orderType;

    @Column(name = "ORDER_CODE")
    private String orderCode;

    @Column(name = "TRANSFER_ID")
    private Long transferId;

    /** PAYROLL or TAX when made by those modules (so accounting doesn't count it twice). */
    @Column(name = "ORIGIN")
    private String origin;

    public String getOrigin() { return origin; }
    public void setOrigin(String o) { this.origin = o; }

    /** Cost centre of hand-entered income or expenses. */
    @Column(name = "COST_CENTER_ID")
    private Long costCenterId;

    public Long getCostCenterId() { return costCenterId; }
    public void setCostCenterId(Long v) { this.costCenterId = v; }

    @Column(name = "CREATED_BY", nullable = false)
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isIn() { return "IN".equals(direction); }
    /** + for money in, - for money out. */
    public BigDecimal getSigned() { return isIn() ? amount : amount.negate(); }
    /** Payments, refunds and transfers are managed where they were made, not deleted here. */
    public boolean isManual() { return paymentId == null && transferId == null; }

    public Long getId() { return id; }
    public CashAccount getAccount() { return account; }
    public void setAccount(CashAccount a) { this.account = a; }
    public LocalDate getMovedOn() { return movedOn; }
    public void setMovedOn(LocalDate d) { this.movedOn = d; }
    public String getDirection() { return direction; }
    public void setDirection(String d) { this.direction = d; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal a) { this.amount = a; }
    public MoneyCategory getCategory() { return category; }
    public void setCategory(MoneyCategory c) { this.category = c; this.direction = c.getDirection(); }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d == null || d.isBlank() ? null : d.trim(); }
    public String getRefNo() { return refNo; }
    public void setRefNo(String r) { this.refNo = r == null || r.isBlank() ? null : r.trim(); }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long p) { this.paymentId = p; }
    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType t) { this.orderType = t; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String c) { this.orderCode = c; }
    public Long getTransferId() { return transferId; }
    public void setTransferId(Long t) { this.transferId = t; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String u) { this.createdBy = u; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** Amount in the account's currency, for foreign-currency accounts (V18). */
    @Column(name = "FX_AMOUNT")
    private BigDecimal fxAmount;

    public BigDecimal getFxAmount() { return fxAmount; }
    public void setFxAmount(BigDecimal a) { this.fxAmount = a; }
    /** Amount in the account's own currency. */
    public BigDecimal getAccountAmount() { return fxAmount != null ? fxAmount : amount; }
    public BigDecimal getAccountSigned() { return isIn() ? getAccountAmount() : getAccountAmount().negate(); }

    /** The bank statement line this movement was matched to or created from (V29). */
    @Column(name = "BANK_REF")
    private String bankRef;

    public String getBankRef() { return bankRef; }
    public void setBankRef(String r) { this.bankRef = r; }
}
