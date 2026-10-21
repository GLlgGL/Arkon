package com.fms.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "APP_USER")
public class AppUser {

    public static final int ROLE_ADMIN = 1;
    public static final int ROLE_USER = 2;
    /** Read-only: sees reports, statistics, debts, cash, taxes, payroll and assets; changes nothing. */
    public static final int ROLE_ACCOUNTANT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "FIRST_NAME", nullable = false)
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false)
    private String lastName;

    private String email;

    @Column(nullable = false, unique = true)
    private String username;

    /** Stored with a {bcrypt} or {noop} prefix; see SecurityConfig. */
    @Column(nullable = false)
    private String password;

    @Column(name = "USER_ROLE", nullable = false)
    private int role = ROLE_USER;

    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;

    @Column(name = "LAST_PASSWORD_CHANGE_AT")
    private LocalDateTime lastPasswordChangeAt;

    public boolean isAdmin() { return role == ROLE_ADMIN; }
    public boolean isAccountant() { return role == ROLE_ACCOUNTANT; }
    /** Sees everyone's orders and figures (not only their own). */
    public boolean isSeesAll() { return isAdmin() || isAccountant(); }
    public String getRoleKey() { return isAdmin() ? "admin" : (isAccountant() ? "accountant" : "user"); }
    public String getFullName() { return firstName + " " + lastName; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getRole() { return role; }
    public void setRole(int role) { this.role = role; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public LocalDateTime getLastPasswordChangeAt() { return lastPasswordChangeAt; }
    public void setLastPasswordChangeAt(LocalDateTime t) { this.lastPasswordChangeAt = t; }

    // ---- Two-step login (V17)
    @Column(name = "TOTP_SECRET")
    private String totpSecret;

    @Column(name = "TOTP_ENABLED", nullable = false)
    private boolean totpEnabled;

    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String s) { this.totpSecret = s; }
    public boolean isTotpEnabled() { return totpEnabled; }
    public void setTotpEnabled(boolean v) { this.totpEnabled = v; }

    /** Shop or warehouse (Locations module); null = the main location. */
    @Column(name = "LOCATION_ID")
    private Long locationId;

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long v) { this.locationId = v; }
}
