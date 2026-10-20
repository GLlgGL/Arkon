package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ASSET")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CODE")
    private String code;

    @Column(name = "NAME")
    private String name;

    @Column(name = "CATEGORY")
    private String category;

    @Column(name = "LOCATION")
    private String location;

    @Column(name = "PURCHASE_DATE")
    private LocalDate purchaseDate;

    @Column(name = "COST")
    private BigDecimal cost;

    @Column(name = "SALVAGE")
    private BigDecimal salvage;

    @Column(name = "LIFE_MONTHS")
    private Integer lifeMonths;

    @Column(name = "METHOD")
    private String method;

    @Column(name = "RATE")
    private BigDecimal rate;

    @Column(name = "COST_CENTER_ID")
    private Long costCenterId;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "DISPOSAL_DATE")
    private LocalDate disposalDate;

    @Column(name = "DISPOSAL_VALUE")
    private BigDecimal disposalValue;

    @Column(name = "NOTE")
    private String note;

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }

    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }

    public String getLocation() { return location; }
    public void setLocation(String v) { this.location = v; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate v) { this.purchaseDate = v; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal v) { this.cost = v; }

    public BigDecimal getSalvage() { return salvage; }
    public void setSalvage(BigDecimal v) { this.salvage = v; }

    public Integer getLifeMonths() { return lifeMonths; }
    public void setLifeMonths(Integer v) { this.lifeMonths = v; }

    public String getMethod() { return method; }
    public void setMethod(String v) { this.method = v; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal v) { this.rate = v; }

    public Long getCostCenterId() { return costCenterId; }
    public void setCostCenterId(Long v) { this.costCenterId = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public LocalDate getDisposalDate() { return disposalDate; }
    public void setDisposalDate(LocalDate v) { this.disposalDate = v; }

    public BigDecimal getDisposalValue() { return disposalValue; }
    public void setDisposalValue(BigDecimal v) { this.disposalValue = v; }

    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }


    { this.status = "ACTIVE"; this.method = "STRAIGHT"; this.salvage = BigDecimal.ZERO; }
    public boolean isDisposed() { return "DISPOSED".equals(status); }
}
