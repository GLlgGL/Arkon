package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 1 unit of a currency = RATE units of the base currency, on a date. */
@Entity
@Table(name = "EXCHANGE_RATE")
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "RATE_DATE", nullable = false)
    private LocalDate rateDate;
    @Column(nullable = false)
    private String currency;
    @Column(nullable = false)
    private BigDecimal rate;

    public Long getId() { return id; }
    public LocalDate getRateDate() { return rateDate; }
    public void setRateDate(LocalDate d) { this.rateDate = d; }
    public String getCurrency() { return currency; }
    public void setCurrency(String c) { this.currency = c; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal r) { this.rate = r; }
}
