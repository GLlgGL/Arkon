package com.fms.domain;

import jakarta.persistence.*;

/** A shop or warehouse with its own stock (Locations module). */
@Entity
@Table(name = "LOCATION")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String kind = "SHOP";
    private String address;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "IS_MAIN", nullable = false)
    private boolean main;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String c) { this.code = c == null ? null : c.trim().toUpperCase(); }
    public String getName() { return name; }
    public void setName(String n) { this.name = n == null ? null : n.trim(); }
    public String getKind() { return kind; }
    public void setKind(String k) { this.kind = "WAREHOUSE".equals(k) ? "WAREHOUSE" : "SHOP"; }
    public String getAddress() { return address; }
    public void setAddress(String a) { this.address = a == null || a.isBlank() ? null : a.trim(); }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public boolean isMain() { return main; }
    public void setMain(boolean m) { this.main = m; }
    public String getLabel() { return name + " (" + code + ")"; }
}
