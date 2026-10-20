package com.fms.domain;

import jakarta.persistence.*;

/** An account in the chart of accounts. */
@Entity
@Table(name = "ACC_ACCOUNT")
public class AccAccount {

    public enum Type { ASSET, LIABILITY, EQUITY, INCOME, EXPENSE;
        /** Assets and expenses grow with debits; the others with credits. */
        public boolean isDebitNature() { return this == ASSET || this == EXPENSE; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;
    @Column(name = "SYSTEM_KEY")
    private String systemKey;
    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String c) { this.code = c == null ? null : c.trim(); }
    public String getName() { return name; }
    public void setName(String n) { this.name = n == null ? null : n.trim(); }
    public Type getType() { return type; }
    public void setType(Type t) { this.type = t; }
    public String getSystemKey() { return systemKey; }
    public void setSystemKey(String k) { this.systemKey = k; }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public String getLabel() { return code + " " + name; }
}
