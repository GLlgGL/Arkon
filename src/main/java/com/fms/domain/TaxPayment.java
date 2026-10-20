package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "TAX_PAYMENT")
public class TaxPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "KIND")
    private String kind;

    @Column(name = "PERIOD")
    private String period;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "PAID_ON")
    private LocalDate paidOn;

    @Column(name = "NOTE")
    private String note;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    public Long getId() { return id; }

    public String getKind() { return kind; }
    public void setKind(String v) { this.kind = v; }

    public String getPeriod() { return period; }
    public void setPeriod(String v) { this.period = v; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }

    public LocalDate getPaidOn() { return paidOn; }
    public void setPaidOn(LocalDate v) { this.paidOn = v; }

    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }


    { this.createdAt = LocalDateTime.now(); }
}
