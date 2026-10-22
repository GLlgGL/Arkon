package com.fms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Records a user shares with another user, view only: all of them (no code) or one record. */
@Entity
@Table(name = "RECORD_SHARE")
public class RecordShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String owner;
    @Column(nullable = false)
    private String grantee;
    @Enumerated(EnumType.STRING)
    @Column(name = "ORDER_TYPE")
    private OrderType orderType;
    @Column(name = "ORDER_CODE")
    private String orderCode;
    private String note;
    @Column(name = "CREATED_BY", nullable = false)
    private String createdBy;
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getOwner() { return owner; }
    public void setOwner(String v) { this.owner = v; }
    public String getGrantee() { return grantee; }
    public void setGrantee(String v) { this.grantee = v; }
    public OrderType getOrderType() { return orderType; }
    public void setOrderType(OrderType v) { this.orderType = v; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String v) { this.orderCode = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v == null || v.isBlank() ? null : v.trim(); }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { this.createdBy = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isAllRecords() { return orderCode == null; }
}
