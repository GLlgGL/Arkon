package com.fms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** A physical shelf or location in the warehouse, e.g. "A1". */
@Entity
@Table(name = "SHELF")
public class Shelf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    private String description;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code == null ? null : code.trim().toUpperCase(); }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d == null || d.isBlank() ? null : d.trim(); }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
