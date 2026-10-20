package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A purchase order: a request to a merchant for products. It doesn't change
 * stock. When the goods arrive it's "received" and becomes a real purchase.
 */
@Entity
@Table(name = "PURCHASE_ORDER")
public class PurchaseOrder {

    public enum Status { OPEN, RECEIVED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "MERCHANT_NAME", nullable = false)
    private String merchantName;

    @Column(name = "COMPANY_NAME", nullable = false)
    private String companyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    private String note;

    @Column(name = "PURCHASE_CODE")
    private String purchaseCode;

    @Column(name = "CREATED_BY", nullable = false)
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    public BigDecimal getTotalNoVat() {
        return lines.stream().map(l -> l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getUnits() {
        return lines.stream().mapToInt(PurchaseOrderLine::getQuantity).sum();
    }

    public boolean isOpen() { return status == Status.OPEN; }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String v) { this.merchantName = v; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String v) { this.companyName = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note == null || note.isBlank() ? null : note.trim(); }
    public String getPurchaseCode() { return purchaseCode; }
    public void setPurchaseCode(String v) { this.purchaseCode = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<PurchaseOrderLine> getLines() { return lines; }
}
