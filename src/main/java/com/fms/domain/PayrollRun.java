package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PAYROLL_RUN")
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PERIOD")
    private String period;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "PAID_ON")
    private LocalDate paidOn;

    @Column(name = "CREATED_BY")
    private String createdBy;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    public Long getId() { return id; }

    public String getPeriod() { return period; }
    public void setPeriod(String v) { this.period = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public LocalDate getPaidOn() { return paidOn; }
    public void setPaidOn(LocalDate v) { this.paidOn = v; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }


    { this.status = "DRAFT"; this.createdAt = LocalDateTime.now(); }
    public boolean isDraft() { return "DRAFT".equals(status); }
}
