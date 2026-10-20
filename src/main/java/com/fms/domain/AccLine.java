package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** One debit or credit of a journal entry. */
@Entity
@Table(name = "ACC_LINE")
public class AccLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "ENTRY_ID")
    private AccEntry entry;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "ACCOUNT_ID")
    private AccAccount account;
    @Column(nullable = false)
    private BigDecimal debit = BigDecimal.ZERO;
    @Column(nullable = false)
    private BigDecimal credit = BigDecimal.ZERO;
    private String note;

    public Long getId() { return id; }
    public AccEntry getEntry() { return entry; }
    public void setEntry(AccEntry e) { this.entry = e; }
    public AccAccount getAccount() { return account; }
    public void setAccount(AccAccount a) { this.account = a; }
    public BigDecimal getDebit() { return debit; }
    public void setDebit(BigDecimal d) { this.debit = d; }
    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal c) { this.credit = c; }
    public String getNote() { return note; }
    public void setNote(String n) { this.note = n; }
}
