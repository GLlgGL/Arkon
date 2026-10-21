package com.fms.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A transfer of goods between two locations; its movements are stock adjustments with this code. */
@Entity
@Table(name = "STOCK_TRANSFER")
public class StockTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(name = "FROM_LOCATION", nullable = false)
    private Long fromLocation;
    @Column(name = "TO_LOCATION", nullable = false)
    private Long toLocation;
    @Column(name = "MOVED_ON", nullable = false)
    private LocalDate movedOn;
    private String note;
    @Column(name = "CREATED_BY", nullable = false)
    private String createdBy;
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String c) { this.code = c; }
    public Long getFromLocation() { return fromLocation; }
    public void setFromLocation(Long v) { this.fromLocation = v; }
    public Long getToLocation() { return toLocation; }
    public void setToLocation(Long v) { this.toLocation = v; }
    public LocalDate getMovedOn() { return movedOn; }
    public void setMovedOn(LocalDate d) { this.movedOn = d; }
    public String getNote() { return note; }
    public void setNote(String n) { this.note = n == null || n.isBlank() ? null : n.trim(); }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String u) { this.createdBy = u; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
