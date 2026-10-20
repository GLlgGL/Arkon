package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One line of a purchase, sale or offer. Lines sharing ORDER_TYPE + ORDER_CODE
 * make up one order, exactly as rows sharing PurchaseCode / SaleCode / OfferCode
 * did in the original tables.
 */
@Entity
@Table(name = "ORDER_LINE")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ORDER_TYPE", nullable = false)
    private OrderType orderType;

    @Column(name = "ORDER_CODE", nullable = false)
    private String orderCode;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "PRODUCT_NAME", nullable = false)
    private String productName;

    @Column(name = "PROVIDER_NAME", nullable = false)
    private String providerName;

    @Column(name = "BUYER_NAME", nullable = false)
    private String buyerName;

    private String unit;

    @Column(nullable = false)
    private int quantity;

    /** Price per unit after discount. */
    @Column(name = "UNIT_PRICE", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "DISCOUNT_PERCENT", nullable = false)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "VALUE_NO_VAT", nullable = false)
    private BigDecimal valueNoVat;

    @Column(name = "VAT_VALUE", nullable = false)
    private BigDecimal vatValue;

    @Column(name = "VALUE_WITH_VAT", nullable = false)
    private BigDecimal valueWithVat;

    /** Sales: average purchase cost per unit when sold (for profit). Null on older lines. */
    @Column(name = "UNIT_COST")
    private BigDecimal unitCost;

    /** Shelf the goods were taken from (sale) or put on (purchase, adjustment); optional. */
    @Column(name = "SHELF_ID")
    private Long shelfId;

    @Column(name = "SHELF_CODE")
    private String shelfCode;

    /** Return lines: the sale line returned, and the credit note number within the sale. */
    @Column(name = "RETURN_OF")
    private Long returnOf;

    @Column(name = "RETURN_NO")
    private Integer returnNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "RETURN_REASON")
    private ReturnReason returnReason;

    @Column(name = "RETURN_NOTE")
    private String returnNote;

    @Column(name = "CREATED_BY", nullable = false)
    private String createdBy;

    /** Offers only: 'Y' when the buyer is a registered client, 'N' for a prospect. */
    @Column(name = "EXISTING_CLIENT", columnDefinition = "CHAR(1)")
    private String existingClient;

    @Column(name = "LAST_UPDATED")
    private LocalDateTime lastUpdated;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType orderType) { this.orderType = orderType; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public BigDecimal getValueNoVat() { return valueNoVat; }
    public void setValueNoVat(BigDecimal valueNoVat) { this.valueNoVat = valueNoVat; }
    public BigDecimal getVatValue() { return vatValue; }
    public void setVatValue(BigDecimal vatValue) { this.vatValue = vatValue; }
    public BigDecimal getValueWithVat() { return valueWithVat; }
    public void setValueWithVat(BigDecimal valueWithVat) { this.valueWithVat = valueWithVat; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public Long getShelfId() { return shelfId; }
    public void setShelfId(Long shelfId) { this.shelfId = shelfId; }
    public String getShelfCode() { return shelfCode; }
    public void setShelfCode(String shelfCode) { this.shelfCode = shelfCode; }
    public Long getReturnOf() { return returnOf; }
    public void setReturnOf(Long returnOf) { this.returnOf = returnOf; }
    public Integer getReturnNo() { return returnNo; }
    public void setReturnNo(Integer returnNo) { this.returnNo = returnNo; }
    public boolean isReturn() { return returnOf != null; }
    public ReturnReason getReturnReason() { return returnReason; }
    public void setReturnReason(ReturnReason r) { this.returnReason = r; }
    public String getReturnNote() { return returnNote; }
    public void setReturnNote(String n) { this.returnNote = n == null || n.isBlank() ? null : n.trim(); }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getExistingClient() { return existingClient; }
    public void setExistingClient(String existingClient) { this.existingClient = existingClient; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
