package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCTION_ORDER")
public class ProductionOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CODE")
    private String code;

    @Column(name = "PRODUCT_NAME")
    private String productName;

    @Column(name = "QUANTITY")
    private Integer quantity;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "PLANNED_ON")
    private LocalDate plannedOn;

    @Column(name = "DONE_AT")
    private LocalDateTime doneAt;

    @Column(name = "UNIT_COST")
    private BigDecimal unitCost;

    @Column(name = "TOTAL_COST")
    private BigDecimal totalCost;

    @Column(name = "NOTE")
    private String note;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }

    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer v) { this.quantity = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public LocalDate getPlannedOn() { return plannedOn; }
    public void setPlannedOn(LocalDate v) { this.plannedOn = v; }

    public LocalDateTime getDoneAt() { return doneAt; }
    public void setDoneAt(LocalDateTime v) { this.doneAt = v; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal v) { this.unitCost = v; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal v) { this.totalCost = v; }

    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }


    { this.status = "DRAFT"; this.createdAt = LocalDateTime.now(); }
    public boolean isDraft() { return "DRAFT".equals(status); }
    public boolean isDone() { return "DONE".equals(status); }
}
