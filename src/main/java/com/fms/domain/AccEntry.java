package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A journal entry: lines whose debits equal their credits. */
@Entity
@Table(name = "ACC_ENTRY")
public class AccEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ENTRY_NO", nullable = false, unique = true)
    private String entryNo;
    @Column(name = "ENTRY_DATE", nullable = false)
    private LocalDate entryDate;
    @Column(nullable = false)
    private String description;
    /** MANUAL, or the automatic source (SALE, PURCHASE, PAYMENT, CASH, PAYROLL, DEPRECIATION, STOCK, TAX, OPENING). */
    @Column(nullable = false)
    private String source;
    @Column(name = "SOURCE_REF")
    private String sourceRef;
    @Column(name = "CREATED_BY", nullable = false)
    private String createdBy;
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    private List<AccLine> lines = new ArrayList<>();

    public boolean isManual() { return "MANUAL".equals(source); }
    public BigDecimal getTotal() { return lines.stream().map(AccLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add); }

    public Long getId() { return id; }
    public String getEntryNo() { return entryNo; }
    public void setEntryNo(String v) { this.entryNo = v; }
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate v) { this.entryDate = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String v) { this.sourceRef = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<AccLine> getLines() { return lines; }
}
