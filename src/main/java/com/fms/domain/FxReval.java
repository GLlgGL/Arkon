package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** One item of a year-end exchange rate adjustment (a foreign-currency account or open invoice). */
@Entity
@Table(name = "FX_REVAL")
public class FxReval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "REVAL_DATE", nullable = false)
    private LocalDate revalDate;
    @Column(name = "ITEM_KEY", nullable = false)
    private String itemKey;
    private String label;
    @Column(nullable = false)
    private String currency;
    @Column(name = "FX_AMOUNT", nullable = false)
    private BigDecimal fxAmount;
    @Column(nullable = false)
    private BigDecimal rate;
    @Column(nullable = false)
    private BigDecimal book;
    @Column(nullable = false)
    private BigDecimal diff;
    @Column(name = "ENTRY_ID")
    private Long entryId;
    @Column(name = "REVERSAL_ID")
    private Long reversalId;

    public Long getId() { return id; }
    public LocalDate getRevalDate() { return revalDate; }
    public void setRevalDate(LocalDate v) { this.revalDate = v; }
    public String getItemKey() { return itemKey; }
    public void setItemKey(String v) { this.itemKey = v; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { this.currency = v; }
    public BigDecimal getFxAmount() { return fxAmount; }
    public void setFxAmount(BigDecimal v) { this.fxAmount = v; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal v) { this.rate = v; }
    public BigDecimal getBook() { return book; }
    public void setBook(BigDecimal v) { this.book = v; }
    public BigDecimal getDiff() { return diff; }
    public void setDiff(BigDecimal v) { this.diff = v; }
    public Long getEntryId() { return entryId; }
    public void setEntryId(Long v) { this.entryId = v; }
    public Long getReversalId() { return reversalId; }
    public void setReversalId(Long v) { this.reversalId = v; }
}
