package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A cash register ("Arka") or bank account ("Banka"). */
@Entity
@Table(name = "CASH_ACCOUNT")
public class CashAccount {

    public enum Kind { CASH, BANK }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind = Kind.CASH;

    @Column(name = "BANK_NAME")
    private String bankName;

    private String iban;

    @Column(name = "OPENING_BALANCE", nullable = false)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "OPENING_DATE", nullable = false)
    private LocalDate openingDate = LocalDate.now();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "IS_DEFAULT", nullable = false)
    private boolean defaultAccount;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? null : name.trim(); }
    public Kind getKind() { return kind; }
    public void setKind(Kind kind) { this.kind = kind; }
    public boolean isBank() { return kind == Kind.BANK; }
    public String getBankName() { return bankName; }
    public void setBankName(String v) { this.bankName = v == null || v.isBlank() ? null : v.trim(); }
    public String getIban() { return iban; }
    public void setIban(String v) { this.iban = v == null || v.isBlank() ? null : v.trim().toUpperCase(); }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal v) { this.openingBalance = v == null ? BigDecimal.ZERO : v; }
    public LocalDate getOpeningDate() { return openingDate; }
    public void setOpeningDate(LocalDate v) { this.openingDate = v == null ? LocalDate.now() : v; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isDefaultAccount() { return defaultAccount; }
    public void setDefaultAccount(boolean v) { this.defaultAccount = v; }

    /** Currency of the account; null = base currency (V18). */
    @Column(name = "CURRENCY")
    private String currency;

    public String getCurrency() { return currency; }
    public void setCurrency(String c) { this.currency = c == null || c.isBlank() ? null : c; }
    public boolean isForeign() { return currency != null; }
}
