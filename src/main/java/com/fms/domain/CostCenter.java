package com.fms.domain;

import jakarta.persistence.*;

/** A cost centre: a project, branch, department or product line that results are grouped by. */
@Entity
@Table(name = "COST_CENTER")
public class CostCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String c) { this.code = c == null ? null : c.trim().toUpperCase(); }
    public String getName() { return name; }
    public void setName(String n) { this.name = n == null ? null : n.trim(); }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d == null || d.isBlank() ? null : d.trim(); }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public String getLabel() { return code + " – " + name; }
}
