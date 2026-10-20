package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PAYROLL_LINE")
public class PayrollLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "RUN_ID")
    private Long runId;

    @Column(name = "EMPLOYEE_ID")
    private Long employeeId;

    @Column(name = "EMPLOYEE_NAME")
    private String employeeName;

    @Column(name = "GROSS")
    private BigDecimal gross;

    @Column(name = "EMP_SOCIAL")
    private BigDecimal empSocial;

    @Column(name = "EMP_HEALTH")
    private BigDecimal empHealth;

    @Column(name = "TAX")
    private BigDecimal tax;

    @Column(name = "NET")
    private BigDecimal net;

    @Column(name = "ER_SOCIAL")
    private BigDecimal erSocial;

    @Column(name = "ER_HEALTH")
    private BigDecimal erHealth;

    @Column(name = "TOTAL_COST")
    private BigDecimal totalCost;

    @Column(name = "COST_CENTER_ID")
    private Long costCenterId;

    public Long getId() { return id; }

    public Long getRunId() { return runId; }
    public void setRunId(Long v) { this.runId = v; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long v) { this.employeeId = v; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String v) { this.employeeName = v; }

    public BigDecimal getGross() { return gross; }
    public void setGross(BigDecimal v) { this.gross = v; }

    public BigDecimal getEmpSocial() { return empSocial; }
    public void setEmpSocial(BigDecimal v) { this.empSocial = v; }

    public BigDecimal getEmpHealth() { return empHealth; }
    public void setEmpHealth(BigDecimal v) { this.empHealth = v; }

    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal v) { this.tax = v; }

    public BigDecimal getNet() { return net; }
    public void setNet(BigDecimal v) { this.net = v; }

    public BigDecimal getErSocial() { return erSocial; }
    public void setErSocial(BigDecimal v) { this.erSocial = v; }

    public BigDecimal getErHealth() { return erHealth; }
    public void setErHealth(BigDecimal v) { this.erHealth = v; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal v) { this.totalCost = v; }

    public Long getCostCenterId() { return costCenterId; }
    public void setCostCenterId(Long v) { this.costCenterId = v; }

}
