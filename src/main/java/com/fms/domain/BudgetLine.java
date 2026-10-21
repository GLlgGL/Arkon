package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "BUDGET_LINE")
public class BudgetLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "BUDGET_YEAR")
    private Integer budgetYear;

    @Column(name = "LINE_KEY")
    private String lineKey;

    @Column(name = "MONTH_NO")
    private Integer monthNo;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    public Long getId() { return id; }

    public Integer getBudgetYear() { return budgetYear; }
    public void setBudgetYear(Integer v) { this.budgetYear = v; }

    public String getLineKey() { return lineKey; }
    public void setLineKey(String v) { this.lineKey = v; }

    public Integer getMonthNo() { return monthNo; }
    public void setMonthNo(Integer v) { this.monthNo = v; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }

}
