package com.fms.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "REMINDER_LOG")
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ORDER_CODE")
    private String orderCode;

    @Column(name = "KIND")
    private String kind;

    @Column(name = "SENT_TO")
    private String sentTo;

    @Column(name = "SENT_AT")
    private LocalDateTime sentAt;

    public Long getId() { return id; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String v) { this.orderCode = v; }

    public String getKind() { return kind; }
    public void setKind(String v) { this.kind = v; }

    public String getSentTo() { return sentTo; }
    public void setSentTo(String v) { this.sentTo = v; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime v) { this.sentAt = v; }


    { this.sentAt = LocalDateTime.now(); }
}
