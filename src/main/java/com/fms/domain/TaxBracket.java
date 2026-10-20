package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "PAYROLL_TAX_BRACKET")
public class TaxBracket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "FROM_AMOUNT")
    private BigDecimal fromAmount;

    @Column(name = "TO_AMOUNT")
    private BigDecimal toAmount;

    @Column(name = "FIXED_AMOUNT")
    private BigDecimal fixedAmount;

    @Column(name = "RATE")
    private BigDecimal rate;

    @Column(name = "OVER_AMOUNT")
    private BigDecimal overAmount;

    public Long getId() { return id; }

    public BigDecimal getFromAmount() { return fromAmount; }
    public void setFromAmount(BigDecimal v) { this.fromAmount = v; }

    public BigDecimal getToAmount() { return toAmount; }
    public void setToAmount(BigDecimal v) { this.toAmount = v; }

    public BigDecimal getFixedAmount() { return fixedAmount; }
    public void setFixedAmount(BigDecimal v) { this.fixedAmount = v; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal v) { this.rate = v; }

    public BigDecimal getOverAmount() { return overAmount; }
    public void setOverAmount(BigDecimal v) { this.overAmount = v; }

}
