package com.fms.domain;

import jakarta.persistence.*;

/** A user's dashboard: comma-separated widget keys in display order. */
@Entity
@Table(name = "USER_DASHBOARD")
public class UserDashboard {
    @Id
    private String username;
    @Column(nullable = false)
    private String widgets;

    public String getUsername() { return username; }
    public void setUsername(String u) { this.username = u; }
    public String getWidgets() { return widgets; }
    public void setWidgets(String w) { this.widgets = w; }
}
