package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "EMPLOYEE")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "PERSONAL_NO")
    private String personalNo;

    @Column(name = "POSITION")
    private String position;

    @Column(name = "GROSS_SALARY")
    private BigDecimal grossSalary;

    @Column(name = "START_DATE")
    private LocalDate startDate;

    @Column(name = "END_DATE")
    private LocalDate endDate;

    @Column(name = "IBAN")
    private String iban;

    @Column(name = "COST_CENTER_ID")
    private Long costCenterId;

    @Column(name = "ACTIVE")
    private boolean active;

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }

    public String getPersonalNo() { return personalNo; }
    public void setPersonalNo(String v) { this.personalNo = v; }

    public String getPosition() { return position; }
    public void setPosition(String v) { this.position = v; }

    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal v) { this.grossSalary = v; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate v) { this.startDate = v; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { this.endDate = v; }

    public String getIban() { return iban; }
    public void setIban(String v) { this.iban = v; }

    public Long getCostCenterId() { return costCenterId; }
    public void setCostCenterId(Long v) { this.costCenterId = v; }

    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }


    { this.active = true; }
}
