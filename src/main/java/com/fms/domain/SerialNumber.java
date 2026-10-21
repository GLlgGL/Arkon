package com.fms.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/** One unit of a serial-tracked product: where it came from, where it is, who bought it. */
@Entity
@Table(name = "SERIAL_NUMBER")
public class SerialNumber {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "PRODUCT_NAME", nullable = false)
    private String productName;
    @Column(nullable = false)
    private String serial;
    @Column(nullable = false)
    private String status = "IN_STOCK";
    @Column(name = "LOCATION_ID")
    private Long locationId;
    @Column(name = "PURCHASE_LINE_ID")
    private Long purchaseLineId;
    @Column(name = "PURCHASE_CODE")
    private String purchaseCode;
    @Column(name = "PURCHASED_ON")
    private LocalDate purchasedOn;
    @Column(name = "SALE_LINE_ID")
    private Long saleLineId;
    @Column(name = "SALE_CODE")
    private String saleCode;
    @Column(name = "SOLD_ON")
    private LocalDate soldOn;

    public Long getId() { return id; }
    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }
    public String getSerial() { return serial; }
    public void setSerial(String v) { this.serial = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long v) { this.locationId = v; }
    public Long getPurchaseLineId() { return purchaseLineId; }
    public void setPurchaseLineId(Long v) { this.purchaseLineId = v; }
    public String getPurchaseCode() { return purchaseCode; }
    public void setPurchaseCode(String v) { this.purchaseCode = v; }
    public LocalDate getPurchasedOn() { return purchasedOn; }
    public void setPurchasedOn(LocalDate v) { this.purchasedOn = v; }
    public Long getSaleLineId() { return saleLineId; }
    public void setSaleLineId(Long v) { this.saleLineId = v; }
    public String getSaleCode() { return saleCode; }
    public void setSaleCode(String v) { this.saleCode = v; }
    public LocalDate getSoldOn() { return soldOn; }
    public void setSoldOn(LocalDate v) { this.soldOn = v; }
}
