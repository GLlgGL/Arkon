package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "STOCK_COUNT")
public class StockCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CODE")
    private String code;

    @Column(name = "COUNTED_ON")
    private LocalDate countedOn;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "NOTE")
    private String note;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "APPLIED_AT")
    private LocalDateTime appliedAt;

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }

    public LocalDate getCountedOn() { return countedOn; }
    public void setCountedOn(LocalDate v) { this.countedOn = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime v) { this.appliedAt = v; }


    { this.status = "OPEN"; this.createdAt = LocalDateTime.now(); }
    public boolean isOpen() { return "OPEN".equals(status); }
}
