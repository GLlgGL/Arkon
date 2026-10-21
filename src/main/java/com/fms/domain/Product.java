package com.fms.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCT")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank
    @Size(max = 30)
    @Column(nullable = false)
    private String unit;

    @NotNull
    @DecimalMin("0")
    @Column(name = "BUY_PRICE", nullable = false)
    private BigDecimal buyPrice = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0")
    @Column(name = "SALE_PRICE", nullable = false)
    private BigDecimal salePrice = BigDecimal.ZERO;

    @Min(0)
    @Column(name = "QUANTITY_LIMIT", nullable = false)
    private int quantityLimit;

    /** Barcode or product code (optional, unique). A scanner can type it on the order page. */
    @Size(max = 50)
    @Column(unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "PRODUCT_CLASS_ID")
    private ProductClass productClass;

    @Column(name = "REGISTERED_AT", nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? null : name.trim(); }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getBuyPrice() { return buyPrice; }
    public void setBuyPrice(BigDecimal buyPrice) { this.buyPrice = buyPrice; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public int getQuantityLimit() { return quantityLimit; }
    public void setQuantityLimit(int quantityLimit) { this.quantityLimit = quantityLimit; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code == null || code.isBlank() ? null : code.trim(); }
    public ProductClass getProductClass() { return productClass; }
    public void setProductClass(ProductClass productClass) { this.productClass = productClass; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    /** NONE, BATCH (batch number and expiry) or SERIAL (one serial number per unit). Tracking module. */
    @Column(name = "TRACKING", nullable = false)
    private String tracking = "NONE";

    public String getTracking() { return tracking; }
    public void setTracking(String t) { this.tracking = "BATCH".equals(t) || "SERIAL".equals(t) ? t : "NONE"; }
    public boolean isBatchTracked() { return "BATCH".equals(tracking); }
    public boolean isSerialTracked() { return "SERIAL".equals(tracking); }
}
