package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "PRICE_LIST")
public class PriceList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "ACTIVE")
    private boolean active;

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }

    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }


    { this.active = true; }
}
