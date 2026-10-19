package com.fms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One line of the audit log: who did what, to which record, and when. */
@Entity
@Table(name = "AUDIT_LOG")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "LOGGED_AT", nullable = false)
    private LocalDateTime loggedAt = LocalDateTime.now();

    @Column(nullable = false)
    private String username;

    @Column(name = "ACTION_NAME", nullable = false)
    private String action;

    @Column(nullable = false)
    private String entity;

    @Column(name = "ENTITY_KEY")
    private String entityKey;

    private String details;

    public Long getId() { return id; }
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    public String getEntityKey() { return entityKey; }
    public void setEntityKey(String entityKey) { this.entityKey = entityKey; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
