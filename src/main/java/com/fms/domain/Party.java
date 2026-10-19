package com.fms.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Shared shape of Merchants, Companies, Clients and NotClients, which all had
 * Name / TIN / Address / Mail / Number columns in the original.
 */
@MappedSuperclass
public abstract class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String name;

    @Size(max = 50)
    private String tin;

    @Size(max = 300)
    private String address;

    @Email
    @Size(max = 200)
    private String email;

    @Size(max = 50)
    private String phone;

    @Column(name = "REGISTERED_AT", nullable = false)
    private LocalDateTime registeredAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? null : name.trim(); }
    public String getTin() { return tin; }
    public void setTin(String tin) { this.tin = tin; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}
